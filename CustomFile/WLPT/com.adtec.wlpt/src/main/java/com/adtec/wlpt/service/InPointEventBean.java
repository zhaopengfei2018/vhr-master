package com.adtec.wlpt.service;

import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.pubmethod.Message;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

    /**
     * InPointEventBean
     * 接入点事件处理类
     *
     * @author wujw
     * @date 2018/10/12
     */
    public class InPointEventBean {

        /**
         * 报文接收前事件
         */
        public void requestTransfer() {
            String tpId = DtaInfo.getInstance().getTpId();
            DtaInfo dtaInfo = DtaInfo.getInstance();
            Message message = (Message) dtaInfo.getMessage();
            try {
                String format = new String((byte[]) message.getBody());//base64编码的json字符串
                TrcLog.log(PubfuncBean.getLogfile(), "request=[%s]", format);
                String newFormat = convRequest(format);
                TrcLog.log(PubfuncBean.getLogfile(), "newFormat=[%s]", newFormat);
                byte[] bytes = newFormat.getBytes();
                message.setBody(bytes);
                dtaInfo.setMessage(message);
            } catch (Exception e) {
                throw new BaseException("报文转换失败！");
            }
        }

        /**
         * 报文转换
         *
         * @param str
         * @return
         * @throws BaseException
         */
        private String convRequest(String str) throws BaseException {
            JSONObject rootObject = JSON.parseObject(str);
            JSONObject bodyObject = rootObject.getJSONObject("ROOT");
            if (bodyObject == null) {
                bodyObject = new JSONObject();
            }
            Object dataObject = bodyObject.get("APP_DATA");
            if (dataObject == null) {
                //donotHasData();
                TrcLog.log(PubfuncBean.getLogfile(), "request=[%s]", bodyObject.toString());
                String format = Base64.getEncoder().encodeToString(bodyObject.toString().getBytes());
                Map<String, String> appData = new HashMap<>();
                appData.put("APP_DATA", format);
                rootObject.put("ROOT", appData);
                return rootObject.toString();
            } else {
                TrcLog.log(PubfuncBean.getLogfile(), "request=[%s]",
                        new String(Base64.getDecoder().decode((String) dataObject)));
                //hasData();
                //return str;
                return new String(Base64.getDecoder().decode((String) dataObject));
            }
        }

        /**
         * 交易发送前事件
         */
        public void responseTransfer() {
            /*if (isHasDataFlag()) {
                return;
            }*/
            String tpId = DtaInfo.getInstance().getTpId();
            byte[] response = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");
            TrcLog.log(PubfuncBean.getLogfile(), "response=[%s]", new String(response));
            try {
                String format = convResponse(new String(response));
                TrcLog.log(PubfuncBean.getLogfile(), "response=[%s]", format);
                EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA", format.getBytes());
                EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA_LENGTH", format.getBytes().length);
            } catch (BaseException e) {
                throw new BaseException("报文转换失败！");
            }
        }

        /**
         * 转换返回报文
         *
         * @param str
         * @return
         * @throws BaseException
         */
        private String convResponse(String str) throws BaseException {
            JSONObject rootObject = JSON.parseObject(str);
            JSONObject bodyObject = rootObject.getJSONObject("ROOT");
            if (bodyObject != null) {
                String appData = bodyObject.getString("APP_DATA");
                if (appData != null) {
                    String format = new String(Base64.getDecoder().decode(appData));
                    JSONObject appObject = JSON.parseObject(format);
                    rootObject.put("ROOT", appObject);
                }
            }
            return rootObject.toString();
        }

        /**
         * 判断是否是DataBus调用
         *
         * @return
         */
        private boolean isHasDataFlag() {
            String hasDataFlag = (String) EPOper.get(DtaInfo.getInstance().getTpId(), "TranEnv[0].HAS_APP_DATA[0]");
            if (!StringTool.isNullOrEmpty(hasDataFlag) && "1".equals(hasDataFlag)) {
                return true;
            }
            return false;
        }

        /**
         * 设置请求方式为DataBus
         */
        private void hasData() {
            EPOper.put(DtaInfo.getInstance().getTpId(), "TranEnv[0].HAS_APP_DATA[0]", "1");
        }

        /**
         * 设置请求方式为正常JSON格式
         */
        private void donotHasData() {
            EPOper.put(DtaInfo.getInstance().getTpId(), "TranEnv[0].HAS_APP_DATA[0]", "0");
        }
}
