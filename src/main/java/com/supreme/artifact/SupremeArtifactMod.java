package com.supreme.artifact;

import com.mojang.logging.LogUtils;
import com.supreme.artifact.event.BladeAttackHandler;
import com.supreme.artifact.event.CharmAttackHandler;
import com.supreme.artifact.event.DefenseHandler;
import com.supreme.artifact.event.LifeInterferenceHandler;
import com.supreme.artifact.event.OffenseHandler;
import com.supreme.artifact.init.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SupremeArtifactMod.MOD_ID)
public class SupremeArtifactMod {

    public static final String MOD_ID = "supreme_artifact";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SupremeArtifactMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // Register items
        ModItems.register(modEventBus);

        // Register attributes (生命妨害值系统)
        LifeInterferenceHandler.ATTRIBUTES.register(modEventBus);

        // Register defense handlers (L1-L8) — all at HIGHEST priority
        DefenseHandler.register(forgeBus);

        // Register AoE offense handlers (万法归一)
        OffenseHandler.register(forgeBus);

        // Register single-target blade handlers (终焉之刃)
        BladeAttackHandler.register(forgeBus);

        // Register charm attack handlers (星辰箭矢)
        forgeBus.register(new CharmAttackHandler());

        LOGGER.info("Supreme Artifact mod loaded — all shall bow.");
    }
}
