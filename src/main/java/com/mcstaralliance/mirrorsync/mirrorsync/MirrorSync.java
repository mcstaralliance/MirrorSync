package com.mcstaralliance.mirrorsync.mirrorsync;

import com.mcstaralliance.mirrorsync.mirrorsync.service.FileSyncService;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod(MirrorSync.MODID)
public class MirrorSync {

    public MirrorSync(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(MirrorSync::onConstruct);
    }

    public static final String MODID = "mirrorsync";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void onConstruct(FMLConstructModEvent event) {
        event.enqueueWork(() -> FileSyncService.getInstance().sync(FMLPaths.GAMEDIR.get()));
    }
}
