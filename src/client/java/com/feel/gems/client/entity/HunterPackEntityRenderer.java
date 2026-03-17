package com.feel.gems.client.entity;

import com.feel.gems.client.ClientShadowCloneState;
import com.feel.gems.entity.HunterPackEntity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Renders HunterPackEntity (Six-Pack Pain clones) with a humanoid (biped) model.
 * Uses the owner's skin texture if available, otherwise falls back to Steve.
 * Reuses ClientShadowCloneState for owner UUID tracking.
 */
public class HunterPackEntityRenderer extends MobRenderer<HunterPackEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    
    private static final Identifier STEVE_SKIN = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
    
    // Store current entity being rendered for texture lookup
    private HunterPackEntity currentEntity;
    
    public HunterPackEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }
    
    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }
    
    @Override
    public void extractRenderState(HunterPackEntity entity, HumanoidRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        this.currentEntity = entity;
    }
    
    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        if (currentEntity == null) {
            return STEVE_SKIN;
        }
        
        // Try to get owner's skin from client state (reuses ShadowClone's state tracker)
        UUID ownerUuid = ClientShadowCloneState.getOwner(currentEntity.getId());
        if (ownerUuid == null) {
            return STEVE_SKIN;
        }
        
        // Look up skin from player list
        Identifier skinId = getSkinIdForUuid(ownerUuid);
        if (skinId != null) {
            return skinId;
        }
        
        return STEVE_SKIN;
    }
    
    /**
     * Get skin texture Identifier for a player UUID from the player list.
     */
    private Identifier getSkinIdForUuid(UUID uuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return null;
        }
        
        PlayerInfo entry = client.getConnection().getPlayerInfo(uuid);
        if (entry == null) {
            return null;
        }
        
        PlayerSkin skinTextures = entry.getSkin();
        if (skinTextures == null) {
            return null;
        }
        
        // SkinTextures record has body() which returns AssetInfo.TextureAsset
        // TextureAsset has texturePath() which returns Identifier
        return skinTextures.body().texturePath();
    }
}
