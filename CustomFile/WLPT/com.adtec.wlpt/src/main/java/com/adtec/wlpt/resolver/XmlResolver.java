package com.adtec.wlpt.resolver;

import com.adtec.starring.datapool.CompSDO;
import com.adtec.starring.datapool.EPOper;
import com.adtec.starring.datapool.FieldSDO;
import com.adtec.starring.datapool.SDO;
import com.adtec.starring.exception.BaseException;
import com.adtec.starring.log.TrcLog;
import com.adtec.starring.respool.PoolOperate;
import com.adtec.starring.struct.dataelem.DataElemItem;
import com.adtec.starring.struct.dataelem.DataElement;
import com.adtec.starring.struct.dataelem.SimpleDataElemItem;
import com.adtec.starring.struct.dataelem.SimpleDataElement;
import com.adtec.starring.struct.dta.DtaInfo;
import com.adtec.starring.util.StringTool;
import com.adtec.wlpt.PubfuncBean;

import org.dom4j.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XmlResolver
 *
 * @author wujw
 * @date 2019/10/17
 */
public class XmlResolver {
    private Document document;

    public XmlResolver(String xmlStr) throws DocumentException {
        document = DocumentHelper.parseText(xmlStr);
    }

    /**
     * 获取root节点下itemName节点的属性
     * @param itemName
     * @return
     */
    public Map<String, String> getMergeItemAtrr(String itemName) {
        Map<String, String> itemAtrrMap = new HashMap<>();
        List<Map<String, String>> list = getSubItemAtrr(null, null, itemName);
        list.forEach(itemAtrrMap::putAll);
        return itemAtrrMap;
    }

    /**
     * 取itemName节点下subItemName节点的属性
     *
     * @param rootElement
     * @param itemName
     * @param subItemName
     * @return
     */
    public List<Map<String, String>> getSubItemAtrr(Element rootElement, String itemName, String subItemName) {
        List<Map<String, String>> list = new ArrayList<>();
        Element element;
        if (rootElement == null) {
            rootElement = document.getRootElement();
        }
        //获取item节点
        if (StringTool.isNullOrEmpty(itemName)) {
            element = rootElement;
        } else {
            element = rootElement.element(itemName);
        }
        if (element == null) {
            return list;
        }
        //获取subItem节点列表
        List<Element> subElementList = element.elements(subItemName);
        subElementList.forEach(
                subElement -> {
                    //获取subItem节点的属性
                    Map<String, String> attrMap = new HashMap<>();
                    List<Attribute> attributeList = subElement.attributes();
                    attributeList.forEach(
                            attribute -> {
                                TrcLog.log(PubfuncBean.getLogfile(), "key=[%s] value=[%s]", attribute.getName(), attribute.getValue());
                                attrMap.put(attribute.getName(), attribute.getValue());
                            }
                    );
                    list.add(attrMap);
                }
        );
        return list;
    }

    /**
     * 写入数据元素池
     *
     * @param rootElementName
     */
    protected void writToElementPool(String rootElementName) {
        if (rootElementName != null && !"".equals(rootElementName)) {
            Element element = document.getRootElement();
            DtaInfo dtaInfo = DtaInfo.getInstance();
            CompSDO compSDO = SDO.getCompSDO(rootElementName);
            DataElement dataElement = compSDO.getDataElement();
            EPOper.put(dtaInfo.getTpId(), dataElement.getName() + "[" + 0 + "]", compSDO);
            if (dataElement.getName().equals("__DYNAMIC_DATA")) {
                compSDO.setValue("__TYPE", 1);
                compSDO.setValue("__BUF", document.asXML());
            } else {
                SimpleDataElement simpleElem = null;
                if (dtaInfo.isEnable()) {
                    simpleElem = (SimpleDataElement) dtaInfo.getElemList().get(dataElement.getName());
                    if (simpleElem == null) {
                        return;
                    }
                }

                ConcurrentHashMap<String, DataElemItem> map = dataElement.getItemList();
                Iterator iterator = map.keySet().iterator();

                while (true) {
                    DataElemItem value;
                    String elementName;
                    String tagName;
                    SimpleDataElemItem subElement;
                    while (true) {
                        if (!iterator.hasNext()) {
                            return;
                        }

                        String key = (String) iterator.next();
                        value = (DataElemItem) map.get(key);
                        elementName = value.getName();
                        tagName = "";
                        if (!dtaInfo.isEnable()) {
                            break;
                        }

                        subElement = simpleElem.getDataElemItem(key);
                        if (subElement != null) {
                            tagName = subElement.getTagName();
                            break;
                        }
                    }

                    List obj;
                    FieldSDO fieldSDO;
                    if (value.getItemType() == 1) {
                        fieldSDO = new FieldSDO(value);
                        fieldSDO.setElementName(rootElementName);
                        if (StringTool.isNullOrEmpty(tagName)) {
                            obj = element.elements(elementName);
                        } else {
                            obj = element.elements(tagName);
                        }

                        if (obj == null) {
                            throw new BaseException("11008", new Object[]{elementName});
                        }

                        this.handleSubElement(obj, fieldSDO);
                        compSDO.set(elementName, fieldSDO);
                    } else {
                        Element subXmlElement;
                        FieldSDO fieldSDO1;
                        if (value.getItemType() == 2) {
                            if (value.getNodeType() == 1) {
                                subXmlElement = null;
                                if (StringTool.isNullOrEmpty(tagName)) {
                                    subXmlElement = element.element(elementName);
                                } else {
                                    subXmlElement = element.element(tagName);
                                }

                                fieldSDO = new FieldSDO(value);
                                if (subXmlElement != null) {
                                    fieldSDO.set(subXmlElement.getText());
                                }

                                compSDO.set(elementName, fieldSDO);
                            } else if (value.getNodeType() == 2) {
                                fieldSDO = new FieldSDO(value);
                                String attValue;
                                if (!StringTool.isNullOrEmpty(tagName)) {
                                    attValue = element.attributeValue(tagName);
                                } else {
                                    attValue = element.attributeValue(elementName);
                                }

                                if (attValue != null) {
                                    fieldSDO.set(attValue);
                                }

                                compSDO.set(elementName, fieldSDO);
                            }
                        } else if (value.getItemType() == 3) {
                            if (!StringTool.isNullOrEmpty(tagName)) {
                                subXmlElement = element.element(tagName);
                            } else {
                                subXmlElement = element.element(elementName);
                            }
                            fieldSDO = new FieldSDO(value);
                            fieldSDO.set(new ArrayList());
                            compSDO.set(elementName, fieldSDO);
                        }
                    }
                }
            }
        }
    }

