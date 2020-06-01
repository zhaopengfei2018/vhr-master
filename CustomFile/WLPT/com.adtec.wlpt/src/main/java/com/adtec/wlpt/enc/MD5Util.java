package com.adtec.wlpt.enc;

import org.apache.struts2.util.tomcat.buf.HexUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5Util
 *
 * @author wujw
 * @date 2019/10/2
 */
public class MD5Util {
    /**
     * MD5编码运算
     *
     * @param srcStr
     * @return
     */
    public static String encrypt(String srcStr) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.reset();
        messageDigest.update(srcStr.getBytes());
        return HexUtils.toHexString(messageDigest.digest());
    }
}
