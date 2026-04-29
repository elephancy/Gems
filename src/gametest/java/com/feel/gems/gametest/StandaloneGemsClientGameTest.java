package com.feel.gems.gametest;

import com.feel.gems.client.screen.GuidebookScreen;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

/** An opt-in client installation check for Gems UI without campaign assets. */
@SuppressWarnings("UnstableApiUsage")
public final class StandaloneGemsClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("gems")
                || FabricLoader.getInstance().isModLoaded("second_chance")
                || FabricLoader.getInstance().isModLoaded("terrablender")) {
            throw new IllegalStateException("Expected Gems and the test fixture without campaign mods");
        }
        try (InputStream guide = getClass().getClassLoader()
                .getResourceAsStream("assets/gems/guidebook/gameplay.md")) {
            if (guide == null || guide.read() < 0) {
                throw new IllegalStateException("Gems-only guidebook content is missing");
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read Gems-only guidebook", e);
        }
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(client -> {
                client.getWindow().setWindowed(854, 480);
                client.options.guiScale().set(2);
                client.resizeGui();
            });
            context.waitTicks(5);
            context.setScreen(GuidebookScreen::new);
            context.waitTicks(5);
            context.takeScreenshot("standalone-gems-guide-home-compact");
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof GuidebookScreen)
                        || client.gui.screen().children().isEmpty()) {
                    throw new IllegalStateException("Standalone Gems guidebook did not open with controls");
                }
            });
            context.clickScreenButton("Search");
            context.runOnClient(client -> {
                EditBox search = client.gui.screen().children().stream()
                        .filter(EditBox.class::isInstance).map(EditBox.class::cast)
                        .findFirst().orElseThrow(() -> new IllegalStateException("Guide search field missing"));
                search.setValue("fire a ranged dagger volley");
                boolean matchingSection = client.gui.screen().children().stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .anyMatch(button -> button.visible
                                && "Gems / Astra Gem".equals(button.getMessage().getString()));
                if (!matchingSection) {
                    throw new IllegalStateException("Guide search did not find the Astra section");
                }
            });
            int compactLine = context.computeOnClient(client -> firstSearchRawLine(
                    (GuidebookScreen) client.gui.screen()));
            context.clickScreenButton("Gems / Astra Gem");
            context.runOnClient(client -> assertExactLineJump(
                    (GuidebookScreen) client.gui.screen(), compactLine));
            context.takeScreenshot("standalone-gems-guidebook-exact-line-854x480");
            context.clickScreenButton("Home");
            context.waitTicks(2);
            context.takeScreenshot("standalone-gems-guide-return-home-compact");
            context.runOnClient(client -> {
                client.getWindow().setWindowed(1280, 720);
                client.options.guiScale().set(2);
                client.resizeGui();
            });
            context.setScreen(GuidebookScreen::new);
            context.waitTicks(5);
            context.takeScreenshot("standalone-gems-guide-home-wide");
            context.clickScreenButton("Topics");
            context.waitTicks(3);
            context.takeScreenshot("standalone-gems-guide-read-wide");
            context.clickScreenButton("More");
            context.clickScreenButton("More");
            context.clickScreenButton("More");
            context.clickScreenButton("Test dummy");
            context.runOnClient(client -> {
                Button previous = client.gui.screen().children().stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> "< Prev".equals(button.getMessage().getString()))
                        .findFirst().orElseThrow();
                if (!previous.active) {
                    throw new IllegalStateException("Guide Previous cannot leave a short chapter");
                }
            });
            context.clickScreenButton("< Prev");
            context.runOnClient(client -> {
                if ((int) field((GuidebookScreen) client.gui.screen(), "selectedIndex") != 12) {
                    throw new IllegalStateException("Guide Previous did not open the prior chapter");
                }
            });
            context.clickScreenButton("Next >");
            context.runOnClient(client -> {
                if ((int) field((GuidebookScreen) client.gui.screen(), "selectedIndex") != 13) {
                    throw new IllegalStateException("Guide Next did not return to the next chapter");
                }
            });
            context.clickScreenButton("Home / Reference");
            context.clickScreenButton("Search");
            context.runOnClient(client -> client.gui.screen().children().stream()
                    .filter(EditBox.class::isInstance).map(EditBox.class::cast)
                    .findFirst().orElseThrow().setValue("fire a ranged dagger volley"));
            context.waitTicks(3);
            context.takeScreenshot("standalone-gems-guide-search-wide");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitTicks(1);
            context.runOnClient(client -> {
                boolean home = client.gui.screen().children().stream()
                        .filter(Button.class::isInstance).map(Button.class::cast)
                        .anyMatch(button -> button.visible
                                && "Learn the basics".equals(button.getMessage().getString()));
                if (!home) throw new IllegalStateException("Escape did not return search to guide home");
            });
            // Fabric TestInput injects key events with modifier flags zero, so
            // a synthetic Ctrl+F cannot verify the real shortcut here.
            context.clickScreenButton("Search");
            context.runOnClient(client -> client.gui.screen().children().stream()
                    .filter(EditBox.class::isInstance).map(EditBox.class::cast)
                    .findFirst().orElseThrow().setValue("fire a ranged dagger volley"));
            int wideLine = context.computeOnClient(client -> firstSearchRawLine(
                    (GuidebookScreen) client.gui.screen()));
            context.clickScreenButton("Gems / Astra Gem");
            context.runOnClient(client -> assertExactLineJump(
                    (GuidebookScreen) client.gui.screen(), wideLine));
            context.takeScreenshot("standalone-gems-guidebook-exact-line-1280x720");
            context.setScreen(() -> null);
        }
        System.out.println("STANDALONE_GEMS_GUIDE_UI_PASS compact=854x480 wide=1280x720 exactLine=2 escape=1 searchButton=1");
    }

    private static int firstSearchRawLine(GuidebookScreen screen) {
        List<?> hits = (List<?>) field(screen, "searchHits");
        if (hits.size() != 1) {
            throw new IllegalStateException("Expected one exact guidebook search result, got " + hits.size());
        }
        try {
            Method rawIndex = hits.getFirst().getClass().getDeclaredMethod("rawLineIndex");
            rawIndex.setAccessible(true);
            return (int) rawIndex.invoke(hits.getFirst());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not inspect guidebook search target", error);
        }
    }

    private static void assertExactLineJump(GuidebookScreen screen, int rawLine) {
        @SuppressWarnings("unchecked")
        List<Integer> starts = (List<Integer>) field(screen, "sourceLineStarts");
        int expected = starts.get(rawLine);
        int highlighted = (int) field(screen, "searchHighlightStart");
        int highlightEnd = (int) field(screen, "searchHighlightEnd");
        int firstVisible = (int) field(screen, "firstLine");
        try {
            Method linesPerPage = GuidebookScreen.class.getDeclaredMethod("linesPerPage");
            linesPerPage.setAccessible(true);
            int pageLength = (int) linesPerPage.invoke(screen);
            if (highlighted != expected || highlightEnd <= highlighted
                    || highlighted < firstVisible || highlighted >= firstVisible + pageLength) {
                throw new IllegalStateException("Guidebook search did not highlight the exact visible line");
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not inspect guidebook visible line", error);
        }
    }

    private static Object field(GuidebookScreen screen, String name) {
        try {
            Field field = GuidebookScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not inspect guidebook " + name, error);
        }
    }
}
