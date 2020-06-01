package com.adtec.wlpt;

import com.adtec.starring.datapool.HttpHeaderOper;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.global.SysDef;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.pubmethod.Message;
import com.adtec.starring.respool.PoolOperate;
import com.adtec.starring.respool.SystemParameter;
import com.adtec.starring.struct.ala.ALA;
import com.adtec.starring.struct.datamap.DataMapInfo;
import com.adtec.starring.struct.datamap.DataMapItem;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.struct.service.Service;
import com.adtec.starring.struct.svcdepend.SvcDepend;
import com.adtec.starring.tool.DTATool;
import com.adtec.starring.util.SpringUtil;
import com.adtec.wlpt.util.ElementUtil;

import sun.misc.BASE64Encoder;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 公共静态方法类
 */
public class PubfuncBean {
    public static final String FAIL_CODE = "9999";
    public static final String RECV_FILE_PATH =
            SysDef.WORK_DIR + SysDef.FILE_SP + "print" + SysDef.FILE_SP + "recvFile" + SysDef.FILE_SP;
    public static final String SEND_FILE_PATH =
            SysDef.WORK_DIR + SysDef.FILE_SP + "print" + SysDef.FILE_SP + "sendFile" + SysDef.FILE_SP;

    /**
     * 调用接出点服务
     *
     * @param destDtaName
     * @param destSvcName
     */
    public static void callOutPut(String destDtaName, String destSvcName) {
        //接出点服务依赖
        setOutputSvcDepend(destDtaName, destSvcName);
        // 调用接出点服务
        DTATool dtaTool = (DTATool) SpringUtil.getBean("dtaTool");
        dtaTool.callOutPut(destDtaName, destSvcName);
    }

    /**
     * 接出点服务依赖
     *
     * @param destDtaName 目的方Dta名
     * @param destSvcName 目的方服务名
     */
    public static void setOutputSvcDepend(String destDtaName, String destSvcName) {
        setSvcDepend(destDtaName, destSvcName, "O");
    }

    /**
     * 子业务服务依赖
     *
     * @param destDtaName
     * @param destSvcName
     */
    public static void setSubSvcDepend(String destDtaName, String destSvcName) {
        setSvcDepend(destDtaName, destSvcName, "C");
    }

    /**
     * 设置服务依赖
     *
     * @param destDtaName
     * @param destSvcName
     * @param destType    目的方类型 O:接出点 C：子业务
     */
    public static void setSvcDepend(String destDtaName, String destSvcName, String destType) {
        DtaInfo dtaInfo = DtaInfo.getInstance();
        String dtaNm = dtaInfo.getDtaName();
        String svcNm = dtaInfo.getSvcName();

        SvcDepend svcDepend = new SvcDepend();
        svcDepend.setSrcType("C");
        svcDepend.setSrcName(dtaNm);
        svcDepend.setSrcSvcName(svcNm);
        svcDepend.setDestType(destType);
        svcDepend.setDestName(destDtaName);
        svcDepend.setSvcName(destSvcName);

        ConcurrentHashMap<String, SvcDepend> svcDependMap = PoolOperate.getResPoolByVersion(dtaInfo.getDepVersion())
                .getAlaHashMap().get(dtaNm).getSvcDependMap();
        String key = dtaNm + svcNm + destDtaName + destSvcName;
        svcDependMap.putIfAbsent(key, svcDepend);
    }

    /**
     * 获取http+json通讯协议中的服务码，直接截取请求路径的/最后一个要素作为服务码
     *
     * @return 服务码
     */
    public static String getServiceCode() {
        String svcCode = "";
        DtaInfo dtaInfo = DtaInfo.getInstance();
        Message message = (Message) dtaInfo.getMessage();
        String url = message.gethead().getUrl();
        int index = url.lastIndexOf("/");
        int index2 = url.indexOf("?");
        if (index2 > 0) {
            svcCode = url.substring(index + 1, index2);
        } else {
            svcCode = url.substring(index + 1);
        }

        return svcCode;
    }

    /**
     * 获取日志文件名
     */
    public static String getLogfile() {
        // return "info.log." + Thread.currentThread().getId();
        return String.format("trans_%s.log", DtaInfo.getInstance().getSvcName());
    }

    /**
     * 获取服务输入对象
     *
     * @return
     */
    public static String getServiceReqElementName(String dtaName, String serviceName) {
        Service service = (Service) PoolOperate.getResData(dtaName, PoolOperate.SERVICE, serviceName);
        return service.getiElem();
    }

    /**
     * 获取服务输出对象
     *
     * @return
     */
    public static String getServiceResElementName(String dtaName, String serviceName) {
        Service service = (Service) PoolOperate.getResData(dtaName, PoolOperate.SERVICE, serviceName);
        return service.getoElem();
    }

    /**
     * 清理httpContetType
     */
    public static void cleanHttpContetType() {
        HttpHeaderOper.delete();
    }

    /**
     * 请求循环域的数据映射
     * 根据服务依赖和数据映射,将循环域拷贝到目的对象中,添加外部调用前事件
     *
     * @param srcItemName
     */
    public static void reqArrayMapper(String srcItemName) {
        arrayMapper(true, srcItemName);
    }

    /**
     * 响应循环域的数据映射
     * 根据服务依赖和数据映射,将循环域拷贝到目的对象中，添加外部调用成功后事件
     *
     * @param srcItemName
     */
    public static void rspArrayMapper(String srcItemName) {
        arrayMapper(false, srcItemName);
    }

