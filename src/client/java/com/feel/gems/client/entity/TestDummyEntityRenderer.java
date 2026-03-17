package com.feel.gems.client.entity;

import com.feel.gems.entity.TestDummyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the test dummy using the default player model/skin.
 */
public final class TestDummyEntityRenderer extends MobRenderer<TestDummyEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier STEVE_SKIN = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public TestDummyEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return STEVE_SKIN;
    }
}
