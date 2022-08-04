package com.study.rocketmq.consumer;


import com.study.utils.RocketMqNameSrvAddr;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

@Slf4j
public class OnewayConsumer {


    public static void main(String[] args) throws MQClientException {
        // 定义一个push消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("xy");
        // 指定nameServer
        consumer.setNamesrvAddr(RocketMqNameSrvAddr.NAME_SERVER);
        // 指定从第一条消息开始消费
//        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        // 指定消费topic与tag
        consumer.subscribe("single", "someTag");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            // 逐条消费消息
            for (MessageExt msg : msgList) {
                log.info("异步消费消息: {},线程名称为: {},线程ID: {},QueueId为: {}",
                        new String(msg.getBody()), Thread.currentThread().getName(), Thread.currentThread().getId(), msg.getQueueId());
            }
            // 返回消费状态：消费成功
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        // 开启消费者消费
        consumer.start();
        System.out.println("Consumer Started");
    }

}
