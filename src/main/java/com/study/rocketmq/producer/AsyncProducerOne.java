package com.study.rocketmq.producer;

import com.study.utils.RocketMqNameSrvAddr;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 异步发送生产者
 *
 * @author dinny-xu
 */
@Slf4j
public class AsyncProducerOne {

    @SneakyThrows
    public static void main(String[] args) {

        DefaultMQProducer producer = new DefaultMQProducer("sync-2");
        producer.setNamesrvAddr(RocketMqNameSrvAddr.NAME_SERVER);
        // 指定异步发送失败后不进行重试发送
        producer.setRetryTimesWhenSendAsyncFailed(0);
        // 指定新创建的Topic的Queue数量为2 默认为4
        producer.setDefaultTopicQueueNums(2);
        producer.start();
        for (int i = 1; i < 10; i++) {
            byte[] body = ("Hi 我是异步发送," + LocalDateTime.now() + i).getBytes(StandardCharsets.UTF_8);

            try {
                Message msg = new Message("myTopicB", "tag1", body);
                log.info("异步发送消息：{},线程名称为：{},线程ID：{}",
                        new String(msg.getBody()), Thread.currentThread().getName(), Thread.currentThread().getId());
                // 异步发送 指定回调
                producer.send(msg, new SendCallback() {

                    // 当producer接收到MQ发送来的ACK后就会触发该回调方法的执行
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        System.out.println(sendResult);
                    }

                    @Override
                    public void onException(Throwable e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 由于采用的是异步发送，所以若这里不sleep
        // 则消息还未发送完就会将producer给关闭，报错
        TimeUnit.SECONDS.sleep(3);
        producer.shutdown();
    }
}
