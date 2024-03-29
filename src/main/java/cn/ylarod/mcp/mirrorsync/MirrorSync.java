package cn.ylarod.mcp.mirrorsync;

import cn.ylarod.mcp.mirrorsync.bean.FileBean;
import cn.ylarod.mcp.mirrorsync.utils.FileUtils;
import cn.ylarod.mcp.mirrorsync.utils.HttpUtils;
import cn.ylarod.mcp.mirrorsync.utils.JsonUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

@Mod(
        modid = MirrorSync.MOD_ID,
        name = MirrorSync.MOD_NAME,
        version = MirrorSync.VERSION,
        dependencies = "before:crafttweaker"
)
public class MirrorSync {

    public static final String MOD_ID = "mirrorsync";
    public static final String MOD_NAME = "MirrorSync";
    public static final String VERSION = "1.0";

    public static final Logger logger = LogManager.getLogger(MOD_ID);

    /**
     * Powered by StarAlliance
     */
    @Mod.Instance(MOD_ID)
    public static MirrorSync INSTANCE;


    public void cleanLocalFiles(ArrayList<FileBean> fileBeans, ArrayList<File> localFiles){
        for(File file : localFiles){
            boolean found = false;
            for (FileBean fileBean : fileBeans){
                if (file.getName().equalsIgnoreCase(fileBean.filename)){
                    found = true;
                    break;
                }
            }
            if(!found){
                file.delete();
            }
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        String basedir = new File("./").getAbsolutePath();
        Boolean hasMinecraft = false;
        if (basedir.contains(".minecraft")){
            hasMinecraft = true;
        }
        String autoUpdateControl = HttpUtils.syncGetString("https://resource.mcstaralliance.com/lastupdate/switch.txt");
        if(autoUpdateControl.equalsIgnoreCase("true")){
            logger.atInfo().log("Basedir:" + basedir);
            String url = "https://resource.mcstaralliance.com/lastupdate/manifest.json";
            String json = HttpUtils.syncGetString(url);
            ArrayList<FileBean> fileBeans = JsonUtils.parseFileBeansJson(json);
            cleanLocalFiles(fileBeans, FileUtils.travel(new File(hasMinecraft ? "scripts" : ".minecraft/scripts")));
            cleanLocalFiles(fileBeans, FileUtils.travel(new File(hasMinecraft ? "resources" : ".minecraft/resources")));
            for(FileBean fileBean : fileBeans){
                String save_path = hasMinecraft ? fileBean.savePath.replaceAll(".minecraft/", "") : fileBean.savePath;
                String file_hash = FileUtils.getFileMD5(save_path);
                if (fileBean.hash.equalsIgnoreCase("DELETE")) {
                    File file = new File(save_path);
                    if (file.exists()){
                        logger.atInfo().log("Delete: " + fileBean.filename);
                        file.delete();
                    }
                }else if(file_hash.equalsIgnoreCase("")){
                    logger.atError().log("Cannot check file: " + fileBean.filename);
                    File path = new File(save_path).getParentFile();
                    if(!path.exists()){
                        path.mkdirs();
                    }
                    InputStream is = HttpUtils.syncGet(fileBean.downloadUrl);
                    FileUtils.writeFile(is, save_path);
                    logger.atInfo().log("Sync: " + fileBean.filename + " from " + fileBean.downloadUrl  + " to " + save_path);
                }else if(!file_hash.equalsIgnoreCase(fileBean.hash)){
                    logger.atInfo().log("Check: " + fileBean.filename + " has different hash " + file_hash + " with server " + fileBean.hash);
                    InputStream is = HttpUtils.syncGet(fileBean.downloadUrl);
                    FileUtils.writeFile(is, save_path);
                    logger.atInfo().log("Sync: " + fileBean.filename + " from " + fileBean.downloadUrl  + " to " + save_path);
                }
            }
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}
