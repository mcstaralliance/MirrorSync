package cn.ylarod.mcp.mirrorsync.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;


public class HttpUtils {
    private static String result = null;
    private static InputStream is = null;

    /**
     * 阻塞post
     */
    public static String syncPost(String url, String param) {
        final CountDownLatch lt = new CountDownLatch(1);
        OkHttpClient okHttp = new OkHttpClient().newBuilder().proxy(Proxy.NO_PROXY).build();
        FormBody.Builder form = new FormBody.Builder();
        if (!param.equals("")) {
            String[] bodyArr = param.split("&");
            for (String item : bodyArr) {
                String[] itemArr = item.split("=");
                if (itemArr.length == 1) {
                    form.add(itemArr[0], "");
                } else {
                    form.add(itemArr[0], itemArr[1]);
                }
            }
        }
        Request request = new Request.Builder()
                .url(url)
                .post(form.build())
                .build();
        okHttp.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    result = Objects.requireNonNull(response.body()).string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lt.countDown();
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }
        });
        try {
            lt.await();
        } catch (InterruptedException ignored) {
        }
        return result;
    }

    /**
     * 阻塞get
     */
    public static InputStream syncGet(String url) {
        final CountDownLatch lt = new CountDownLatch(1);
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().get()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                is = Objects.requireNonNull(response.body()).byteStream();
                lt.countDown();
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }
        });
        try {
            lt.await();
        } catch (InterruptedException ignored) {
        }
        return is;
    }

    public static String syncGetString(String url) {
        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().get()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.body() == null){
                return "";
            }
            return Objects.requireNonNull(response.body()).string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
