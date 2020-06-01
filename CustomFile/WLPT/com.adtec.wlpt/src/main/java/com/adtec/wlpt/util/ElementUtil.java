package com.adtec.wlpt.util;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.adtec.starring.datapool.CompSDO;
import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.datapool.FieldSDO;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.respool.PoolOperate;
import com.adtec.starring.struct.dataelem.DataElemItem;
import com.adtec.starring.struct.dataelem.DataElement;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.adtec.wlpt.constant.HttpCoreRspCode;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.util.Base64Utils;

/**
 * ElementUtil
 *
 * @author wujw
 * @date 2019/5/23
 */
public class ElementUtil {
    public static final DecimalFormat decimalFormat = new DecimalFormat("#.00");

    public static double getDouble(String name) {
        Object object = EPOper.get(DtaInfo.getInstance().getTpId(), name);
        return object == null ? 0.00 : (double) object;
    }

    public static int getInt(String name) {
        Object object = EPOper.get(DtaInfo.getInstance().getTpId(), name);
        return object == null ? 0 : (int) object;
    }

    public static Boolean getBoolean(String name) {
        Object object = EPOper.get(DtaInfo.getInstance().getTpId(), name);
        return object == null ? false : (Boolean) object;
    }

    public static String getString(String name) {
        Object object = EPOper.get(DtaInfo.getInstance().getTpId(), name);
        return object == null ? "" : (String) object;
    }

    public static void put(String name, Object value) {
        EPOper.put(DtaInfo.getInstance().getTpId(), name, value);
    }

    /**
     * 从源数据对象属性值拷贝到目录数据对象中
     *
     * @param srcElementName 被取值数组对象数据池中详细名称
     * @param desElementName 待赋值数组对象数据池中详细名称
     */
    public static void copyValue(String srcElementName, String desElementName) {
        Map<String, String> mapper = new HashMap<>();
        DataElement dataElement = PoolOperate.getPlatDataElemHashMap().get(srcElementName);
        ConcurrentHashMap<String, DataElemItem> itemMap = dataElement.getItemList();
        for (String field : itemMap.keySet()) {
            //数据字典
            if (itemMap.get(field).getItemType() == 2) {
                mapper.put(field, field);
            }
        }
        String tpId = DtaInfo.getInstance().getTpId();
        CompSDO srcCompSDO = EPOper.getCompSDO(tpId, srcElementName);
        CompSDO desCompSDO = EPOper.getCompSDO(tpId, desElementName);
        copyValue(srcCompSDO, desCompSDO, mapper);
    }

    /**
     * 从源数据对象属性值拷贝到目录数据对象中
     *
     * @param srcElementName 被取值数组对象数据池中详细名称
     * @param desElementName 待赋值数组对象数据池中详细名称
     * @param mapper         属性名的映射集
     */
    public static void copyValue(String srcElementName, String desElementName, Map<String, String> mapper) {
        TrcLog.info(PubfuncBean.getLogfile(), "==拷贝数据元素 src[%s] des[%s]==", srcElementName, desElementName);
        CompSDO srcCompSDO = getCompSDO(srcElementName);
        CompSDO desCompSDO = getCompSDO(desElementName);
        copyValue(srcCompSDO, desCompSDO, mapper);
    }

    /**
     * 从源数据对象属性值拷贝到目录数据对象中(循环对象)
     *
     * @param srcElementName 被取值数组对象数据池中详细名称
     * @param desElementName 待赋值数组对象数据池中详细名称
     * @param mapper         属性名的映射集
     */
    public static void copyArrayValue(String srcElementName, String desElementName, Map<String, String> mapper) {
        TrcLog.info(PubfuncBean.getLogfile(), "==拷贝数据元素 src[%s] des[%s]==", srcElementName, desElementName);
        //忽略最后一级的下标
        if (srcElementName.endsWith("]")) {
            srcElementName = srcElementName.substring(0, srcElementName.lastIndexOf("["));
        }
        //获取元素下标
        int maxIndex = getMaxIndex(srcElementName);
        //循环拷贝
        for (int i = 0; i < maxIndex; i++) {
            TrcLog.info(PubfuncBean.getLogfile(), "==循环拷贝下标[%d]==", i);
            CompSDO srcCompSDO = getCompSDO(srcElementName, i);
            CompSDO desCompSDO = getCompSDO(desElementName, i);
            copyValue(srcCompSDO, desCompSDO, mapper);
        }
    }

