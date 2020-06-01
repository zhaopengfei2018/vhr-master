package com.adtec.wlpt.util;

import com.adtec.starring.respool.ResPool;

/**
 * ConfigUtil
 * 键值配置在 $HOME/etc/config.zh_cn.utf8.properties
 *
 * @author wujw
 * @date 2019/10/2
 */
public class ConfigUtil {

    //青岛审批局政务服务
    public static final String SHENPI_URL = "shenpi.url";
    public static final String SHENPI_TOKEN_KEY = "shenpi.token";
    public static final String SHENPI_SM4_KEY = "shebao.key";
    public static final String SHEBAO_URL = "shebao.url";
    public static final String IMMOVABLEPROPERTY_URL = "property.url";//不动产请求地址
    //对象存储服务
    public static final String IMG_SERVICE_URL_KEY = "img.service.upload.url";
    public static final String IMG_SERVICE_NET_URL_KEY = "img.service.download.net.url";
    public static final String IMG_SERVICE_BUCKET_ID_KEY = "img.service.bucketId";
    public static final String IMG_SERVICE_APP_ID_KEY = "img.service.appId";

    /**
     * 获取参数,配置在 $HOME/etc/config.zh_cn.utf8.properties
     *
     * @param key
     * @return
     */
    public static String getConfigValue(String key) {
        return ResPool.configMap.get(key);
    }

}
