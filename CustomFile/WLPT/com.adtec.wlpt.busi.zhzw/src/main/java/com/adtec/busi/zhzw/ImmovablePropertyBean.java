package com.adtec.busi.zhzw;

import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.datapool.HttpHeaderOper;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.adtec.wlpt.util.ElementUtil;

import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import org.springframework.util.Base64Utils;

import static com.adtec.wlpt.util.ConfigUtil.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ImmovablePropertyBean
 * 不动产接出点客户化处理
 *
 * @author dww
 * @date 2019年11月19日15:07:53
 */
public class ImmovablePropertyBean {

    /**
     * 拼接Url
     *
     * @author dww
     * @date 2019年11月21日11:09:09
     */
    public String getUrl() {
        String elementName = DtaInfo.getInstance().getInElemName();//请求数据对象
        String urlStr = getConfigValue(IMMOVABLEPROPERTY_URL);//获取Url
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
        String elementName = DtaInfo.getInstance().getInElemName();//请求数据对象
        //获取交易报文
        //byte[] sendData = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");
        //1.首先需对共享结果的每个属性值进行GBK编码格式转化以及Base64方式加密；
        //CompSDO compSDO = EPOper.getCompSDO(tpId,elementName);
        //遍历对象各个属性并加密
        Map<String, String> parmsMap = ElementUtil.getEnCrypItemValueMap(true,elementName);//获取加密参数
        //2.然后通过方法将反馈信息组合成xml格式反馈报文
        String xmlData = ElementUtil.map2Xml(parmsMap);
        //3.最后整个反馈报文需再次经过Base64方式加密才可生成最终反馈密文，反馈至审批局前置客户端。
        String finalBase64Data = new String(Base64Utils.encode(xmlData.getBytes()));
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA[0]", "finalBase64Data");//覆盖原有的报文数据
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA_LENGTH[0]", Base64Utils.encode(xmlData.getBytes()).length);
    }

    /**
     * 解析交易报文前事件
     * 解密
     *
     * @return
     */

    public void doActionAfterParseData() throws NoSuchProviderException, NoSuchAlgorithmException, UnsupportedEncodingException {

        String tpId = DtaInfo.getInstance().getTpId();
        //获取交易报文
        byte[] recvData = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");

        //1.首先对获取的密文进行Base64解码操作，获得属性值加密的xml格式报文
        byte[] xmlData = Base64Utils.decode(recvData);
        //2.然后经过对报文的解析操作获得核验条件属性值的密文
        String finalBase64Data = new String(xmlData);
        //3.把xml格式的字符串转换为JSON
        XMLSerializer xmlSerializer = new XMLSerializer();
        String jsonStr = xmlSerializer.read(finalBase64Data).toString();
        jsonStr = jsonStr.replace("[[[","");
        jsonStr = jsonStr.replace("]]]","");
        JSONObject jsonObjict = JSONObject.fromObject(jsonStr);
        //4.遍历JSON字符串得到value
        Map<String,String> map = new HashMap();
        for (Object object:jsonObjict.keySet()) {
            Object value = jsonObjict.getString(object.toString());
        //5.通过GBK方式进行解密,最后需对共享条件属性值密文进行Base64解码及GBK编码格式的转化才可获得共享条件属性值明文
            byte [] jeiMa = Base64Utils.decode(value.toString().getBytes("GBK"));
            map.put(object.toString(),new String(jeiMa));
        }
        //6.遍历map
       // String as = ElementUtil.map2Xml(map);
        JSONObject jsonObject = JSONObject.fromObject(map);
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA[0]", jsonObject.toString().getBytes());//覆盖原有的报文数据
        EPOper.put(tpId, "__GDTA_FORMAT[0].__ITEMDATA_LENGTH[0]", jsonObject.toString().getBytes().length);
    }

    /**
     * 解析交易报文前事件
     * PDF输出
     *
     * @return
     */
    public void parseDataBefor() {
        String tpId = DtaInfo.getInstance().getTpId();
        //获取交易报文
        byte[] recvData = (byte[]) EPOper.get(tpId, "__GDTA_FORMAT.__ITEMDATA");

        String base64FileData = new String(recvData); //获取文件内容
        TrcLog.log(PubfuncBean.getLogfile(), "fileData=[%s]", base64FileData);
        if (StringTool.isNullOrEmpty(base64FileData)) {
            throw new BaseException("30070000", "不动产未返回数据！");
        }
       /* BASE64Decoder decoder = new BASE64Decoder();
        try {
            byte [] b = decoder.decodeBuffer(base64String);//base64解码
            for(int i=0;i<b.length;i++){
                if(b[0]<0){
                    b[i]+=256;//调整异常数据
                }
            }*/
        try {
            //base64解码
            byte[] fileData = Base64Utils.decode(recvData);
            //生成本地文件
            String fileName = UUID.randomUUID().toString() + "." + "property";
            File file = new File(PubfuncBean.RECV_FILE_PATH + "property/" + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(fileData);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
           // throw new BaseException(HttpCoreRspCode.QINGDAO_IMVALPROPERTY_FAIL_CODE, e, "文件解析失败！" + e.getMessage());
        }
        //1.文件上传到文件服务器

        // 2.返回url赋值

    }
}