    /**
     * 循环域的数据映射
     * 根据服务依赖和数据映射,将循环域拷贝到目的对象中
     *
     * @param isRequest
     * @param srcItemName
     */
    private static void arrayMapper(boolean isRequest, String srcItemName) {
        TrcLog.log(PubfuncBean.getLogfile(), " srcItemName=[%s] isRequest=[%s]", srcItemName, String.valueOf(isRequest));
        //源Dta和服务名
        String srcDtaName = DtaInfo.getInstance().getDtaName();
        String srcSvcName = DtaInfo.getInstance().getSvcName();
        TrcLog.log(PubfuncBean.getLogfile(), "srcDtaName=[%s] srcSvcName=[%s]", srcDtaName, srcSvcName);
        //请求源数据对象
        String srcElementReq = DtaInfo.getInstance().getInElemName();
        String srcElementRsp = DtaInfo.getInstance().getOutElemName();
        //获取服务依赖
        ALA ala = (ALA) PoolOperate.getResData(PoolOperate.ALA, srcDtaName);
        Map<String, SvcDepend> svcDependMap = ala.getSvcDependMap();
        List<String> svcDependKeyList = svcDependMap.keySet().stream()
                .filter(str -> str.startsWith(srcDtaName + srcSvcName))
                .collect(Collectors.toList());
        if (svcDependKeyList.size() == 0) {
            TrcLog.log(PubfuncBean.getLogfile(), "srcDtaName[%s] srcSvcName[%s]未定义服务依赖!", srcDtaName, srcSvcName);
            return;
        }
        //目的Dta和服务名
        String destDtaName = svcDependMap.get(svcDependKeyList.get(0)).getDestName();
        String destSvcName = svcDependMap.get(svcDependKeyList.get(0)).getSvcName();
        TrcLog.log(PubfuncBean.getLogfile(), "destDtaName=[%s] destSvcName=[%s]", destDtaName, destSvcName);
        //目的数据对象
        String desElementReq = PubfuncBean.getServiceReqElementName(destDtaName, destSvcName);
        String desElementRsp = PubfuncBean.getServiceResElementName(destDtaName, destSvcName);
        //组件类型
        Integer sType = SystemParameter.DATAMAP_SRCTYPE.get("ALA");
        Integer dType = SystemParameter.DATAMAP_SRCTYPE.get("DTA");
        //获取数据映射Map
        String key = String.format("%s|%s|%s|%s|%s|%s", srcDtaName, sType, srcSvcName, destDtaName, dType, destSvcName);
        DataMapInfo dataMapInfo = ala.getDataMapTable().get(key);
        if (dataMapInfo == null) {
            TrcLog.log(PubfuncBean.getLogfile(), "srcSvcName[%s] destSvcName[%s]未定义数据映射!", srcSvcName, destSvcName);
            return;
        }
        //获取映射数据项列表
        String descItemName = null;
        Map<String, String> itemMapper = new HashMap<>();
        for (DataMapItem dataMapItem : dataMapInfo.getItemList()) {
            //映射子项
            String srcSdo = dataMapItem.getSrcSdo();
            String destSdo = dataMapItem.getDestSdo();
            if (srcSdo.contains(".")) {
                String compareStr = srcSdo.substring(0, srcSdo.lastIndexOf("."));
                //匹配循环映射对象
                if (compareStr.equals(srcItemName)) {
                    itemMapper.put(srcSdo.substring(srcSdo.lastIndexOf(".") + 1), destSdo.substring(destSdo.lastIndexOf(".") + 1));
                    //目的数据对象
                    if (descItemName == null) {
                        descItemName = destSdo.substring(0, destSdo.lastIndexOf("."));
                    }
                }
            }
        }
        //数据映射
        if (isRequest) {
            srcItemName = srcElementReq + "." + srcItemName;
            descItemName = desElementReq + "." + descItemName;
        } else {
            srcItemName = desElementRsp + "." + srcItemName;
            descItemName = srcElementRsp + "." + descItemName;
        }
        ElementUtil.copyArrayValue(srcItemName, descItemName, itemMapper);
    }
    /**
     * 获取body参数串
     *
     * @param parmsMap
     * @return
     */
    public static String getParmsStr(Map<String, String> parmsMap) {
        //参数Urlcode编码
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : parmsMap.entrySet()) {
            try {
                stringBuilder.append("&").append(entry.getKey()).append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new BaseException(FAIL_CODE, e,
                        String.format("参数[%s][%s]转码失败!", entry.getKey(), entry.getValue()));
            }
        }
        String parmsStr = stringBuilder.toString();
        return parmsStr.length() > 0 ? parmsStr.substring(1) : "";
    }
    /**
     * 报文接收后事件
     * return 报文解析成交易数据对象
     */
    /*public static  void parseResponse(String decStr) {
        String elementName = DtaInfo.getInstance().getOutElemName();
        TrcLog.info(PubfuncBean.getLogfile(), "elementName=[%s]", elementName);
        TrcLog.info(PubfuncBean.getLogfile(), "SM4DecStr=[%s]", decStr);
        //获取解密的字符串
        //JSONObject jsonObject = JSONObject.parseObject(encStr);

        try {
            //解析JSON串
            JsonResolver jsonResolver = new JsonResolver(decStr);
            //写入数据元素池
            jsonResolver.writToElementPool(elementName);
            //响应码转换
           // resposeMapper("DHZX_P5370000YH0001_RSP");
        } catch (JSONException e) {
            throw new BaseException(HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE, e, e.getMessage());
        }
    }*/
    /**
     * 将文件转换成Base64字符串
     *
     * @param file 文件路径
     * @return
     */
    public static String fileToStr(String file){
        InputStream inputStream = null;
        byte [] data = null;
        try {
            inputStream = new FileInputStream(file);
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }
}