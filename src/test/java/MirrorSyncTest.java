import cn.ylarod.mcp.mirrorsync.bean.FileBean;
import cn.ylarod.mcp.mirrorsync.utils.HttpUtils;
import cn.ylarod.mcp.mirrorsync.utils.JsonUtils;
import org.junit.Test;

import java.util.ArrayList;


public class MirrorSyncTest {
    @Test
    public void parseManifest(){
        String url = "https://ftp.mcstaralliance.com/lastupdate/manifest.json";
        String json = HttpUtils.syncGetString(url);
        ArrayList<FileBean> fileBeans = JsonUtils.parseFileBeansJson(json);
        for(FileBean fileBean : fileBeans){
            System.out.println(fileBean);
        }
    }

    @Test
    public void testNullJson(){
        JsonUtils.parseFileBeansJson("[]");
    }
}
