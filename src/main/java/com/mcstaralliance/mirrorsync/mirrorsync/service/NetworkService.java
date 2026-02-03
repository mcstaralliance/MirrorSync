package com.mcstaralliance.mirrorsync.mirrorsync.service;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteRegistryEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteUpdateEntry;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class NetworkService implements AutoCloseable {

    private final CloseableHttpClient httpClient = HttpClients.custom()
            .disableCookieManagement()
            .disableAutomaticRetries()
            .disableConnectionState()
            .disableRedirectHandling()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setContentCompressionEnabled(true)
                    .build())
            .setDefaultHeaders(List.of(
                    new BasicHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"),
                    new BasicHeader(HttpHeaders.PRAGMA, "no-cache"),
                    new BasicHeader(HttpHeaders.EXPIRES, "0")
            ))
            .build();

    private final Gson gson = new Gson();

    public boolean shouldCheck() throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://resource.mcstaralliance.com/lastupdate/switch.txt"))) {
            return Boolean.parseBoolean(EntityUtils.toString(response.getEntity()));
        }
    }

    public List<RemoteRegistryEntry> retrieveRemoteRegistryEntries() throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://resource.mcstaralliance.com/lastupdate/manifest.json"))) {
            return gson.fromJson(new BufferedReader(new InputStreamReader(response.getEntity().getContent())), new TypeToken<>() {});
        }
    }

    public void downloadFile(URI remote, Path local, FileSystemService fileSystemService) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(remote))) {
            HttpEntity entity = response.getEntity();
            fileSystemService.saveBytesTo(local, entity.getContent());
        }
    }

    public List<RemoteUpdateEntry> retrieveRemoteUpdateEntries() throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://resource.mcstaralliance.com/lastupdate/dir_manifest.json"))) {

            HttpEntity entity = response.getEntity();
            Header contentEncoding = entity.getContentEncoding();
            Charset encoding = contentEncoding == null ? StandardCharsets.UTF_8 : contentEncoding.getValue() == null ? StandardCharsets.UTF_8 : Charset.forName(contentEncoding.getValue());
            return gson.fromJson(new BufferedReader(new InputStreamReader(entity.getContent(), encoding)), new TypeToken<>() {});
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
