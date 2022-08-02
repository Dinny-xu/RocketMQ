package com.study.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 顺序消费 2
 *
 * @author dinny-xu
 */
@Slf4j
@Service
public class ConsumerOrderTagTwo {

    public static void main(String[] args) throws Exception {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumerOrderTwo");
        consumer.setNamesrvAddr("49.233.26.33:9876");
        consumer.subscribe("myTopicA", "tag2");
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    log.info("顺序消费消息：{},线程名称为：{},线程ID：{},QueueId为：{}",
                            new String(msg.getBody()), Thread.currentThread().getName(), Thread.currentThread().getId(), msg.getQueueId());
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        consumer.start();
        System.out.println("Consumer start...");
    }
}