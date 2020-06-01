package com.adtec.wlpt.file;

import com.adtec.starring.log.TrcLog;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.adtec.wlpt.http.HttpClientUtil;
import com.adtec.wlpt.http.HttpResult;
import com.adtec.wlpt.util.ConfigUtil;

import org.apache.http.Header;

import static com.adtec.wlpt.util.ConfigUtil.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * ObjectTransferService
 * 对象存储服务
 *
 * @author wujw
 * @date 2019/10/5
 */
public class ObjectTransferService {

    /**
     * 对象存储上传
     *
     * @param appId    请求方ID
     * @param filePath 目标文件
     * @param file     本地文件对象
     * @return url地址
     * @throws Exception
     */
    public static String upload(String appId, String filePath, File file) throws Exception {
        //获取URl
        String uploadUrl = ConfigUtil.getConfigValue(IMG_SERVICE_URL_KEY);
        String downloadUrl = ConfigUtil.getConfigValue(IMG_SERVICE_NET_URL_KEY);
        String bucketId = ConfigUtil.getConfigValue(IMG_SERVICE_BUCKET_ID_KEY);
        if (StringTool.isNullOrEmpty(appId)) {
            appId = ConfigUtil.getConfigValue(IMG_SERVICE_APP_ID_KEY);
        }
        String requestUrl = String.format("%s?ObjNm=%s&bucketId=%s&C-App-Id=%s", uploadUrl, filePath, bucketId, appId);
        String responseUrl = String.format("%s?ObjNm=%s&bucketId=%s&C-App-Id=%s", downloadUrl, filePath, bucketId, appId);

        TrcLog.log(PubfuncBean.getLogfile(), "开始文件上传" + file.getPath() + "==>" + filePath);
        TrcLog.log(PubfuncBean.getLogfile(), "urlStr=[%s]", requestUrl);
        //发送POST请求
        HttpResult httpResult = HttpClientUtil.sendPostForm(requestUrl, file, null, null, null, "UTF-8");
        // 处理响应结果
        handleHttpResult(httpResult, null);
        //返回url路径
        return responseUrl;
    }

    /**
     * 获取文件
     *
     * @param url           对象存储Url
     * @param localFilePath 本地文件路径
     * @return
     */
    public static void download(String url, String localFilePath) throws IOException {
        TrcLog.log(PubfuncBean.getLogfile(), "开始下载文件");
        TrcLog.log(PubfuncBean.getLogfile(), "urlStr=[%s]", url);
        // TODO: 2019/10/8 未测试
        //发送POST请求
        HttpResult httpResult = HttpClientUtil.sendGet(url, null, null, null, null);
        // 处理响应结果
        handleHttpResult(httpResult, localFilePath);
    }

    /**
     * 返回对象处理，上传图片不处理，下载图片写入文件
     * @param httpResult
     * @param filePath
     * @throws IOException
     */
    private static void handleHttpResult(HttpResult httpResult, String filePath) throws IOException {
        TrcLog.log(PubfuncBean.getLogfile(), "响应状态码:" + httpResult.getStatusCode());
        TrcLog.log(PubfuncBean.getLogfile(), "响应结果类型:" + httpResult.getContentType());
        TrcLog.log(PubfuncBean.getLogfile(), "响应结果是否是文本:" + httpResult.isTextType());
        if (httpResult.getHeaders() != null) {
            TrcLog.log(PubfuncBean.getLogfile(), "\n响应的Header如下:");
            for (Header header : httpResult.getHeaders()) {
                TrcLog.log(PubfuncBean.getLogfile(), header.getName() + " : " + header.getValue());
            }
        }
        TrcLog.log(PubfuncBean.getLogfile(), "响应结果是否是文本:" + httpResult.isTextType());
        if (httpResult.isTextType()) {
            TrcLog.log(PubfuncBean.getLogfile(), "响应的文本结果如下:");
            TrcLog.log(PubfuncBean.getLogfile(), httpResult.getStringContent());
        } else {
            if (filePath != null && filePath.length() > 0) {
                File file = new File(filePath);
                File parent = file.getParentFile();
                if (!parent.exists() && parent.mkdirs()) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        fileOutputStream.write(httpResult.getByteArrayContent());
                    }
                }
            }
        }
    }

}