    private void handleSubElement(List<Element> elementList, FieldSDO fieldSDO) {
        Iterator<Element> iteratorEle = elementList.iterator();
        DtaInfo dtaInfo = DtaInfo.getInstance();
        SimpleDataElement simpleElem = null;
        if (dtaInfo.isEnable()) {
            simpleElem = (SimpleDataElement) dtaInfo.getElemList().get(fieldSDO.getDataElemItem().getTypeName());
            if (simpleElem == null) {
                return;
            }
        }

        label80:
        while (iteratorEle.hasNext()) {
            Element element = (Element) iteratorEle.next();
            CompSDO compSDO = SDO.getCompSDO(fieldSDO.getDataElemItem().getTypeName());
            DataElement dataElement = compSDO.getDataElement();
            EPOper.put(DtaInfo.getInstance().getTpId(), dataElement.getName(), compSDO);
            fieldSDO.set(compSDO);
            if (dataElement.getName().equals("__DYNAMIC_DATA")) {
                String xml = element.asXML();
                compSDO.setValue("__TYPE", 1);
                compSDO.setValue("__BUF", xml);
                return;
            }

            ConcurrentHashMap<String, DataElemItem> map = dataElement.getItemList();
            Iterator iterator = map.keySet().iterator();

            while (true) {
                String key;
                DataElemItem value;
                String elementName;
                String tagName;
                SimpleDataElemItem subElemenet;
                while (true) {
                    if (!iterator.hasNext()) {
                        continue label80;
                    }

                    key = (String) iterator.next();
                    value = (DataElemItem) map.get(key);
                    elementName = value.getName();
                    tagName = "";
                    if (!dtaInfo.isEnable()) {
                        break;
                    }

                    subElemenet = simpleElem.getDataElemItem(key);
                    if (subElemenet != null) {
                        tagName = subElemenet.getTagName();
                        break;
                    }
                }

                FieldSDO field;
                if (value.getItemType() == 1) {
                    field = new FieldSDO(value);
                    field.setElementName(fieldSDO.getDataElemItem().getTypeName());
                    List<Element> subElemenetList = element.elements(elementName);
                    DataElement subDataElement = (DataElement) PoolOperate.getResData(2, value.getTypeName());
                    if (subElemenetList == null) {
                        throw new BaseException("11008", new Object[]{elementName});
                    }

                    this.handleSubElement(subElemenetList, field);
                    compSDO.set(elementName, field);
                } else {
                    Element subXmlElemenet;
                    if (value.getItemType() == 2) {
                        if (value.getNodeType() == 1) {
                            if (elementName.equals(compSDO.getElementName())) {
                                field = new FieldSDO(value);
                                field.set(element.getText());
                                compSDO.set(elementName, field);
                            } else {
                                if (StringTool.isNullOrEmpty(tagName)) {
                                    subXmlElemenet = element.element(elementName);
                                } else {
                                    subXmlElemenet = element.element(tagName);
                                }

                                field = new FieldSDO(value);
                                if (subXmlElemenet != null) {
                                    field.set(subXmlElemenet.getText());
                                }

                                compSDO.set(elementName, field);
                            }
                        } else if (value.getNodeType() == 2) {
                            field = new FieldSDO(value);
                            if (element != null && element.attributeValue(elementName) != null) {
                                field.set(element.attributeValue(elementName));
                                compSDO.set(elementName, field);
                            }
                        }
                    } else if (value.getItemType() == 3) {
                        if (StringTool.isNullOrEmpty(tagName)) {
                            subXmlElemenet = element.element(key);
                        } else {
                            subXmlElemenet = element.element(tagName);
                        }
                        field = new FieldSDO(value);
                        compSDO.set(elementName, field);
                    }
                }
            }
        }

    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
