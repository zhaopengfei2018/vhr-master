package com.adtec.busi.zhzw;

import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.datapool.HttpHeaderOper;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.adtec.wlpt.constant.HttpCoreRspCode;
import com.adtec.wlpt.resolver.XmlResolver;
import com.adtec.wlpt.util.ElementUtil;
import com.alibaba.fastjson.JSONException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.dom4j.DocumentException;

import static com.adtec.wlpt.util.ConfigUtil.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ShenPiOutPutBean
 * 审批局接出点客户化处理
 *
 * @author dww
 * @date 2019/10/25
 */
public class ShenPiOutPutBean {
    public static final String SUCCESS_CODE = "200";
    public static final String FAIL_CODE = "9999";
    public static final List<String> serviceList = Arrays.asList("getItemOrgList",
            "getItemListByPage",
            "getBusinessListByIdCard",
            "getItemInfoByItemCode", "createReceiveNum");

    /**
     * 拼接Url
     *
     * @return
     */
    public String getUrl() {

        String elementName = DtaInfo.getInstance().getInElemName();//请求数据对象
        TrcLog.log(PubfuncBean.getLogfile(), "elementName:" + elementName);
        Map<String, String> parmsMap = ElementUtil.getItemValueMap(true, elementName);//获取参数
        //String body = PubfuncBean.getParmsStr(parmsMap);
        String urlStr = getConfigValue(SHENPI_URL); //获取Url
        TrcLog.log(PubfuncBean.getLogfile(), urlStr);
        String subUrl = urlStr.substring(urlStr.indexOf("//") + 2);//截取host参数
        String host = subUrl.contains("/") ? subUrl.substring(0, subUrl.indexOf("/")) : subUrl;
        // String token = getConfigValue(SHENPI_TOKEN_KEY);//获取token
        //Header参数
        HttpHeaderOper.insert("HOST", host);
        HttpHeaderOper.insert("Content-Type", "application/json");
        String svcName = ElementUtil.getString("__GDTA_FORMAT.__GDTA_SVCNAME");//外部服务名
        String joinParam = PubfuncBean.getParmsStr(parmsMap);
        if (serviceList.contains(DtaInfo.getInstance().getSvcName())) {
            //ElementUtil.put("__GDTA_FORMAT.__HTTP_TYPE","GET");//GET请求
            //TrcLog.log(PubfuncBean.getLogfile(), "重新组装URL：" + urlStr + svcName + "?" + PubfuncBean.getParmsStr(parmsMap));
            if (StringTool.isNullOrEmpty(joinParam)) {
                return urlStr + svcName;
            }else
            return urlStr + svcName + "?" + joinParam;
        } else {
            return urlStr + svcName;
        }

    }

    /**
     * 部门列表服务 解析交易报文中的json格式
     */
    public void deptListResponse() {
        try {
            String elementName = DtaInfo.getInstance().getOutElemName();//响应数据对象
            byte[] data = (byte[]) EPOper.get(DtaInfo.getInstance().getTpId(), "__GDTA_FORMAT.__ITEMDATA");
            JSONArray jsonArray = JSONArray.fromObject(new String(data));
            //CompSDO compSDO = EPOper.getCompSDO(DtaInfo.getInstance().getTpId(),elementName);//获取响应对象
            TrcLog.log(PubfuncBean.getLogfile(), "OutElemName" + elementName);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = JSONObject.fromObject(jsonArray.get(i).toString());
                Iterator iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    String value = (String) jsonObject.get(key);
                    //ElementUtil.setValueString(compSDO,key,(String)jsonObject.get(key));
                    EPOper.put(DtaInfo.getInstance().getTpId(), elementName + ".deptList[" + i + "]." + key, 0, value);//给平台数据对象赋值
                }
            }

        } catch (JSONException e) {
            throw new BaseException(HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE, e, e.getMessage());
        }

    }


    /**
     * 解析交易报文中的XML格式
     */
    public void parseXMLToJSON() {
        String tpId = DtaInfo.getInstance().getTpId();
        byte[] recvData = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");
        String format = new String(recvData);
        if (StringTool.isNullOrEmpty(format)) {
            throw new BaseException(HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE, "返回数据为空!");
        }
        try {
            //解析xml报文
            XmlResolver xmlResolver = new XmlResolver(format);

            //数据对象赋值
            Map<String, String> xmlAtrrMap = xmlResolver.getMergeItemAtrr("RESULT");
            xmlAtrrMap.forEach((k, v) -> {
                TrcLog.info(PubfuncBean.getLogfile(), "数据对象map=[%s][%s]", k, v);
            });
            String elementName = DtaInfo.getInstance().getOutElemName();
            TrcLog.info(PubfuncBean.getLogfile(), "elementName=[%s]", elementName);
            ElementUtil.setItemValueMap(elementName, xmlAtrrMap);
        } catch (DocumentException e) {
            throw new BaseException(HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE, e, e.getMessage());
        }
    }

    /**
     * 响应码映射--每一类接口返回不一样
     */
    public void resposeMapper() {
        //系统错误
        String errRet = ElementUtil.getString("__GDTA_FORMAT.__ERR_RET");
        String errMsg = ElementUtil.getString("__GDTA_FORMAT.__ERR_MSG");
        String retCode="";
        String retMsg="";
        //对方返回错误码和错误信息
        if (StringTool.isNullOrEmpty(retCode)) {
            if (StringTool.isNullOrEmpty(errRet)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", "响应码为空!");
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            } else {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", errRet, errMsg));
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
            }
        } else {
            if (!SUCCESS_CODE.equals(retCode)) {
                if (!StringTool.isNullOrEmpty(retMsg)) {
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, retMsg));
                } else {
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, "审批局未返回错误信息！"));
                }

                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
            }
        }
    }
    /**
     * 响应码映射--每一类接口返回不一样
     */
    public void resposeMapper1() {
        //系统错误
        String errRet = ElementUtil.getString("__GDTA_FORMAT.__ERR_RET");
        String errMsg = ElementUtil.getString("__GDTA_FORMAT.__ERR_MSG");
        //交易状态,响应码
        String rspDataElementName = DtaInfo.getInstance().getOutElemName();
        String retCode = ElementUtil.getString(rspDataElementName + ".state");
        String retMsg=ElementUtil.getString(rspDataElementName + ".error");
        TrcLog.info(PubfuncBean.getLogfile(), "retCode=[%s] retMsg=[%s]", retCode,retMsg);
        //对方返回错误码和错误信息
        if (StringTool.isNullOrEmpty(retCode)) {
            if (StringTool.isNullOrEmpty(errRet)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", "响应码为空!");
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            } else {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", errRet, errMsg));
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
            }
        } else {
            if (!SUCCESS_CODE.equals(retCode)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, retMsg));
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
        String retCode = ElementUtil.getString(rspDataElementName + ".state");
        String retMsg=ElementUtil.getString(rspDataElementName + ".error");
        //TrcLog.info(PubfuncBean.getLogfile(), "retCode=[%s] retMsg=[%s]", retCode,retMsg);
        //对方返回错误码和错误信息
        if (StringTool.isNullOrEmpty(retCode)) {
            if (StringTool.isNullOrEmpty(errRet)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", "响应码为空!");
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_NULL_CODE);
            } else {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", errRet, errMsg));
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
            }
        } else {
            if (!"1".equals(retCode)) {
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE", HttpCoreRspCode.QINGDAO_SHENPI_FAIL_CODE);
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG", String.format("[%s][%s]", retCode, retMsg));
            }
        }
    }
}
