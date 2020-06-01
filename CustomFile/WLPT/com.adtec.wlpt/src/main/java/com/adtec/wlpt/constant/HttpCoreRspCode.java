package com.adtec.wlpt.constant;

/**
 * P8RspCode
 *
 * @author wujw
 * @date 2019/10/3
 */
public class HttpCoreRspCode {
    public static final String SUCCESS_CODE = "00000";//交易成功
    public static final String COMM_FAIL_CODE = "50000"; //P5处理异常[%s]
    public static final String QINGDAO_SHENPI_FAIL_CODE = "50001"; //青岛审批局政务处理异常[%s]
    public static final String QINGDAO_NULL_CODE = "50002"; //未返回交易代码
    public static final String QINGDAO_SHEBAO_FAIL_CODE = "50003"; //青岛社保处理异常[%s]
    public static final String QINGDAO_IMVALPROPERTY_FAIL_CODE = "50004"; //青岛不动产处理异常[%s]

}
