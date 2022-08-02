package com.study.download;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author dinny-xu
 */
public class BooksDownload {

    @SneakyThrows
    public static void main(String[] args) {

        URL url = new URL("https://bj29.cn-beijing.data.alicloudccp.com/5fb640d1dec45914a9b542b18ea7215fa578063b%2F5fb640d1bbd2736fe6ec41e0916f593d1153dea0?di=bj29&dr=123739&f=613b826e85073789ad6544c785af96182d3b5f56&u=741d61bd09c1481ab3b9b47da8bae17c&x-oss-access-key-id=LTAIsE5mAn2F493Q&x-oss-expires=1644314305&x-oss-signature=l%2F0cfsf4UDgpFcgzScAt13o1xXAH9%2BfnNZN7XjFbSUk%3D&x-oss-signature-version=OSS2");
        URLConnection urlConnection = url.openConnection();

        InputStream inputStream = urlConnection.getInputStream();
        byte[] getData = readInputStream(inputStream);

        File saveDir = new File("/Users/dinny-xu/Desktop/bookDownload");
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
        File file = new File(saveDir + File.separator + "603162 Java Web开发实例大全 提高卷.pdf");
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(getData);
        inputStream.close();
        System.out.println("info:" + url + "download success");

    }


    private static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        byteArrayOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }
}
