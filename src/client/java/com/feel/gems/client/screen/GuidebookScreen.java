package com.feel.gems.client.screen;

import static com.feel.gems.client.screen.GemsScreenConstants.*;

import com.feel.gems.client.ClientGemState;
import com.feel.gems.client.modmenu.GemsConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;

/**
 * In-game guidebook for player-facing docs.
 */
public final class GuidebookScreen extends GemsScreenBase {
    private static final int MARGIN = 12;
    private static final int PANEL_TOP = 34;
    private static final int TOPIC_TOP = 62;
    private static final int ENTRY_HEIGHT = 18;
    private static final int ENTRY_STEP = 22;
    private static final int SEARCH_STEP = 31;
    private static final int ACCENT = GemsUiTheme.GOLD;
    private static final int MUTED = GemsUiTheme.MUTED;
    private static final int INK = GemsUiTheme.PALE;
    private static final int SURFACE = GemsUiTheme.PANEL;
    private static final int SIDEBAR = GemsUiTheme.RAIL;
    private static final int LINE = GemsUiTheme.RULE;
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");

    private final List<DocEntry> entries = createEntries();

    private static List<DocEntry> createEntries() {
        List<DocEntry> docs = new ArrayList<>();
        docs.add(new DocEntry("gameplay", "Start here", "gameplay.md", GuideGroup.START));
        if (FabricLoader.getInstance().isModLoaded("second_chance")) {
            docs.add(new DocEntry("adventure", "Find your path", "adventure.md", GuideGroup.START));
            docs.add(new DocEntry("exploration", "Explore further", "exploration.md", GuideGroup.START));
        }
        docs.addAll(List.of(
            new DocEntry("progression", "Progression", "progression.md", GuideGroup.START),
            new DocEntry("gems", "Gems", "gems.md", GuideGroup.POWERS),
            new DocEntry("bonus_pool", "Bonus powers", "bonus_pool.md", GuideGroup.POWERS),
            new DocEntry("synergies", "Synergies", "synergies.md", GuideGroup.POWERS),
            new DocEntry("legendary", "Legendary items", "legendary_items.md", GuideGroup.POWERS),
            new DocEntry("mastery_rivalry", "Mastery & rivals", "mastery_and_rivalry.md", GuideGroup.PEOPLE),
            new DocEntry("titles", "Titles", "titles.md", GuideGroup.PEOPLE),
            new DocEntry("bounties", "Bounties", "bounties.md", GuideGroup.PEOPLE),
            new DocEntry("trust", "Trust & teams", "trust_and_ownership.md", GuideGroup.PEOPLE),
            new DocEntry("controls", "Controls", "controls_and_hud.md", GuideGroup.REFERENCE),
            new DocEntry("assassin", "Assassin", "assassin_endgame.md", GuideGroup.REFERENCE),
            new DocEntry("recipes", "Recipes", "recipes.md", GuideGroup.REFERENCE),
            new DocEntry("test_dummy", "Test dummy", "test_dummy.md", GuideGroup.REFERENCE)
        ));
        return List.copyOf(docs);
    }

    private final Map<String, GuideDoc> docCache = new HashMap<>();

    private int selectedIndex = 0;
    private boolean showStart = true;
    private int topicPage = 0;
    private int contentsPage = 0;
    private boolean showContents = false;
    private boolean showSections = false;
    private boolean showSearch = false;
    private boolean searchOpenedFromHome = false;
    private String searchQuery = "";
    private int searchPage = 0;
    private List<SearchHit> searchHits = List.of();
    private final List<GemsActionButton> searchButtons = new ArrayList<>();
    private EditBox searchField;
    private int sectionPage = 0;
    private int focusedSection = -1;
    private int firstLine = 0;
    private int searchHighlightStart = -1;
    private int searchHighlightEnd = -1;
    private List<FormattedCharSequence> wrappedLines = List.of();
    private List<Integer> sourceLineStarts = List.of();
    private List<Section> sections = List.of();

    private Button previousTopics;
    private Button nextTopics;
    private Button prevPage;
    private Button nextPage;
    private Button prevSection;
    private Button nextSection;

    public GuidebookScreen() {
        super(Component.translatable("gems.screen.guidebook.title"));
    }

    public static void open(Minecraft client) {
        if (client != null) {
            client.gui.setScreen(new GuidebookScreen());
        }
    }

    @Override
    protected void init() {
        super.init();
        topicPage = entries.get(selectedIndex).group().ordinal();
        contentsPage = clampPage(contentsPage, totalPages(entries.size(), contentsPerPage()));
        searchPage = clampPage(searchPage, totalPages(searchHits.size(), searchRows()));
        wrapBody();
        firstLine = Math.clamp(firstLine, 0, lastPageStart());
        sectionPage = clampPage(sectionPage, totalPages(sections.size(), sectionsPerPage()));
        refreshGuideWidgets();
    }

    private void refreshGuideWidgets() {
        setFocused(null);
        clearWidgets();
        previousTopics = null;
        nextTopics = null;
        prevSection = null;
        nextSection = null;
        searchButtons.clear();
        searchField = null;
        if (showStart) {
            buildStartButtons();
            addCloseButton();
            return;
        }
        if (compact()) {
            buildCompactTopics();
        } else {
            buildDocButtons();
        }
        if (showSearch) {
            buildSearchWidgets();
        } else if (showSections) {
            buildSectionIndex();
        }
        buildNavButtons();
        addCloseButton();
        updateNavButtons();
    }

