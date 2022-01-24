package cn.ylarod.mcp.mirrorsync.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Stack;

public class FileUtils {
    public static String getFileMD5(String filepath){
        try {
            return DigestUtils.md5Hex(new FileInputStream(filepath));
        } catch (IOException ignore) {

        }
        return "";
    }

    public static void writeFile(InputStream in, String des) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(in);
            bos = new BufferedOutputStream(new FileOutputStream(des));
            byte[] bys = new byte[1024];
            int len;
            while ((len = bis.read(bys)) != -1) { //读入bys.len长度的数据放入bys中，并返回字节长度
                bos.write(bys, 0, len); //将字节数组bys[0~len]写入
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null)
                    bis.close();
                if (bos != null)
                    bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<File> travel(File file) {
        ArrayList<File> fileList = new ArrayList<>();
        if (!file.exists()) {
            return fileList;
        }
        Stack<File> stack = new Stack<>();
        stack.add(file);
        while (!stack.isEmpty()) {
            File f = stack.pop();
            File[] files = f.listFiles();
            for (File ffff : files){
                if (ffff.isDirectory()) {
                    stack.add(ffff);
                } else {
                    fileList.add(ffff);
                }
            }
        }
        return fileList;
    }
}
