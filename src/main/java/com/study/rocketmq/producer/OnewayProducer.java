package com.study.rocketmq.producer;

import lombok.SneakyThrows;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

/**
 * @author dinny-xu
 */
public class OnewayProducer {

    @SneakyThrows
    public static void main(String[] args) {

        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("49.233.26.33:9876");
        producer.start();
        for (int i = 0; i < 10; i++) {
            byte[] body = ("测试消息," + i).getBytes();
            Message msg = new Message("single", "someTag", body);
            // 单向发送
            producer.sendOneway(msg);
        }
        producer.shutdown();
        System.out.println("producer shutdown");
    }
}
