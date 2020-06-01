package com.adtec.wlpt.http;

import org.apache.http.Header;
import org.apache.http.cookie.Cookie;

import java.util.List;
/**
 * HttpResult
 *
 * @author wujw
 * @date 2019/10/5
 */
public class HttpResult {
    /**
     * 响应的Header信息
     */
    private Header[] headers;

    /**
     * 响应的Cookie信息
     */
    private List<Cookie> cookies;

    /**
     * 响应状态码
     */
    private int statusCode;

    /**
     * 响应内容的类型
     */
    private String contentType;

    /**
     * 响应的内容是否是文本类型
     */
    private boolean isTextType;

    /**
     * 响应的内容（字符串形式）
     */
    private String stringContent;

    /**
     * 响应的内容（字节数组形式）
     */
    private byte[] byteArrayContent;

    public Header[] getHeaders() {
        return headers;
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isTextType() {
        return isTextType;
    }

    public void setTextType(boolean textType) {
        isTextType = textType;
    }

    public String getStringContent() {
        return stringContent;
    }

    public void setStringContent(String stringContent) {
        this.stringContent = stringContent;
    }

    public byte[] getByteArrayContent() {
        return byteArrayContent;
    }

    public void setByteArrayContent(byte[] byteArrayContent) {
        this.byteArrayContent = byteArrayContent;
    }
}
