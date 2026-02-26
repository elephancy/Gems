package com.feel.gems.entity;

import com.feel.gems.GemsMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;

/**
 * Registry for custom Gems mod entities.
 */
public final class ModEntities {
    
    private static final Identifier SHADOW_CLONE_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "shadow_clone");
    private static final Identifier HUNTER_PACK_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "hunter_pack");
    private static final Identifier TEST_DUMMY_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "test_dummy");
    
    public static final EntityType<ShadowCloneEntity> SHADOW_CLONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SHADOW_CLONE_ID,
            EntityType.Builder.of(ShadowCloneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F) // Same as player
                    .eyeHeight(1.62F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, SHADOW_CLONE_ID))
    );
    
    public static final EntityType<HunterPackEntity> HUNTER_PACK = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            HUNTER_PACK_ID,
            EntityType.Builder.of(HunterPackEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F) // Same as player
                    .eyeHeight(1.62F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, HUNTER_PACK_ID))
    );

    public static final EntityType<TestDummyEntity> TEST_DUMMY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            TEST_DUMMY_ID,
            EntityType.Builder.of(TestDummyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, TEST_DUMMY_ID))
    );

    
    private ModEntities() {}
    
    public static void init() {
        // Register entity attributes
        FabricDefaultAttributeRegistry.register(SHADOW_CLONE, ShadowCloneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HUNTER_PACK, HunterPackEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TEST_DUMMY, TestDummyEntity.createAttributes());
        GemsMod.LOGGER.info("Registered Gems entities");
    }
}