    /**
     * 从源数据对象属性值拷贝到目录数据对象中
     *
     * @param srcCompSDO 源数据对象
     * @param desCompSDO 目的数据对象
     * @param mapper     属性名的映射集
     */
    public static void copyValue(CompSDO srcCompSDO, CompSDO desCompSDO, Map<String, String> mapper) {
        //循环参数列表
        for (Map.Entry<String, String> entry : mapper.entrySet()) {
            //所有类型先转换成String
            String value = getValueString(srcCompSDO, entry.getKey());
            //目的数据袁术赋值
            setValueString(desCompSDO, entry.getValue(), value);
            TrcLog.info(PubfuncBean.getLogfile(), "拷贝数据项 src[%s] des[%s] value=[%s]",
                    entry.getKey(), entry.getValue(), value);
        }
    }

    /**
     * 获取数据对象属性并转换成String
     *
     * @param compSDO  源数据对象
     * @param itemName 属性名称
     * @return 值
     */
    public static String getValueString(CompSDO compSDO, String itemName) {
        //数据项
        DataElemItem dataElemItem = compSDO.getDataElement().getDataElemItem(itemName);
        if (dataElemItem == null) {
            TrcLog.log(PubfuncBean.getLogfile(),
                    String.format("数据对象[%s]属性[%s]不存在!", compSDO.getDataElement(), itemName));
            return null;
        }
        //数据类型
        int dataType = dataElemItem.getDataDict().getDataType();
        Object obj = compSDO.getValue(itemName);
        switch (dataType) {
            case 1: // string
                return (String) obj;
            case 3: // int
                return obj == null ? null : String.valueOf((int) obj);
            case 6: // double
                return obj == null ? null : decimalFormat.format((double) obj);
            default:
                TrcLog.info(PubfuncBean.getLogfile(), "拷贝数据项 itemName[%s] dataType[%d] 不支持!", itemName, dataType);
                return null;
        }
    }

    /**
     * 使用String对数据元素赋值
     *
     * @param compSDO  目的数据对象
     * @param itemName 属性值
     * @param value    值
     * @return
     */
    public static void setValueString(CompSDO compSDO, String itemName, String value) {
        if (value == null) {
            return;
        }
        //数据项
        DataElemItem dataElemItem = compSDO.getDataElement().getDataElemItem(itemName);
        if (dataElemItem == null) {
            TrcLog.log(PubfuncBean.getLogfile(),
                    String.format("数据对象[%s]属性[%s]不存在!", compSDO.getDataElement(), itemName));
            return;
        }
        //数据类型
        int dataType = dataElemItem.getDataDict().getDataType();
        switch (dataType) {
            case 1: // string
                compSDO.setValue(itemName, value);
                break;
            case 3: // int
                compSDO.setValue(itemName, Integer.valueOf(value));
                break;
            case 6: // double
                compSDO.setValue(itemName, Double.valueOf(value));
                break;
            default:
                //do nothing
        }
    }

    /**
     * 获取指定数据对象
     *
     * @param dataElementName 数据元素--使用"."分割对象 “[i]"标注数组下标
     * @return
     */
    public static CompSDO getCompSDO(String dataElementName) {
        return getCompSDO(dataElementName, -1);
    }

    /**
     * 获取指定数据对象
     *
     * @param dataElementName 数据元素--使用"."分割对象 “[i]"标注数组下标
     * @return
     */
    public static CompSDO getCompSDO(String dataElementName, int suffixNo) {
        CompSDO compSDO = null;
        String[] array = dataElementName.split("\\.");
        //没有子对象
        if (array.length == 0) {
            return EPOper.getCompSDO(DtaInfo.getInstance().getTpId(), dataElementName);
        }
        //获取子对象
        for (int i = 0; i < array.length; i++) {
            String arrayStr = array[i];
            String itemName = array[i];
            int index = arrayStr.indexOf("[");
            //带下标
            if (index > 0) {
                itemName = arrayStr.substring(0, index);
                index = Integer.valueOf(arrayStr.substring(index + 1, arrayStr.indexOf("]")));
            } else {
                index = 0;
            }
            //有效下标参数
            if (suffixNo >= 0 && i == array.length - 1) {
                index = suffixNo;
            }
            //根据下标取数据对象
            if (i == 0) {
                compSDO = EPOper.getCompSDO(DtaInfo.getInstance().getTpId(), itemName, index);
            } else {
                compSDO = (CompSDO) compSDO.getValue(itemName, index);
            }
        }
        return compSDO;
    }