    private void buildStartButtons() {
        action(Component.literal("Topics"), MARGIN + 5, 39, 68, ENTRY_HEIGHT, b -> {
            showStart = false;
            showContents = compact();
            contentsPage = 0;
            wrapBody();
            refreshGuideWidgets();
        }, false);
        action(Component.literal("Search"), MARGIN + 78, 39, 66, ENTRY_HEIGHT,
                b -> toggleSearch(), false);
        GemsActionButton settings = action(Component.literal("Settings"), MARGIN + 149, 39,
                72, ENTRY_HEIGHT,
                b -> Minecraft.getInstance().gui.setScreen(new GemsConfigScreen(this)), false);
        settings.setTooltip(Tooltip.create(Component.literal("Change your Gems controls and settings.")));
        boolean campaign = FabricLoader.getInstance().isModLoaded("second_chance");
        List<StartTask> tasks = startTasks(campaign);
        int columns = homeColumns();
        int itemWidth = (articleWidth() - 20 - (columns - 1) * 8) / columns;
        int step = homeStep(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            StartTask task = tasks.get(i);
            int x = articleLeft() + 10 + (i % columns) * (itemWidth + 8);
            int y = 100 + (i / columns) * step;
            GemsActionButton button = action(Component.literal(task.label()), x, y,
                    itemWidth, columns == 2 ? 34 : ENTRY_HEIGHT,
                    b -> openStartTask(task), i == 0);
            if (columns == 2) button.withDetail(Component.literal(task.description()));
            button.setTooltip(Tooltip.create(Component.literal(task.description())));
        }
    }

    private int homeColumns() {
        return width >= 600 && height >= 300 ? 2 : 1;
    }

    private int homeStep(int taskCount) {
        return homeColumns() == 2 ? 42 : Math.max(23, Math.min(27, (height - 145) / taskCount));
    }

    private static List<StartTask> startTasks(boolean campaign) {
        List<StartTask> tasks = new ArrayList<>();
        tasks.add(new StartTask("Learn the basics", "Get your first gem, use a power, and read your screen.", "gameplay", null));
        String gemSection = hasCurrentGem()
                ? titleCase(ClientGemState.activeGem().name()) + " Gem" : null;
        String gemDescription = gemSection == null ? "Find a gem and read its powers."
                : gemSection + " · " + Math.clamp(ClientGemState.energy(), 0, 10)
                        + "/10 energy. " + nextUnlock(Math.clamp(ClientGemState.energy(), 0, 10));
        tasks.add(new StartTask(gemSection == null ? "Browse gems" : "See my gem",
                gemDescription,
                "gems", gemSection));
        if (campaign) {
            tasks.add(new StartTask("Find a place", "Use the Atlas to find the story's places.", "adventure", null));
        }
        tasks.add(new StartTask("Energy & unlocks", "See what you can use at each energy level.", "progression", null));
        tasks.add(new StartTask("Check controls", "Find keys, saved setups, and screen options.", "controls", null));
        return tasks;
    }

