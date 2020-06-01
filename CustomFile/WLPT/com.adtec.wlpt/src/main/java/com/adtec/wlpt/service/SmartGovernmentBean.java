package com.adtec.wlpt.service;

import com.adtec.starring.datapool.CompSDO;
import com.adtec.starring.datapool.HttpHeaderOper;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.adtec.wlpt.constant.HttpCoreRspCode;
import com.adtec.wlpt.file.ObjectTransferService;
import com.adtec.wlpt.util.ElementUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * SmartGovernmentBean
 * 智慧政务
 *
 * @author wujw
 * @date 2019/10/2
 */
public class SmartGovernmentBean {

    /**
     * 服务处理成功后事件
     */
    public void successAfter() {
        //接入点响应体信息
        //setRespBody();
        //接入点响应头信息
        setRespCode(HttpCoreRspCode.SUCCESS_CODE, "交易成功");
        //清空返回Transfer-Encoding
        HttpHeaderOper.delete(DtaInfo.getInstance().getTpId(), "Transfer-Encoding");
    }

    /**
     * 服务处理失败后事件
     */
    public void failAfter() {
        //默认返回失败
        setRespCode(null, null);
        //清空返回Transfer-Encoding
        HttpHeaderOper.delete(DtaInfo.getInstance().getTpId(), "Transfer-Encoding");
    }

    /**
     * 修改交易返回结果，如果交易结果已存在，则不做处理
     *
     * @param code
     * @param msg
     */
    public static void setRespCode(String code, String msg) {
        //系统错误
        String errRet = ElementUtil.getString("__PLAT_FLOW.__ERR_CODE");
        String errMsg = ElementUtil.getString("__PLAT_FLOW.__ERR_MSG");
        //交易状态,响应码
        String retCode = ElementUtil.getString("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]");
        String retMsg = ElementUtil.getString("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]");
        TrcLog.log(PubfuncBean.getLogfile(), "errRet=[%s] errMsg=[%s] retCode=[%s] retMsg=[%s] code=[%s] msg=[%s]",
                errRet, errMsg, retCode, retMsg,code,msg);

        //如果交易响应码为空，检查系统错误，如果都不存在默认交易失败
        if (StringTool.isNullOrEmpty(retCode)) {
            if (StringTool.isNullOrEmpty(code)) {
                if ("0".equals(errRet)) {
                    retCode = HttpCoreRspCode.SUCCESS_CODE;
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]", "交易成功!");
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]", HttpCoreRspCode.SUCCESS_CODE);
                } else {
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]", errMsg);
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]", HttpCoreRspCode.COMM_FAIL_CODE);
                }
            } else {
                //不覆盖已有的响应码
                retCode = code;
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]", msg);
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]", code);
            }
        }else if(HttpCoreRspCode.QINGDAO_NULL_CODE.equals(retCode)){//响应码为空
            if (StringTool.isNullOrEmpty(code)) {
                if ("0".equals(errRet)) {
                    retCode = HttpCoreRspCode.SUCCESS_CODE;
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]", "交易成功!");
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]", HttpCoreRspCode.SUCCESS_CODE);
                } else {
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]", errMsg);
                    ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]", HttpCoreRspCode.COMM_FAIL_CODE);
                }
            } else {
                //不覆盖已有的响应码
            	TrcLog.log(PubfuncBean.getLogfile(), "errRet=[%s] errMsg=[%s] retCode=[%s] retMsg=[%s]",
                        errRet, errMsg, retCode, retMsg);
                retCode = code;
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_MSG[0]", msg+","+retMsg);
                ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_RET[0].RET_CODE[0]", code);
            }
        }

        //交易状态
        if (HttpCoreRspCode.SUCCESS_CODE.equals(retCode)) {
            ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_STAT[0]", "SUCCESS");
        } else {
            ElementUtil.put("STARRING_RES[0].SYS_HEAD[0].TRAN_STAT[0]", "FAIL");
        }
    }

    /**
     * 交易体转换成String
     */
    private void setRespBody() {
        String srcElementRsp = DtaInfo.getInstance().getOutElemName();
        String responseBody = ElementUtil.getCompSDO(srcElementRsp).toJSON();
        int beginIndex = responseBody.indexOf(":{");
        int endIndex = responseBody.lastIndexOf("}");
        ElementUtil.put("STARRING_RES[0].ResponseBody[0]", responseBody.substring(beginIndex + 1, endIndex));
    }

}
