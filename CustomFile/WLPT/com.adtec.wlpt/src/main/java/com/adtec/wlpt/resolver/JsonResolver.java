package com.adtec.wlpt.resolver;

import com.adtec.starring.datapool.CompSDO;
import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.datapool.FieldSDO;
import com.adtec.starring.datapool.SDO;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.BaseLog;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.pubmethod.Message;
import com.adtec.starring.respool.PoolOperate;
import com.adtec.starring.struct.dataelem.DataElemItem;
import com.adtec.starring.struct.dataelem.DataElement;
import com.adtec.starring.struct.dataelem.SimpleDataElemItem;
import com.adtec.starring.struct.dataelem.SimpleDataElement;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JsonResolver
 *
 * @author wujw
 * @date 2019/10/13
 */
public class JsonResolver {
    private JSONObject jsonObject;

    public JsonResolver(String jsonStr) throws JSONException {
        jsonObject = JSONObject.parseObject(jsonStr);
    }

    public JsonResolver(CompSDO compSDO) {
        jsonObject = JSONObject.parseObject(compSDO.toJSON()).getJSONObject(compSDO.getElementName());
    }

    public JsonResolver(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    /**
     * 从json对象中获取Map参数
     *
     * @return
     */
    public Map<String, String> getJsonMap() {
        Map<String, String> jsonMap = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            Object object = jsonObject.get(key);
            if (object instanceof String) {
                jsonMap.put(key, (String) jsonObject.get(key));
            }
        }
        return jsonMap;
    }

    /**
     * 写入数据元素池
     * @param rootElementName
     */
    public void writToElementPool(String rootElementName) {
        String jsonStr = jsonObject.toJSONString();
        TrcLog.info(PubfuncBean.getLogfile(), "dataElementValue=[%s]", jsonStr);
        ObjectMapper mapper = new ObjectMapper();
        DtaInfo dtaInfo = DtaInfo.getInstance();
        TrcLog.info(PubfuncBean.getLogfile(), "rootElementName=[%s]", rootElementName);
        if (rootElementName != null && !"".equals(rootElementName)) {
            DataElement dataElement = (DataElement) PoolOperate.getResData(2, rootElementName);
            Map jsonInMap = null;

            try {
                jsonInMap = (Map) mapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
                });
                if (jsonInMap == null) {
                    return;
                }
            } catch (JsonParseException var21) {
                throw new BaseException("16021", var21, new Object[]{rootElementName});
            } catch (JsonMappingException var22) {
                throw new BaseException("16021", var22, new Object[]{rootElementName});
            } catch (IOException var23) {
                throw new BaseException("16021", var23, new Object[]{rootElementName});
            }