    /**
     * 获取属性集合,只支持数据项类型为数据字段
     *
     * @param containsSDO 是否包含子对象， 子对象默认转换成json串
     * @param elementName 数据对象名称
     * @return
     */
    public static Map<String, String> getItemValueMap(boolean containsSDO, String elementName) {
        Map<String, String> result = new HashMap<>();
        //数据对象
        CompSDO compSDO = getCompSDO(elementName);
        //JSON对象
        JSONObject jsonObject = (JSONObject) JSONObject.parseObject(compSDO.toJSON()).get(elementName);
        TrcLog.log(PubfuncBean.getLogfile(), "数据对象json=[%s]", jsonObject.toJSONString());
        //遍历数据项
        Map<String, DataElemItem> itemMap = compSDO.getDataElement().getItemList();
        for (Map.Entry<String, DataElemItem> entry : itemMap.entrySet()) {
            //数据字典
            if (entry.getValue().getItemType() == 2) {
                String value = getValueString(compSDO, entry.getKey());
                if (!StringTool.isNullOrEmpty(value)) {
                    result.put(entry.getKey(), value);
                }
            } else if (containsSDO && entry.getValue().getItemType() == 1) {
                TrcLog.log(PubfuncBean.getLogfile(), "数据节点=[%s]", entry.getKey());
                //数据对象, 转成JSON字符串
                if (entry.getValue().isArray()) {
                    JSONArray jsonArray = (JSONArray) jsonObject.get(entry.getKey());
                    if (jsonArray != null) {
                        TrcLog.log(PubfuncBean.getLogfile(), "数据对象json=[%s]", jsonArray.toJSONString());
                        result.put(entry.getKey(), jsonArray.toJSONString());
                    }
                } else {
                    JSONObject subObject = (JSONObject) jsonObject.get(entry.getKey());
                    if (subObject != null) {
                        TrcLog.log(PubfuncBean.getLogfile(), "数据对象json=[%s]", subObject.toJSONString());
                        result.put(entry.getKey(), subObject.toJSONString());
                    }
                }
            }
        }
        return result;
    }

    /**
     * 数据元素赋值,只支持数据项类型为数据字段
     *
     * @return
     */
    public static void setItemValueMap(String elementName, Map<String, String> itemValueMap) {
        CompSDO compSDO = getCompSDO(elementName);
        setItemValueMap(compSDO, itemValueMap);
    }

    /**
     * 数据元素赋值,只支持数据项类型为数据字段
     *
     * @return
     */
    public static void setItemValueMap(CompSDO compSDO, Map<String, String> itemValueMap) {
        Map<String, DataElemItem> itemMap = compSDO.getDataElement().getItemList();
        for (Map.Entry<String, DataElemItem> entry : itemMap.entrySet()) {
            //数据字典
            if (entry.getValue().getItemType() == 2) {
                if (itemValueMap.containsKey(entry.getKey())) {
                    setValueString(compSDO, entry.getKey(), itemValueMap.get(entry.getKey()));
                }
            }
        }
    }

    /**
     * 数据元素赋值, 子对象赋值
     */
    public static void setItemElementMap(String elementName, String itemName, List<Map<String, String>> xmlAtrrMapList) {
        CompSDO compSDO = ElementUtil.getCompSDO(elementName);
        DataElement dataElement = compSDO.getDataElement();
        Map<String, DataElemItem> itemMap = dataElement.getItemList();
        for (DataElemItem dataElemItem : itemMap.values()) {
            //数据对象
            if (dataElemItem.getItemType() == 1) {
                //创建子对象
                FieldSDO fieldSDO = new FieldSDO(dataElemItem);
                for (Map<String, String> attrMap : xmlAtrrMapList) {
                    //创建子对象成员
                    CompSDO tempSDO = CompSDO.getCompSDO(dataElemItem.getTypeName());
                    setItemValueMap(tempSDO, attrMap);
                    fieldSDO.get().add(tempSDO);
                }
                //添加到父对象中
                compSDO.set(dataElemItem.getName(), fieldSDO);
            }
        }
    }

