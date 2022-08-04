package com.study.rocketmq.producer;

import com.study.utils.RocketMqNameSrvAddr;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

/**
 * @author dinny-xu
 */
public class OnewayProducer {

    @SneakyThrows
    public static void main(String[] args) {

        DefaultMQProducer producer = new DefaultMQProducer("xy");
//        producer.setNamesrvAddr("49.233.26.33:9876");
        producer.setNamesrvAddr(RocketMqNameSrvAddr.NAME_SERVER);
        producer.start();
        byte[] body = ("测试消息2").getBytes();
        Message msg = new Message("single", "someTag", body);
        // 单向发送
        producer.sendOneway(msg);
        producer.shutdown();
        System.out.println("producer shutdown");
    }
}