            CompSDO compSDO = EPOper.getCompSDO(dtaInfo.getTpId(), dataElement.getName());
            if (dataElement.getName().equals("__DYNAMIC_DATA")) {
                compSDO.setValue("__TYPE", 0);
                compSDO.setValue("__BUF", jsonStr);
            } else {
                SimpleDataElement simpleElem = null;
                if (dtaInfo.isEnable()) {
                    simpleElem = (SimpleDataElement) dtaInfo.getElemList().get(dataElement.getName());
                    if (simpleElem == null) {
                        return;
                    }
                }

                Iterator iterator = dataElement.getItemList().keySet().iterator();

                while (true) {
                    while (true) {
                        String key;
                        DataElemItem value;
                        String tagName;
                        while (true) {
                            if (!iterator.hasNext()) {
                                return;
                            }

                            key = (String) iterator.next();
                            value = (DataElemItem) dataElement.getItemList().get(key);
                            tagName = "";
                            if (!dtaInfo.isEnable()) {
                                break;
                            }

                            SimpleDataElemItem simpleDataElemItem = simpleElem.getDataElemItem(key);
                            if (simpleDataElemItem != null) {
                                tagName = simpleDataElemItem.getTagName();
                                break;
                            }
                        }

                        FieldSDO fieldSDO;
                        if (value.getItemType() == 1) {
                            fieldSDO = new FieldSDO((DataElemItem) dataElement.getItemList().get(key));
                            DataElement subDataElement = (DataElement) PoolOperate.getResData(2, value.getTypeName());
                            if (fieldSDO.isArray()) {
                                List list;
                                if (StringTool.isNullOrEmpty(tagName)) {
                                    list = (List) jsonInMap.get(key);
                                } else {
                                    list = (List) jsonInMap.get(tagName);
                                }

                                if (list != null) {
                                    Iterator it = list.iterator();

                                    while (it.hasNext()) {
                                        Map map = (Map) it.next();
                                        this.handleTree(map, fieldSDO, subDataElement, mapper);
                                    }
                                }
                            } else if (StringTool.isNullOrEmpty(tagName)) {
                                this.handleTree(jsonInMap.get(key), fieldSDO, subDataElement, mapper);
                            } else {
                                this.handleTree(jsonInMap.get(tagName), fieldSDO, subDataElement, mapper);
                            }

                            compSDO.set(key, fieldSDO);
                        } else if (value.getItemType() == 2) {
                            if (rootElementName.equals("__DYNAMIC_DATA") && key.equals("__BUF")) {
                                Map map;
                                if (StringTool.isNullOrEmpty(tagName)) {
                                    map = (Map) jsonInMap.get(key);
                                } else {
                                    map = (Map) jsonInMap.get(tagName);
                                }

                                String json = this.mapToJsonString(map);
                                compSDO.setValue(key, json);
                            } else {
                                fieldSDO = new FieldSDO((DataElemItem) dataElement.getItemList().get(key));
                                if (fieldSDO.isArray()) {
                                    Iterator temp;
                                    if (StringTool.isNullOrEmpty(tagName)) {
                                        temp = ((ArrayList) ((ArrayList) jsonInMap.get(key))).iterator();
                                    } else {
                                        temp = ((ArrayList) ((ArrayList) jsonInMap.get(tagName))).iterator();
                                    }

                                    Object o;
                                    for (; temp.hasNext(); fieldSDO.set(o)) {
                                        o = temp.next();
                                        if (o instanceof Boolean) {
                                            if ((Boolean) o) {
                                                o = "true";
                                            } else {
                                                o = "false";
                                            }
                                        }
                                    }
                                } else if (StringTool.isNullOrEmpty(tagName)) {
                                    fieldSDO.set(jsonInMap.get(key));
                                } else {
                                    fieldSDO.set(jsonInMap.get(tagName));
                                }

                                compSDO.set(key, fieldSDO);
                            }
                        } else if (value.getItemType() == 3) {
                            fieldSDO = new FieldSDO(value);

                            try {
                                Map map;
                                if (StringTool.isNullOrEmpty(tagName)) {
                                    map = (Map) jsonInMap.get(key);
                                } else {
                                    map = (Map) jsonInMap.get(tagName);
                                }

                                mapper.writeValueAsString(map);
                            } catch (JsonProcessingException var19) {
                                throw new RuntimeException(var19);
                            }

                            fieldSDO.set(new ArrayList());
                            compSDO.set(key, fieldSDO);
                        }
                    }
                }
            }
        }
    }

    private void handleTree(Object object, FieldSDO fieldSDO, DataElement dataElement, ObjectMapper mapper) {
        Map obj = (Map) object;
        if (obj != null) {
            CompSDO compSDO = SDO.getCompSDO(dataElement.getName());
            fieldSDO.get().add(compSDO);
            if (dataElement.getName().equals("__DYNAMIC_DATA")) {
                String json = this.mapToJsonString(obj);
                compSDO.setValue("__TYPE", 0);
                compSDO.setValue("__BUF", json);
            } else {
                DtaInfo dtaInfo = DtaInfo.getInstance();
                SimpleDataElement simpleElem = null;
                if (dtaInfo.isEnable()) {
                    simpleElem = (SimpleDataElement) dtaInfo.getElemList().get(dataElement.getName());
                    if (simpleElem == null) {
                        return;
                    }
                }

                ConcurrentHashMap<String, DataElemItem> map = dataElement.getItemList();
                Iterator iterator = map.entrySet().iterator();

                while (true) {
                    while (true) {
                        DataElemItem value;
                        String elementName;
                        String tagName;
                        while (true) {
                            if (!iterator.hasNext()) {
                                return;
                            }

                            Map.Entry<String, DataElemItem> e = (Map.Entry) iterator.next();
                            value = (DataElemItem) e.getValue();
                            elementName = (String) e.getKey();
                            tagName = "";
                            if (!dtaInfo.isEnable()) {
                                break;
                            }

                            SimpleDataElemItem simpleDataElemItem = simpleElem.getDataElemItem(elementName);
                            if (simpleDataElemItem != null) {
                                tagName = simpleDataElemItem.getTagName();
                                break;
                            }
                        }

                        Iterator temp;
                        FieldSDO field;
                        if (value.getItemType() == 1) {
                            field = new FieldSDO((DataElemItem) dataElement.getItemList().get(elementName));
                            DataElement subDataElement = (DataElement) PoolOperate.getResData(2, value.getTypeName());
                            if (field.isArray()) {
                                temp = null;

                                List list;
                                try {
                                    if (StringTool.isNullOrEmpty(tagName)) {
                                        list = (List) obj.get(elementName);
                                    } else {
                                        list = (List) obj.get(tagName);
                                    }
                                } catch (Exception var22) {
                                    throw new BaseException("11007", var22, new Object[]{elementName + "不能转换成数组"});
                                }

                                if (list != null && list.size() > 0) {
                                    Iterator it = list.iterator();

                                    while (it.hasNext()) {
                                        Map submap = (Map) it.next();
                                        this.handleTree(submap, field, subDataElement, mapper);
                                    }
                                }
                            } else {
                                this.handleTree(obj.get(elementName), field, subDataElement, mapper);
                            }

                            compSDO.set(elementName, field);
                        } else if (value.getItemType() != 2) {
                            if (value.getItemType() == 3) {
                                field = new FieldSDO((DataElemItem) dataElement.getItemList().get(elementName));

                                try {
                                    mapper.writeValueAsString(obj);
                                } catch (JsonProcessingException var21) {
                                    var21.printStackTrace(BaseLog.getExpOut());
                                    throw new BaseException("");
                                }

                                compSDO.set(elementName, field);
                            }
                        } else {
                            field = new FieldSDO((DataElemItem) dataElement.getItemList().get(elementName));
                            Object o;
                            if (field.isArray()) {
                                for (temp = ((ArrayList) obj.get(elementName)).iterator(); temp.hasNext(); field.set(o)) {
                                    o = temp.next();
                                    if (o instanceof Boolean) {
                                        if ((Boolean) o) {
                                            o = "true";
                                        } else {
                                            o = "false";
                                        }
                                    }
                                }
                            } else {
                                if (StringTool.isNullOrEmpty(tagName)) {
                                    o = obj.get(elementName);
                                } else {
                                    o = obj.get(tagName);
                                }

                                if (o instanceof Boolean) {
                                    if ((Boolean) o) {
                                        o = "true";
                                    } else {
                                        o = "false";
                                    }
                                }

                                field.set(o);
                            }

                            compSDO.set(elementName, field);
                        }
                    }
                }
            }
        }
    }

    private String mapToJsonString(Map<String, Object> map) {
        String json = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(map);
            json = new String(json.getBytes(System.getProperty("file.encoding")), "UTF-8");
            return json;
        } catch (Exception var4) {
            throw new BaseException("10000", new Object[]{"生成JSON失败"});
        }
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }
}