    private static boolean hasCurrentGem() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.player != null && ClientGemState.isInitialized();
    }

    private static String titleCase(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void openStartTask(StartTask task) {
        select(entryIndex(task.docId()));
        if (task.sectionName() != null) {
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).name().equals(task.sectionName())) {
                    selectSection(i);
                    break;
                }
            }
        }
    }

    private GemsActionButton action(Component label, int x, int y, int width, int height,
                          Button.OnPress onPress, boolean selected) {
        return addRenderableWidget(new GemsActionButton(x, y, width, height,
                label, onPress, selected));
    }

    @Override
    protected Button addCloseButton() {
        return action(Component.translatable("gems.screen.button.close"),
                (width - CLOSE_BUTTON_WIDTH) / 2, closeButtonY(height),
                CLOSE_BUTTON_WIDTH, BUTTON_HEIGHT, b -> onClose(), false);
    }

    private int entryIndex(String id) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(id)) return index;
        }
        throw new IllegalStateException("Guidebook start task has no topic: " + id);
    }

    private void buildCompactTopics() {
        action(Component.literal("Home"), MARGIN + 5, 39, 50, ENTRY_HEIGHT,
                b -> openHome(), false);
        action(Component.literal(showContents ? "Read" : "Topics"), MARGIN + 60, 39, 60,
                ENTRY_HEIGHT, b -> toggleContents(), showContents);
        action(Component.literal(showSearch ? "Read" : "Search"), MARGIN + 125, 39, 62,
                ENTRY_HEIGHT, b -> toggleSearch(), showSearch);
        if (showSearch) return;
        if (showContents) {
            int start = contentsPage * contentsPerPage();
            int end = Math.min(entries.size(), start + contentsPerPage());
            int columns = contentsColumns();
            int columnWidth = (articleWidth() - 20 - (columns - 1) * 10) / columns;
            for (int i = start; i < end; i++) {
                int index = i;
                int position = i - start;
                int x = articleLeft() + 10 + (position / contentsRows()) * (columnWidth + 10);
                int y = 96 + (position % contentsRows()) * ENTRY_STEP;
                String title = this.font.plainSubstrByWidth(entries.get(i).defaultTitle(), columnWidth - 12);
                action(Component.literal(title), x, y, columnWidth, ENTRY_HEIGHT,
                        b -> select(index), index == selectedIndex);
            }
        }
    }

    private void buildDocButtons() {
        String shelf = switch (GuideGroup.values()[topicPage]) {
            case START -> "Start";
            case POWERS -> "Powers";
            case PEOPLE -> "Together";
            case REFERENCE -> "Reference";
        };
        action(Component.literal("Home / " + shelf), MARGIN + 5, 39, sidebarWidth() - 10,
                ENTRY_HEIGHT, b -> openHome(), false);
        int row = 0;
        for (int i = 0; i < entries.size(); i++) {
            int idx = i;
            DocEntry entry = entries.get(i);
            if (entry.group().ordinal() != topicPage) continue;
            String title = this.font.plainSubstrByWidth(entry.defaultTitle(), sidebarWidth() - 22);
            action(Component.literal(title), MARGIN + 5,
                    TOPIC_TOP + row++ * ENTRY_STEP,
                    sidebarWidth() - 10, ENTRY_HEIGHT, b -> select(idx), idx == selectedIndex);
        }
        int quickWidth = (sidebarWidth() - 15) / 2;
        action(Component.literal(showSearch ? "Read" : "Search"), MARGIN + 5,
                height - 82, quickWidth, ENTRY_HEIGHT, b -> toggleSearch(), showSearch);
        Button myGem = action(Component.literal("My gem"), MARGIN + 10 + quickWidth,
                height - 82, quickWidth, ENTRY_HEIGHT,
                b -> openStartTask(new StartTask("See my gem", "Your current gem's powers.", "gems",
                        titleCase(ClientGemState.activeGem().name()) + " Gem")), false);
        myGem.active = ClientGemState.isInitialized();
        myGem.setTooltip(Tooltip.create(Component.literal("Open your current gem's powers")));
    }

    private void openHome() {
        showStart = true;
        showContents = false;
        showSections = false;
        showSearch = false;
        searchOpenedFromHome = false;
        refreshGuideWidgets();
    }

    private void buildSearchWidgets() {
        int left = articleLeft();
        searchField = addRenderableWidget(new EditBox(this.font, left + 10, sectionRowY(),
                articleWidth() - 20, ENTRY_HEIGHT, Component.literal("Search guide")));
        searchField.setMaxLength(64);
        searchField.setValue(searchQuery);
        searchField.setResponder(this::onSearchChanged);
        for (int slot = 0; slot < searchRows(); slot++) {
            int resultSlot = slot;
            GemsActionButton result = action(Component.empty(), left + 10,
                    bodyTop() + 18 + slot * SEARCH_STEP, articleWidth() - 20, SEARCH_STEP - 2,
                    b -> openSearchHit(searchPage * searchRows() + resultSlot), false);
            searchButtons.add(result);
        }
        updateSearchButtons();
        setFocused(searchField);
    }

    private int searchRows() {
        int navY = compact() ? height - 26 : height - 56;
        return Math.max(1, (navY - bodyTop() - 24) / SEARCH_STEP);
    }

    private void onSearchChanged(String value) {
        if (!showSearch || searchField == null) return;
        searchQuery = value;
        searchPage = 0;
        searchHits = findSearchHits(value);
        updateSearchButtons();
        updateNavButtons();
    }

    private List<SearchHit> findSearchHits(String value) {
        String query = value.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return List.of();
        List<SearchHit> matches = new ArrayList<>();
        for (int docIndex = 0; docIndex < entries.size(); docIndex++) {
            DocEntry entry = entries.get(docIndex);
            GuideDoc doc = loadDoc(entry);
            boolean titleMatch = (entry.defaultTitle() + " " + doc.title())
                    .toLowerCase(Locale.ROOT).contains(query);
            if (titleMatch) {
                matches.add(new SearchHit(docIndex, 0, entry.defaultTitle(), "Start at the beginning", false));
            }
            String sectionName = "Overview";
            boolean started = false;
            List<SearchLine> sectionLines = new ArrayList<>();
            for (int rawIndex = 0; rawIndex < doc.lines().size(); rawIndex++) {
                DocLine line = doc.lines().get(rawIndex);
                String text = line.text().getString();
                if (!started && text.isBlank()) continue;
                if (line.heading() != null) {
                    if (started) {
                        appendSectionMatch(matches, docIndex, entry.defaultTitle(),
                                sectionName, sectionLines, query);
                    }
                    sectionName = line.heading();
                    sectionLines = new ArrayList<>();
                }
                started = true;
                sectionLines.add(new SearchLine(rawIndex, text));
            }
            if (started) {
                appendSectionMatch(matches, docIndex, entry.defaultTitle(),
                        sectionName, sectionLines, query);
            }
        }
        return List.copyOf(matches);
    }

    private static void appendSectionMatch(List<SearchHit> matches, int docIndex,
                                           String docTitle, String sectionName, List<SearchLine> lines,
                                           String query) {
        SearchLine matched = null;
        for (SearchLine line : lines) {
            if (line.text().toLowerCase(Locale.ROOT).contains(query)) {
                matched = line;
                break;
            }
        }
        if (matched != null) {
            matches.add(new SearchHit(docIndex, matched.rawIndex(), docTitle + " / " + sectionName,
                    matched.text().strip(), true));
        }
    }

    private void updateSearchButtons() {
        int start = searchPage * searchRows();
        for (int slot = 0; slot < searchButtons.size(); slot++) {
            GemsActionButton button = searchButtons.get(slot);
            int index = start + slot;
            button.visible = index < searchHits.size();
            if (button.visible) {
                SearchHit hit = searchHits.get(index);
                button.setMessage(Component.literal(this.font.plainSubstrByWidth(hit.label(),
                        button.getWidth() - 16)));
                button.withDetail(Component.literal(hit.preview()));
                button.setTooltip(Tooltip.create(Component.literal(hit.preview())));
            }
        }
    }

    private void openSearchHit(int index) {
        if (index < 0 || index >= searchHits.size()) return;
        SearchHit hit = searchHits.get(index);
        select(hit.docIndex());
        if (hit.rawLineIndex() >= sourceLineStarts.size()) return;
        int targetLine = sourceLineStarts.get(hit.rawLineIndex());
        firstLine = Math.min(targetLine, lastPageStart());
        if (hit.highlight()) {
            searchHighlightStart = targetLine;
            searchHighlightEnd = hit.rawLineIndex() + 1 < sourceLineStarts.size()
                    ? sourceLineStarts.get(hit.rawLineIndex() + 1) : wrappedLines.size();
        }
        refreshGuideWidgets();
    }

    private void buildSectionIndex() {
        int start = sectionPage * sectionsPerPage();
        int end = Math.min(sections.size(), start + sectionsPerPage());
        int columns = sectionColumns();
        int columnWidth = (articleWidth() - 30 - (columns - 1) * 10) / columns;
        for (int i = start; i < end; i++) {
            int sectionIndex = i;
            int position = i - start;
            int x = articleLeft() + 10 + (position / sectionRows()) * (columnWidth + 10);
            int y = bodyTop() + 18 + (position % sectionRows()) * ENTRY_STEP;
            String label = (i + 1) + ". " + sections.get(i).name();
            label = this.font.plainSubstrByWidth(label, columnWidth - 12);
            Button jump = action(Component.literal(label), x, y, columnWidth,
                    ENTRY_HEIGHT, b -> selectSection(sectionIndex), sectionIndex == activeSection());
            jump.setTooltip(Tooltip.create(Component.literal((i + 1) + ". " + sections.get(i).name())));
        }
    }

    private void buildNavButtons() {
        int navY = compact() ? height - 26 : height - 56;
        if (!compact()) {
            int sidebarButtonWidth = (sidebarWidth() - 15) / 2;
            previousTopics = action(Component.literal("Back"), MARGIN + 5, navY,
                    sidebarButtonWidth, ENTRY_HEIGHT, b -> changeTopics(-1), false);
            nextTopics = action(Component.literal("More"), MARGIN + 10 + sidebarButtonWidth,
                    navY, sidebarButtonWidth, ENTRY_HEIGHT, b -> changeTopics(1), false);
            previousTopics.setTooltip(Tooltip.create(Component.literal("Previous topics")));
            nextTopics.setTooltip(Tooltip.create(Component.literal("More topics")));
        }
        int articleLeft = articleLeft();
        int articleButtonWidth = Math.min(70, (articleWidth() - 30) / 2);
        if (compact()) {
            articleButtonWidth = Math.min(articleButtonWidth, Math.max(24,
                    (width - CLOSE_BUTTON_WIDTH - 4 * MARGIN) / 2));
        }
        prevPage = action(Component.translatable("gems.screen.button.prev"),
                articleLeft + 10, navY, articleButtonWidth, ENTRY_HEIGHT,
                b -> changePage(-1), false);
        nextPage = action(Component.translatable("gems.screen.button.next"),
                articleLeft + articleWidth() - 10 - articleButtonWidth, navY,
                articleButtonWidth, ENTRY_HEIGHT, b -> changePage(1), false);
        if (showSearch) {
            return;
        }
        if (compact() && showContents) {
            return;
        }
        int sectionY = sectionRowY();
        if (showSections) {
            action(Component.literal("Read"), articleLeft() + 42, sectionY,
                    articleWidth() - 84, ENTRY_HEIGHT, b -> toggleSections(), false);
            return;
        }
        prevSection = action(Component.literal("<"), articleLeft + 10, sectionY,
                24, ENTRY_HEIGHT, b -> changeSection(-1), false);
        nextSection = action(Component.literal(">"), articleLeft + articleWidth() - 34,
                sectionY, 24, ENTRY_HEIGHT, b -> changeSection(1), false);
        prevSection.setTooltip(Tooltip.create(Component.literal("Previous section")));
        nextSection.setTooltip(Tooltip.create(Component.literal("Next section")));
        String label = "Jump: " + sections.get(activeSection()).name() + " ("
                + (activeSection() + 1) + "/" + sections.size() + ")";
        label = this.font.plainSubstrByWidth(label, articleWidth() - 92);
        action(Component.literal(label), articleLeft + 42, sectionY,
                articleWidth() - 84, ENTRY_HEIGHT, b -> toggleSections(), false);
    }

    private boolean compact() {
        return width < 430;
    }

    private int sidebarWidth() {
        return Math.min(142, Math.max(104, width / 3));
    }

    private int articleLeft() {
        return compact() || showStart ? MARGIN : MARGIN + sidebarWidth() + 8;
    }

    private int articleWidth() {
        return Math.max(90, width - articleLeft() - MARGIN);
    }

    private int readingWidth() {
        return Math.min(500, articleWidth() - 20);
    }

    private int readingLeft() {
        return articleLeft() + (articleWidth() - readingWidth()) / 2;
    }

    private int articleTop() {
        return compact() || showStart ? 64 : PANEL_TOP;
    }

    private int sectionRowY() {
        return compact() ? 68 : 63;
    }

    private int bodyTop() {
        return compact() ? 94 : 87;
    }

    private int contentsRows() {
        return Math.max(1, Math.min(4, (height - 148) / ENTRY_STEP));
    }

    private int contentsColumns() {
        return articleWidth() >= 360 ? 2 : 1;
    }

    private int contentsPerPage() {
        return contentsColumns() * contentsRows();
    }

    private int sectionColumns() {
        return articleWidth() >= 350 ? 2 : 1;
    }

    private int sectionRows() {
        int navY = compact() ? height - 26 : height - 56;
        return Math.max(1, Math.min(12, (navY - bodyTop() - 18) / ENTRY_STEP));
    }

    private int sectionsPerPage() {
        return sectionColumns() * sectionRows();
    }

    private void toggleContents() {
        showStart = false;
        showContents = !showContents;
        showSections = false;
        showSearch = false;
        if (showContents) {
            contentsPage = selectedIndex / contentsPerPage();
        }
        refreshGuideWidgets();
    }

    private void changeTopics(int delta) {
        topicPage = clampPage(topicPage + delta, GuideGroup.values().length);
        refreshGuideWidgets();
    }

    private void select(int index) {
        showStart = false;
        searchOpenedFromHome = false;
        selectedIndex = Math.max(0, Math.min(entries.size() - 1, index));
        showContents = false;
        showSections = false;
        showSearch = false;
        focusedSection = -1;
        searchHighlightStart = -1;
        searchHighlightEnd = -1;
        topicPage = entries.get(selectedIndex).group().ordinal();
        firstLine = 0;
        wrapBody();
        refreshGuideWidgets();
    }

    private void changePage(int delta) {
        if (showStart) return;
        if (showSearch) {
            searchPage = clampPage(searchPage + delta, totalPages(searchHits.size(), searchRows()));
            updateSearchButtons();
            updateNavButtons();
            return;
        }
        if (compact() && showContents) {
            contentsPage = clampPage(contentsPage + delta, totalPages(entries.size(), contentsPerPage()));
            refreshGuideWidgets();
            return;
        }
        if (showSections) {
            sectionPage = clampPage(sectionPage + delta, totalPages(sections.size(), sectionsPerPage()));
            refreshGuideWidgets();
            return;
        }
        if (delta < 0 && firstLine == 0 && selectedIndex > 0) {
            select(selectedIndex - 1);
            firstLine = lastPageStart();
            updateNavButtons();
            return;
        }
        if (delta > 0 && firstLine == lastPageStart() && selectedIndex + 1 < entries.size()) {
            select(selectedIndex + 1);
            return;
        }
        focusedSection = -1;
        firstLine = Math.clamp(firstLine + delta * linesPerPage(), 0, lastPageStart());
        updateNavButtons();
    }

    private void changeSection(int delta) {
        int target = activeSection() + delta;
        if (target >= 0 && target < sections.size()) {
            firstLine = Math.min(sections.get(target).lineIndex(), lastPageStart());
            focusedSection = target;
            updateNavButtons();
        }
    }

    private void toggleSections() {
        showSections = !showSections;
        showContents = false;
        showSearch = false;
        if (showSections) {
            sectionPage = activeSection() / sectionsPerPage();
        }
        refreshGuideWidgets();
    }

    private void selectSection(int index) {
        firstLine = Math.min(sections.get(index).lineIndex(), lastPageStart());
        focusedSection = index;
        showSections = false;
        refreshGuideWidgets();
    }

    private void toggleSearch() {
        boolean fromHome = showStart;
        if (showSearch && searchOpenedFromHome) {
            openHome();
            return;
        }
        if (!showSearch) searchOpenedFromHome = fromHome;
        showStart = false;
        showSearch = !showSearch;
        showContents = false;
        showSections = false;
        if (showSearch) {
            searchPage = 0;
            searchHits = findSearchHits(searchQuery);
        }
        if (fromHome) wrapBody();
        refreshGuideWidgets();
    }

    private int activeSection() {
        if (focusedSection >= 0 && focusedSection < sections.size()) {
            return focusedSection;
        }
        int active = 0;
        for (int i = 1; i < sections.size(); i++) {
            if (sections.get(i).lineIndex() > firstLine) {
                break;
            }
            active = i;
        }
        return active;
    }

    private void updateNavButtons() {
        if (previousTopics != null) {
            previousTopics.active = topicPage > 0;
        }
        if (nextTopics != null) {
            nextTopics.active = topicPage + 1 < GuideGroup.values().length;
        }
        if (showSearch) {
            prevPage.active = searchPage > 0;
            nextPage.active = searchPage + 1 < totalPages(searchHits.size(), searchRows());
            return;
        }
        if (compact() && showContents) {
            prevPage.active = contentsPage > 0;
            nextPage.active = contentsPage + 1 < totalPages(entries.size(), contentsPerPage());
            return;
        }
        if (showSections) {
            prevPage.active = sectionPage > 0;
            nextPage.active = sectionPage + 1 < totalPages(sections.size(), sectionsPerPage());
            return;
        }
        prevPage.active = firstLine > 0 || selectedIndex > 0;
        nextPage.active = firstLine < lastPageStart() || selectedIndex + 1 < entries.size();
        prevPage.setTooltip(Tooltip.create(Component.literal(
                firstLine > 0 ? "Previous page" : "Previous topic")));
        nextPage.setTooltip(Tooltip.create(Component.literal(
                firstLine < lastPageStart() ? "Next page" : "Next topic")));
        prevSection.active = activeSection() > 0;
        nextSection.active = activeSection() + 1 < sections.size();
    }

    private int lastPageStart() {
        return wrappedLines.isEmpty() ? 0
                : (wrappedLines.size() - 1) / linesPerPage() * linesPerPage();
    }

    private void wrapBody() {
        DocEntry entry = entries.get(selectedIndex);
        GuideDoc doc = loadDoc(entry);
        int wrapWidth = Math.max(70, readingWidth());
        List<FormattedCharSequence> lines = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<DocLine> rawLines = doc.lines();
        if (rawLines == null || rawLines.isEmpty()) {
            rawLines = List.of(new DocLine(Component.literal("(No content)"), null));
        }
        List<Section> found = new ArrayList<>();
        found.add(new Section("Overview", 0));
        for (DocLine source : rawLines) {
            starts.add(lines.size());
            Component raw = source.text();
            if (lines.isEmpty() && (raw == null || raw.getString().isBlank())) {
                continue;
            }
            if (source.heading() != null) {
                if (lines.isEmpty()) {
                    found.clear();
                }
                found.add(new Section(source.heading(), lines.size()));
            }
            if (raw == null || raw.getString().isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(this.font.split(raw, wrapWidth));
        }
        wrappedLines = lines;
        sourceLineStarts = starts;
        sections = found;
    }

    private int linesPerPage() {
        int available = height - (compact() ? 49 : 75) - bodyTop();
        return Math.max(1, available / (this.font.lineHeight + 2));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (compact() || showStart) {
            drawBookPanel(context, MARGIN, PANEL_TOP, width - MARGIN, 61, SIDEBAR);
        } else {
            drawBookPanel(context, MARGIN, PANEL_TOP, MARGIN + sidebarWidth(), height - 36, SIDEBAR);
        }
        drawBookPanel(context, articleLeft(), articleTop(), articleLeft() + articleWidth(),
                height - (compact() || showStart ? 5 : 36), SURFACE);
        if (showStart) {
            List<StartTask> tasks = startTasks(FabricLoader.getInstance().isModLoaded("second_chance"));
            int step = homeStep(tasks.size());
            int columns = homeColumns();
            int itemWidth = (articleWidth() - 20 - (columns - 1) * 8) / columns;
            for (int i = 0; i < tasks.size(); i++) {
                int x = articleLeft() + 10 + (i % columns) * (itemWidth + 8);
                int y = 98 + (i / columns) * step;
                GemsUiTheme.card(context, x - 2, y, x + itemWidth + 2,
                        y + (columns == 2 ? 38 : 21), i == 0 ? ACCENT : LINE);
            }
        }
        if (!compact()) {
            int selectedRow = 0;
            for (int i = 0; i < selectedIndex; i++) {
                if (entries.get(i).group().ordinal() == topicPage) selectedRow++;
            }
            if (!showStart && entries.get(selectedIndex).group().ordinal() == topicPage) {
                int selectedY = TOPIC_TOP + selectedRow * ENTRY_STEP;
                context.fill(MARGIN + 3, selectedY - 2, MARGIN + sidebarWidth() - 3,
                        selectedY + ENTRY_HEIGHT + 2, GemsUiTheme.WELL);
                context.fill(MARGIN + 3, selectedY - 2, MARGIN + 6,
                        selectedY + ENTRY_HEIGHT + 2, ACCENT);
            }
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        renderBase(context);

        if (showStart) {
            if (width >= 340) {
                context.text(font, "FIELD GUIDE", width - MARGIN - 88, 44, ACCENT);
            }
            context.text(font, "What would you like to do?", articleLeft() + 10, articleTop() + 12, INK);
            GemsUiTheme.rule(context, articleLeft() + 10, articleTop() + 27,
                    articleLeft() + articleWidth() - 10, 0);
            drawHomeOverview(context);
            return;
        }

        if (showSearch) {
            if (compact()) {
                context.centeredText(this.font, "SEARCH", width / 2 + 30, 44, ACCENT);
            } else {
                context.text(this.font, "SEARCH GUIDE", articleLeft() + 10,
                        articleTop() + 10, INK);
            }
            String status = searchQuery.isBlank() ? "Try a name or effect, such as dash"
                    : searchHits.isEmpty() ? "No matches. Try another word."
                    : searchHits.size() + " matches  ·  " + (searchPage + 1) + " / "
                            + totalPages(searchHits.size(), searchRows());
            context.text(this.font, this.font.plainSubstrByWidth(status, articleWidth() - 20),
                    articleLeft() + 10, bodyTop(), MUTED);
            return;
        }

        DocEntry entry = entries.get(selectedIndex);
        if (compact()) {
            if (showContents) {
                int start = contentsPage * contentsPerPage();
                int end = Math.min(entries.size(), start + contentsPerPage());
                context.text(this.font, "CONTENTS", width - MARGIN - 80, 44, ACCENT);
                context.text(this.font, "Choose a topic", articleLeft() + 10, articleTop() + 10, INK);
                context.fill(articleLeft() + 10, articleTop() + 24,
                        articleLeft() + articleWidth() - 10, articleTop() + 25, LINE);
                if (selectedIndex >= start && selectedIndex < end) {
                    int position = selectedIndex - start;
                    int columnWidth = (articleWidth() - 20 - (contentsColumns() - 1) * 10) / contentsColumns();
                    int x = articleLeft() + 10 + (position / contentsRows()) * (columnWidth + 10);
                    int y = 96 + (position % contentsRows()) * ENTRY_STEP;
                    context.fill(x - 4, y, x - 2, y + ENTRY_HEIGHT, ACCENT);
                }
                context.text(this.font, "Chapters " + (start + 1) + "-" + end + " of " + entries.size(),
                        articleLeft() + 10, height - 47, MUTED);
                return;
            }
            String topic = (selectedIndex + 1) + "/" + entries.size() + "  " + entry.defaultTitle();
            String label = this.font.plainSubstrByWidth(topic, Math.max(50, width - 2 * MARGIN - 200));
            context.text(this.font, label, MARGIN + 205, 44, ACCENT);
        } else {
            GemsUiTheme.segments(context, MARGIN + 8, height - 76,
                    GuideGroup.values().length, topicPage + 1, ACCENT);
        }

        int left = articleLeft();
        if (!compact()) {
            String chapter = String.format("%02d / %02d", selectedIndex + 1, entries.size());
            int titleWidth = articleWidth() - 20 - (articleWidth() >= 210 ? this.font.width(chapter) + 12 : 0);
            context.text(this.font,
                    Component.literal(this.font.plainSubstrByWidth(entry.defaultTitle(), titleWidth)),
                    left + 10, articleTop() + 10, INK);
            if (articleWidth() >= 210) {
                context.text(this.font, chapter,
                        left + articleWidth() - 10 - this.font.width(chapter), articleTop() + 10, ACCENT);
            }
        }
        GemsUiTheme.rule(context, left + 10, articleTop() + 24, left + articleWidth() - 10, 0);
        if (showSections) {
            int start = sectionPage * sectionsPerPage();
            int end = Math.min(sections.size(), start + sectionsPerPage());
            context.text(this.font, "Jump to a section  " + (start + 1) + "-" + end + " / " + sections.size(),
                    left + 10, bodyTop(), MUTED);
            int active = activeSection();
            if (active >= start && active < end) {
                int position = active - start;
                int columnWidth = (articleWidth() - 30 - (sectionColumns() - 1) * 10) / sectionColumns();
                int x = left + 10 + (position / sectionRows()) * (columnWidth + 10);
                int y = bodyTop() + 18 + (position % sectionRows()) * ENTRY_STEP;
                context.fill(x - 4, y, x - 2, y + ENTRY_HEIGHT, ACCENT);
            }
            return;
        }
        int visibleEnd = Math.min(wrappedLines.size(), firstLine + linesPerPage());
        int progress = wrappedLines.isEmpty() ? 0
                : (int) ((long) visibleEnd * (articleWidth() - 20) / wrappedLines.size());
        GemsUiTheme.rule(context, left + 10, articleTop() + 24, left + articleWidth() - 10, progress);

        int linesPerPage = linesPerPage();
        int start = firstLine;
        int end = Math.min(wrappedLines.size(), start + linesPerPage);
        int y = bodyTop();
        for (int i = start; i < end; i++) {
            if (i >= searchHighlightStart && i < searchHighlightEnd) {
                context.fill(readingLeft() - 4, y - 1,
                        readingLeft() + readingWidth() + 3, y + this.font.lineHeight + 2,
                        GemsUiTheme.WELL);
                context.fill(readingLeft() - 4, y - 1,
                        readingLeft() - 2, y + this.font.lineHeight + 2, ACCENT);
            }
            context.text(this.font, wrappedLines.get(i), readingLeft(), y, INK);
            y += this.font.lineHeight + 2;
        }
        int pageCount = totalPages(wrappedLines.size(), linesPerPage);
        int pageNumber = Math.min(pageCount, firstLine / linesPerPage + 1);
        String footer = "Page " + pageNumber + " / " + pageCount + "   ·   Wheel or PgUp/PgDn";
        context.text(this.font, this.font.plainSubstrByWidth(footer, readingWidth()),
                readingLeft(), height - (compact() ? 47 : 75), MUTED);
    }

    private void drawHomeOverview(GuiGraphicsExtractor context) {
        if (homeColumns() != 2) return;
        int taskCount = startTasks(FabricLoader.getInstance().isModLoaded("second_chance")).size();
        int top = 100 + ((taskCount + 1) / 2) * homeStep(taskCount) + 8;
        int bottom = Math.min(height - 44, top + 86);
        if (bottom - top < 72) return;
        int left = articleLeft() + 10;
        int right = articleLeft() + articleWidth() - 10;
        GemsUiTheme.panel(context, left, top, right, bottom, GemsUiTheme.WELL, false);
        if (!hasCurrentGem()) {
            context.text(font, "YOUR GEM", left + 12, top + 9, ACCENT);
            GemsUiTheme.rule(context, left + 12, top + 23, right - 12, 0);
            context.text(font, "Gem and energy details are not available yet.", left + 12, top + 34, INK);
            context.text(font, "Join a world to see what you can use next.", left + 12, top + 50, MUTED);
            return;
        }
        int energy = Math.clamp(ClientGemState.energy(), 0, 10);
        String gem = titleCase(ClientGemState.activeGem().name());
        context.text(font, font.plainSubstrByWidth("YOUR GEM  /  " + gem.toUpperCase(Locale.ROOT),
                right - left - 24), left + 12, top + 9, ACCENT);
        GemsUiTheme.rule(context, left + 12, top + 23, right - 12, 0);
        context.text(font, font.plainSubstrByWidth("Energy " + energy + "/10  ·  " + currentUnlock(energy),
                right - left - 24), left + 12, top + 34, INK);
        context.text(font, font.plainSubstrByWidth(nextUnlock(energy), right - left - 24),
                left + 12, top + 50, MUTED);
        GemsUiTheme.segments(context, left + 12, bottom - 12, 10, energy, ACCENT);
    }

    private static String currentUnlock(int energy) {
        return switch (energy) {
            case 0 -> "Powers are dormant";
            case 1 -> "Passives are ready";
            case 2 -> "First ability is ready";
            case 3 -> "First two abilities are ready";
            case 4 -> "First three abilities are ready";
            case 10 -> "Bonus powers are ready to choose";
            default -> "All gem abilities are ready";
        };
    }

    private static String nextUnlock(int energy) {
        return switch (energy) {
            case 0 -> "Next at 1: your passives.";
            case 1 -> "Next at 2: your first ability.";
            case 2 -> "Next at 3: your second ability.";
            case 3 -> "Next at 4: your third ability.";
            case 4 -> "Next at 5: all remaining abilities.";
            case 10 -> "Every energy milestone is open.";
            default -> "Next at 10: choose bonus powers.";
        };
    }

    private static void drawBookPanel(GuiGraphicsExtractor context, int left, int top, int right, int bottom, int fill) {
        GemsUiTheme.panel(context, left, top, right, bottom, fill, true);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GemsUiTheme.backdrop(context, width, height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= articleLeft() && mouseX < articleLeft() + articleWidth()
                && mouseY >= bodyTop() && mouseY < height - (compact() ? 37 : 60)
                && verticalAmount != 0) {
            changePage(verticalAmount > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent key) {
        if (key.key() == InputConstants.KEY_F && key.hasControlDown()) {
            if (!showSearch) toggleSearch();
            else if (searchField != null) setFocused(searchField);
            return true;
        }
        if (key.key() == InputConstants.KEY_ESCAPE && (showSearch || showSections || showContents)) {
            if (showSearch && searchOpenedFromHome) {
                openHome();
                return true;
            }
            showSearch = false;
            showSections = false;
            showContents = false;
            refreshGuideWidgets();
            return true;
        }
        if (showStart) return super.keyPressed(key);
        if (showSearch && searchField != null && searchField.isFocused()
                && (key.key() == InputConstants.KEY_HOME || key.key() == InputConstants.KEY_END)) {
            return super.keyPressed(key);
        }
        switch (key.key()) {
            case InputConstants.KEY_PAGEUP -> changePage(-1);
            case InputConstants.KEY_PAGEDOWN -> changePage(1);
            case InputConstants.KEY_HOME -> {
                if (showSearch) {
                    searchPage = 0;
                    updateSearchButtons();
                    updateNavButtons();
                } else if (compact() && showContents) {
                    contentsPage = 0;
                    refreshGuideWidgets();
                } else if (showSections) {
                    sectionPage = 0;
                    refreshGuideWidgets();
                } else {
                    focusedSection = -1;
                    firstLine = 0;
                    updateNavButtons();
                }
            }
            case InputConstants.KEY_END -> {
                if (showSearch) {
                    searchPage = totalPages(searchHits.size(), searchRows()) - 1;
                    updateSearchButtons();
                    updateNavButtons();
                } else if (compact() && showContents) {
                    contentsPage = totalPages(entries.size(), contentsPerPage()) - 1;
                    refreshGuideWidgets();
                } else if (showSections) {
                    sectionPage = totalPages(sections.size(), sectionsPerPage()) - 1;
                    refreshGuideWidgets();
                } else {
                    focusedSection = -1;
                    firstLine = lastPageStart();
                    updateNavButtons();
                }
            }
            default -> {
                return super.keyPressed(key);
            }
        }
        return true;
    }

    private GuideDoc loadDoc(DocEntry entry) {
        GuideDoc cached = docCache.get(entry.id());
        if (cached != null) {
            return cached;
        }
        GuideDoc loaded = readDoc(entry);
        docCache.put(entry.id(), loaded);
        return loaded;
    }

    private GuideDoc readDoc(DocEntry entry) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return new GuideDoc(entry.defaultTitle(), List.of(
                    new DocLine(Component.literal("Guidebook data unavailable."), null)));
        }
        Identifier id = Identifier.fromNamespaceAndPath("gems", "guidebook/" + entry.resourcePath());
        try {
            Resource resource = client.getResourceManager().getResourceOrThrow(id);
            try (InputStream stream = resource.open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder raw = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    raw.append(line).append("\n");
                }
                return parseDoc(entry.defaultTitle(), raw.toString());
            }
        } catch (IOException e) {
            return new GuideDoc(entry.defaultTitle(), List.of(
                    new DocLine(Component.literal("Missing guidebook entry: " + entry.resourcePath()), null)));
        }
    }

    private GuideDoc parseDoc(String fallbackTitle, String raw) {
        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        String title = null;
        List<DocLine> body = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (title == null && trimmed.startsWith("#")) {
                title = trimmed.replaceFirst("^#+\\s*", "").trim();
                if (!title.isEmpty()) {
                    continue;
                }
            }
            if (trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                String heading = trimmed.replaceFirst("^#+\\s*", "").trim();
                if (!heading.isEmpty()) {
                    body.add(new DocLine(formatHeading(level, heading), level >= 2 ? heading : null));
                }
                continue;
            }
            if (trimmed.isEmpty()) {
                body.add(new DocLine(Component.empty(), null));
                continue;
            }
            if (trimmed.startsWith("- ")) {
                line = "- " + trimmed.substring(2);
            }
            line = LINK_PATTERN.matcher(line).replaceAll("$1");
            body.add(new DocLine(parseInline(line), null));
        }
        if (body.isEmpty()) {
            body = List.of(new DocLine(Component.literal("(No content)"), null));
        }
        return new GuideDoc(title != null && !title.isEmpty() ? title : fallbackTitle, body);
    }

    private static Component formatHeading(int level, String text) {
        ChatFormatting color = switch (level) {
            case 1 -> ChatFormatting.GOLD;
            case 2 -> ChatFormatting.YELLOW;
            case 3 -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
        return Component.literal(text).withStyle(ChatFormatting.BOLD, color);
    }

    private static Component parseInline(String raw) {
        MutableComponent out = Component.empty();
        StringBuilder buf = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean code = false;
        TextColor color = null;

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                buf.append(raw.charAt(i + 1));
                i += 2;
                continue;
            }
            if (raw.startsWith("{/}", i)) {
                flush(buf, out, styleFor(bold, italic, underline, code, color));
                color = null;
                i += 3;
                continue;
            }
            if (raw.startsWith("{#", i) && i + 8 < raw.length() && raw.charAt(i + 8) == '}') {
                String hex = raw.substring(i + 2, i + 8);
                try {
                    int rgb = Integer.parseInt(hex, 16);
                    flush(buf, out, styleFor(bold, italic, underline, code, color));
                    color = TextColor.fromRgb(rgb);
                    i += 9;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            if (raw.startsWith("**", i)) {
                flush(buf, out, styleFor(bold, italic, underline, code, color));
                bold = !bold;
                i += 2;
                continue;
            }
            if (raw.startsWith("__", i)) {
                flush(buf, out, styleFor(bold, italic, underline, code, color));
                underline = !underline;
                i += 2;
                continue;
            }
            if (c == '*') {
                flush(buf, out, styleFor(bold, italic, underline, code, color));
                italic = !italic;
                i += 1;
                continue;
            }
            if (c == '`') {
                flush(buf, out, styleFor(bold, italic, underline, code, color));
                code = !code;
                i += 1;
                continue;
            }
            buf.append(c);
            i += 1;
        }
        flush(buf, out, styleFor(bold, italic, underline, code, color));
        return out;
    }

    private static void flush(StringBuilder buf, MutableComponent out, Style style) {
        if (buf.length() == 0) {
            return;
        }
        out.append(Component.literal(buf.toString()).setStyle(style));
        buf.setLength(0);
    }

    private static Style styleFor(boolean bold, boolean italic, boolean underline, boolean code, TextColor color) {
        Style style = Style.EMPTY.withBold(bold).withItalic(italic).withUnderlined(underline);
        if (color != null) {
            return style.withColor(color);
        }
        if (code) {
            return style.withColor(ChatFormatting.GRAY);
        }
        return style;
    }

    private enum GuideGroup {
        START, POWERS, PEOPLE, REFERENCE
    }

    private record DocEntry(String id, String defaultTitle, String resourcePath, GuideGroup group) {
    }

    private record StartTask(String label, String description, String docId, String sectionName) {
    }

    private record SearchHit(int docIndex, int rawLineIndex, String label, String preview, boolean highlight) {
    }

    private record SearchLine(int rawIndex, String text) {
    }

    private record GuideDoc(String title, List<DocLine> lines) {
    }

    private record DocLine(Component text, String heading) {
    }

    private record Section(String name, int lineIndex) {
    }
}
