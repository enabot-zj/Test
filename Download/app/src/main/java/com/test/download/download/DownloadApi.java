package com.test.download.download;

import com.test.download.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;

public class DownloadApi {

    public static String DOWNLOAD_DIR = "/sdcard/DownloadFile/";
    public static int BUFFER_SIZE = 1024;

    private static final int DEFAULT_TIMEOUT = 5;
    private Retrofit retrofit;
    private FileService fileService;
    private volatile static DownloadApi instance;
    private Call<ResponseBody> call;
    private String url;

    private static Hashtable<String, DownloadApi> mFileApiTable;

    static {
        mFileApiTable = new Hashtable<>();
    }

    /**
     * 单例模式-私有构造函数
     *
     * @param baseUrl
     */
    private DownloadApi(String baseUrl) {
        this.url = baseUrl;
        int index = baseUrl.lastIndexOf("/");
        if (index >= 0) {
            baseUrl = baseUrl.substring(0, index) + "/";
        }
        retrofit = new Retrofit.Builder()
                .client(initOkHttpClient())
                .baseUrl(baseUrl)
                .build();
        fileService = retrofit.create(FileService.class);
    }

    /**
     * 获取实例
     *
     * @param baseUrl
     * @return
     */
    public static DownloadApi getInstance(String baseUrl) {
        instance = mFileApiTable.get(baseUrl);
        if (instance == null) {
            synchronized (DownloadApi.class) {
                if (instance == null) {
                    instance = new DownloadApi(baseUrl);
                    mFileApiTable.put(baseUrl, instance);
                }
            }
        }
        return instance;
    }

    /**
     * 下载文件
     *
     * @param callback
     */
    public void loadFileByName(DownloadCallback callback) {
       loadFileByName(DOWNLOAD_DIR, callback);
    }

    /**
     * 下载文件
     *
     * @param downloadDir
     * @param callback
     */
    public void loadFileByName(String downloadDir, DownloadCallback callback) {
        HashMap<String, DownloadInfo> downloadInfoMap = DownloadInfoUtil.getAllDownloadInfo();
        DownloadInfo downloadInfo = null;
        if(downloadInfoMap != null){
            downloadInfo = downloadInfoMap.get(url);
        }
        if (downloadInfo != null) {
            LogUtil.i("开启续传下载： " + downloadInfo.getDownloadSize() + ", " + downloadInfo.getTotalSize() + ", "
                    + downloadInfo.getUrl() + ", " + downloadInfo.getName());
            String header = "bytes=" + downloadInfo.getDownloadSize() + "-";
            call = fileService.loadFile(url, header);
            call.enqueue(callback);
        } else {
            LogUtil.i("开启下载： " + url + ", ");
            call = fileService.loadFile(url);
            call.enqueue(callback);
        }
    }

    /**
     * 取消下载
     */
    public void cancel() {
        if (call != null && call.isCanceled() == false) {
            call.cancel();
        }
    }

    /**
     * 初始化OkHttpClient
     *
     * @return
     */
    private OkHttpClient initOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.networkInterceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse
                        .newBuilder()
                        .body(new FileResponseBody(originalResponse))
                        .build();
            }
        });
        return builder.build();
    }

}
