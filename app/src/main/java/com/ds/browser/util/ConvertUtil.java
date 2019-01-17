package com.ds.browser.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ConvertUtil {
    //inputStream转outputStream
    public static ByteArrayOutputStream parse(InputStream inputStream) throws Exception {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read()) != -1) {
            swapStream.write(length);
        }
        return swapStream;
    }
    //outputStream转inputStream
    public static ByteArrayInputStream parse(OutputStream outputStream) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos = (ByteArrayOutputStream) outputStream;
        ByteArrayInputStream swapStream = new ByteArrayInputStream(baos.toByteArray());
        return swapStream;
    }
    //inputStream转String
    public static String parseToString(InputStream inputStream) throws Exception {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        int length;
        while ((length = inputStream.read()) != -1) {
            swapStream.write(length);
        }
        return swapStream.toString();
    }
    //OutputStream 转String
    public static String parseToString(OutputStream outputStream)throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos = (ByteArrayOutputStream) outputStream;
        ByteArrayInputStream swapStream = new ByteArrayInputStream(baos.toByteArray());
        return swapStream.toString();
    }
    //String转inputStream
    public static ByteArrayInputStream parseToInputStream(String s)throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(s.getBytes());
        return inputStream;
    }
    //String 转outputStream
    public static ByteArrayOutputStream parseToOutputStream(String s)throws Exception {
        return parse(parseToInputStream(s));
    }
}