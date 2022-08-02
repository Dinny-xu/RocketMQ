package com.study;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication(scanBasePackages = "com.study")
@MapperScan(basePackages = "cn.sxw.pros.ai.iot.mq.biz.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class MqServerApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(MqServerApplication.class, args);
    }

}