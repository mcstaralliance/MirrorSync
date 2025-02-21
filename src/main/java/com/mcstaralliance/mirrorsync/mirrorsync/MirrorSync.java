package com.mcstaralliance.mirrorsync.mirrorsync;

import com.mcstaralliance.mirrorsync.mirrorsync.service.FileSyncService;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod(MirrorSync.MODID)
public class MirrorSync {


    public static final String MODID = "mirrorsync";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            FileSyncService.getInstance().sync(FMLPaths.GAMEDIR.get());
        }
    }
}
