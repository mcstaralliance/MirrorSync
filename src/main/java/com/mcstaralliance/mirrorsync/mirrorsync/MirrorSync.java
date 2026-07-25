package com.mcstaralliance.mirrorsync.mirrorsync;

import com.mcstaralliance.mirrorsync.mirrorsync.service.FileSyncService;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod(MirrorSync.MODID)
public class MirrorSync {

    public MirrorSync(IEventBus modEventBus) {
        modEventBus.addListener(MirrorSync::onConstruct);
    }

    public static final String MODID = "mirrorsync";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void onConstruct(FMLConstructModEvent event) {
        event.enqueueWork(() -> FileSyncService.getInstance().sync(FMLPaths.GAMEDIR.get()));
    }
}
