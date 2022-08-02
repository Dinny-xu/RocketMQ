package com.study.download;

import cn.hutool.http.HttpUtil;

import java.util.HashMap;

/**
 * @author dinny-xu
 */
public class BooksUrl {

    public static void main(String[] args) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("page_num", "1");
        map.put("page_size", "30");
        map.put("password", "");
        map.put("path", "/Drive2/书 籍/计算机语言资料/Java");

        String url = "https://p.lurenli.cn/api/public/path";
        String result = HttpUtil.post(url, map);
        System.out.println(result);
    }
}
