package cn.ylarod.mcp.mirrorsync.utils;

import cn.ylarod.mcp.mirrorsync.MirrorSync;
import cn.ylarod.mcp.mirrorsync.bean.FileBean;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;

public class JsonUtils {
    public static ArrayList<FileBean> parseFileBeansJson(String jsonString){
        ArrayList<FileBean> fileBeans = new ArrayList<>();
        JsonParser jsonParser = new JsonParser();
        try{
            JsonArray jsonArray = jsonParser.parse(jsonString).getAsJsonArray();
            Gson gson = new Gson();
            for (JsonElement user : jsonArray) {
                FileBean fileBean = gson.fromJson(user, FileBean.class);
                fileBeans.add(fileBean);
            }
        }catch (Exception e){
            MirrorSync.logger.atError().log("");
        }
        return fileBeans;
    }
}
