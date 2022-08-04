package com.study.rocketmq.consumer;

import com.study.utils.RocketMqNameSrvAddr;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;


/**
 * 顺序消费 1
 *
 * @author dinny-xu
 */
@Slf4j
public class ConsumerOrderTagOne {

    public static void main(String[] args) throws Exception {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("sync-1");
        consumer.setNamesrvAddr(RocketMqNameSrvAddr.NAME_SERVER);
        consumer.subscribe("myTopicA", "tag1");
        //consumer.subscribe("myTopic2", "tag2");
        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                log.info("顺序消费消息：{},线程名称为：{},线程ID：{},QueueId为：{}",
                        new String(msg.getBody()), Thread.currentThread().getName(), Thread.currentThread().getId(), msg.getQueueId());
            }
            return ConsumeOrderlyStatus.SUCCESS;
        });
        consumer.start();
        System.out.println("Consumer start...");
    }
}