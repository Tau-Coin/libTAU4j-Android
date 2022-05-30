package io.taucoin.torrent.publishing.core.utils;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Http网络请求相关
 */
public class HttpUtil {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    /**
     * 发送http post请求
     * @param url 网络请求url
     * @param map body参数
     * @return Response
     * @throws IOException
     */
    public static Response httpPost(String url, Map<String, Integer> map) throws IOException {
        RequestBody requestBody = createRequestBody(map);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client
                .newCall(request)
                .execute();
        if (response.isSuccessful()) {
            return response;
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    /**
     * 发送http post请求
     * @param url 网络请求url
     * @param file 上传的文件
     * @return Response
     * @throws IOException
     */
    public static Response httpPostFile(String url, File file) throws Exception {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("multipart/form-data");
        MultipartBody.Builder builder = new MultipartBody.Builder();    // 首先拿到创建者
        builder.setType(mediaType);       // 设置请求体类型
        builder.addFormDataPart("file", file.getName(), RequestBody.create(mediaType, file));
        RequestBody requestBody = builder.build(); //由创建者创建RequestBody

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Response response = client
                .newCall(request)
                .execute();
        if (response.isSuccessful()) {
            return response;
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    private static RequestBody createRequestBody(Map<String, Integer> map) {
        String jsonStr = new Gson().toJson(map);
        return RequestBody.create(JSON, jsonStr);
    }
}
