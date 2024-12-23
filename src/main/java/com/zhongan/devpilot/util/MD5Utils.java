package com.zhongan.devpilot.util;

import com.intellij.openapi.vfs.VirtualFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang3.StringUtils;

public class MD5Utils {

    public static String calculateMD5(VirtualFile virtualFile) {
        try {
            return calculateMD5(virtualFile.contentsToByteArray());
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }

    public static String calculateMD5(byte[] contentBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(contentBytes);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return StringUtils.EMPTY;
        }
    }

}
