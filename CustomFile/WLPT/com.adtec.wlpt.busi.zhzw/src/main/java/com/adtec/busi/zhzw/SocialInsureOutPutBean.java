package com.adtec.busi.zhzw;

import com.adtec.starring.datapool.CompSDO;
import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.datapool.HttpHeaderOper;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.adtec.wlpt.constant.HttpCoreRspCode;
import com.adtec.wlpt.enc.EncUtils;
import com.adtec.wlpt.enc.SM4EncryptUtil;
import com.adtec.wlpt.util.ElementUtil;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static com.adtec.wlpt.util.ConfigUtil.*;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

/**
 * SocialInsureOutPutBean
 * 社保接出点客户化处理
 *
 * @author dww
 * @date 2019/10/25
 */
public class SocialInsureOutPutBean {
    public static final Boolean SUCCESS_CODE = true;
    public static final String FAIL_CODE = "9999";


    /**
     * 拼接Url
     *
     * @return
     */
    public String getUrl() {
        //请求数据对象
        String elementName = DtaInfo.getInstance().getInElemName();
        //获取Url
        String urlStr = getConfigValue(SHEBAO_URL);
        TrcLog.log(PubfuncBean.getLogfile(), urlStr);
        //截取host参数
        String subUrl = urlStr.substring(urlStr.indexOf("//") + 2);
        String host = subUrl.contains("/") ? subUrl.substring(0, subUrl.indexOf("/")) : subUrl;
        //Header参数
        HttpHeaderOper.insert("HOST", host);
        HttpHeaderOper.insert("Content-Type", "application/json");
        //外部服务名
        String svcName = ElementUtil.getString("__GDTA_FORMAT.__GDTA_SVCNAME");
        TrcLog.log(PubfuncBean.getLogfile(), "外部服务名：" + svcName);
        return urlStr + svcName;
    }

    /**
     * 组织交易报文前事件
     *
     * @return
     */
    public void doActionBeaforParseData() throws Exception {
        String tpId = DtaInfo.getInstance().getTpId();
        //获取交易报文
        byte[] sendData = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");

        String key1 = getConfigValue(SHENPI_SM4_KEY);//密钥要求是16字节16进制
        KeyGenerator kg = KeyGenerator.getInstance("SM4", BouncyCastleProvider.PROVIDER_NAME);//实例化一个SM4加密算法的密钥生成器
        kg.init(128, new SecureRandom(key1.getBytes()));//使用提供的密钥初始化此密钥生成器，使其具有确定的密钥大小128字节长
        SecretKey secretKey = kg.generateKey();//生成一个密钥
        byte[] kk = secretKey.getEncoded();//返回基本编码格式的密钥
        String key = EncUtils.encodeHexString(kk);//将字节数组转换为十六进制字符串
        String cipher = SM4EncryptUtil.encryptEcb(key, new String(sendData));//SM4加密
        TrcLog.log(PubfuncBean.getLogfile(), "加密后的数据：" + cipher);//加密后的数据
        TrcLog.log(PubfuncBean.getLogfile(), "校验加密前后的字符串是否为同一数据：" + SM4EncryptUtil.verifyEcb(key, cipher, new String(sendData)));
        TrcLog.log(PubfuncBean.getLogfile(), "解密后的字符串：" + SM4EncryptUtil.decryptEcb(key, cipher));
        String elementName = DtaInfo.getInstance().getInElemName();//请求数据对象
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA[0]", cipher.getBytes());//覆盖原有的报文数据
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA_LENGTH[0]", key.getBytes().length);
    }

    /**
     * 解析协议报文后事件
     * SM4解密
     *
     * @return
     */

    public void doActionAfterParseData() throws NoSuchProviderException, NoSuchAlgorithmException {

        String tpId = DtaInfo.getInstance().getTpId();
        //获取交易报文
        byte[] recvData = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");

        String key1 = getConfigValue(SHENPI_SM4_KEY);
        KeyGenerator kg = KeyGenerator.getInstance("SM4", BouncyCastleProvider.PROVIDER_NAME);
        kg.init(128, new SecureRandom(key1.getBytes()));
        SecretKey secretKey = kg.generateKey();
        byte[] kk = secretKey.getEncoded();
        String key = EncUtils.encodeHexString(kk);//将字节数组转换为十六进制字符串
        String decData = "";
        try {
            decData = SM4EncryptUtil.decryptEcb(key, new String(recvData));
        } catch (Exception e) {
            throw new BaseException(HttpCoreRspCode.COMM_FAIL_CODE, e, "解密失败");//P5处理异常
        }
        TrcLog.log(PubfuncBean.getLogfile(), "解密后的数据：" + decData);//解密后的数据
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA[0]", decData.getBytes());//覆盖原有的报文数据
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA_LENGTH[0]", decData.getBytes().length);
    }

    /**
     * 响应码映射--每一类接口返回不一样
     */
    public void resposeMapper() {
        //系统错误
        String errRet = ElementUtil.getString("__GDTA_FORMAT.__ERR_RET");
        String errMsg = ElementUtil.getString("__GDTA_FORMAT.__ERR_MSG");
        //交易状态,响应码
        String rspDataElementName = DtaInfo.getInstance().getOutElemName();
        Boolean retCode = ElementUtil.getBoolean(rspDataElementName + ".success");
        //String retMsg = ElementUtil.getString(rspDataElementName + ".msg");
        TrcLog.log(PubfuncBean.getLogfile(), "errRet=[%s] errMsg=[%s] retCode=[%s] ",
                errRet, errMsg, retCode);

        //对方返回错误码和错误信息
        if (null == retCode) {
            if (StringTool.isNullOrEmpty(errRet)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", "响应码为空!");
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            } else {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", errRet, errMsg));
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            }
        } else {
            if (SUCCESS_CODE != retCode) {//社保返回失败交易码
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, "社保未返回错误信息！"));
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
            }
        }
    }

    /**
     * 响应码映射--每一类接口返回不一样
     */
    public void resposeMapper2() {
        //系统错误
        String errRet = ElementUtil.getString("__GDTA_FORMAT.__ERR_RET");
        String errMsg = ElementUtil.getString("__GDTA_FORMAT.__ERR_MSG");
        //交易状态,响应码
        String rspDataElementName = DtaInfo.getInstance().getOutElemName();
        Boolean retCode = ElementUtil.getBoolean(rspDataElementName + ".success");
        String retMsg = ElementUtil.getString(rspDataElementName + ".msg");
        TrcLog.log(PubfuncBean.getLogfile(), "errRet=[%s] errMsg=[%s] retCode=[%s] ",
                errRet, errMsg, retCode);

        //对方返回错误码和错误信息
        if (null == retCode) {
            if (StringTool.isNullOrEmpty(errRet)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", "响应码为空!");
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            } else {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", errRet, errMsg));
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            }
        } else {
            if (SUCCESS_CODE != retCode) {//社保返回失败交易码
                if (!StringTool.isNullOrEmpty(retMsg)) {
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, retMsg));
                } else {
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, "社保未返回错误信息！"));
                }
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
            }
        }
    }
}
