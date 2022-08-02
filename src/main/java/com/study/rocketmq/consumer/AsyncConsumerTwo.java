package com.study.rocketmq.consumer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * @author dinny-xu
 */
@Slf4j
public class AsyncConsumerTwo {

    @SneakyThrows
    public static void main(String[] args) {

        // 定义一个pull消费者
        // DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("syncSend");
        // 定义一个push消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("syncSend2");
        // 指定nameServer
        consumer.setNamesrvAddr("49.233.26.33:9876");
        // 指定从第一条消息开始消费
//        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        // 指定消费topic与tag
        consumer.subscribe("myTopic1", "tag2");
        // 指定采用"广播模式"进行消费，默认为"集群模式"
//        consumer.setMessageModel(MessageModel.BROADCASTING);

//        consumer.setAdjustThreadPoolNumsThreshold(1);
//        consumer.setConsumeThreadMax(1);
//        consumer.setConsumeThreadMin(1);// 最小线程消费数

        // 注册消息监听器(并发消费)
        // 一旦broker中有了其订阅的消息就会触发该方法的执行，
        // 其返回值为当前consumer消费的状态

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