    /**
     * 获取数据元素最大下标
     *
     * @param elementName
     * @return
     */
    public static int getMaxIndex(String elementName) {
        int maxIndex = 0;
        String tpId = DtaInfo.getInstance().getTpId();
        //如果包含子对象
        if (elementName.contains(".")) {
            int index = elementName.lastIndexOf(".");
            CompSDO compSDO = getCompSDO(elementName.substring(0, index));
            FieldSDO fieldSDO = compSDO.getItemList().get(elementName.substring(index + 1));
            if (fieldSDO != null) {
                maxIndex = fieldSDO.size();
            }
        } else {
            //如果带下标则忽略下标
            if (elementName.contains("[")) {
                maxIndex = EPOper.getSuffixNo(tpId, elementName.substring(0, elementName.indexOf("[")));
            } else {
                maxIndex = EPOper.getSuffixNo(tpId, elementName);
            }
        }
        return maxIndex;
    }

    /**
     * 获取加密后的数据对象
     *
     * @param containsSDO 是否包含子对象， 子对象默认转换成json串
     * @param elementName 数据对象名称
     * @return
     */
    public static Map<String, String> getEnCrypItemValueMap(boolean containsSDO, String elementName) {
        Map<String, String> result = new HashMap<>();
        //数据对象
        CompSDO compSDO = getCompSDO(elementName);
        //JSON对象
        JSONObject jsonObject = (JSONObject) JSONObject.parseObject(compSDO.toJSON()).get(elementName);
        TrcLog.log(PubfuncBean.getLogfile(), "数据对象json=[%s]", jsonObject.toJSONString());
        //遍历数据项
        Map<String, DataElemItem> itemMap = compSDO.getDataElement().getItemList();
        //每个属性值进行GBK编码格式转化以及Base64方式加密
        for (Map.Entry<String, DataElemItem> entry : itemMap.entrySet()) {
            //数据字典
            if (entry.getValue().getItemType() == 2) {
                String value = getValueString(compSDO, entry.getKey());
                String base64Value = "";
                try {
                    if (!StringTool.isNullOrEmpty(value)) {
                        base64Value = new String(Base64Utils.encode(value.getBytes("GBK")));
                        result.put(entry.getKey(), base64Value);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new BaseException(HttpCoreRspCode.COMM_FAIL_CODE, e, e.getMessage());
                }
            } else if (containsSDO && entry.getValue().getItemType() == 1) {//数据对象
                TrcLog.log(PubfuncBean.getLogfile(), "数据节点=[%s]", entry.getKey());
                //数据对象, 转成JSON字符串
                if (entry.getValue().isArray()) {
                    JSONArray jsonArray = (JSONArray) jsonObject.get(entry.getKey());
                    if (jsonArray != null) {
                        TrcLog.log(PubfuncBean.getLogfile(), "数据对象json=[%s]", jsonArray.toJSONString());
                        String base64ArrayData = "";
                        try {
                             base64ArrayData = new String(Base64Utils.encode(jsonArray.toJSONString().getBytes("GBK")));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        result.put(entry.getKey(), base64ArrayData);
                    }
                } else {
                    JSONObject subObject = (JSONObject) jsonObject.get(entry.getKey());
                    if (subObject != null) {
                        TrcLog.log(PubfuncBean.getLogfile(), "数据对象json=[%s]", subObject.toJSONString());
                        String base64ObjData = "";
                        try {
                            base64ObjData = new String(Base64Utils.encode(subObject.toJSONString().getBytes("GBK")));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        result.put(entry.getKey(), base64ObjData);
                    }
                }
            }
        }
        return result;
    }

    public static String map2Xml(Map<String,String> map){
        StringBuffer sb = new StringBuffer();
        /*sb.append("<<?xml version=\"1.0\" encoding=\"GBK\"?>>");
        sb.append("<xml>");//根节点，待询问*/
        Document document = DocumentHelper.createDocument();
        Element rootElement = document.addElement("p");//根节点，待询问
        map.forEach((key, value) -> {
            TrcLog.info(PubfuncBean.getLogfile(), "数据对象map=[%s][%s]", key, value);
            /*sb.append("<"+ key + ">");
            sb.append(value);
            sb.append("</"+key +">");*/
            rootElement.addElement("s").addAttribute(key, value);

        });
        //sb.append("</xml>");//根节点，待询问
        return document.asXML();
    }
}
