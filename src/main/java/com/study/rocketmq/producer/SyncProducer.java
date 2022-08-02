package com.study.rocketmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步消息发送者
 *
 * @author dinny-xu
 */
@Slf4j
@Service
public class SyncProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("syncProducer1");
        producer.setNamesrvAddr("49.233.26.33:9876");
        producer.start();
        for (int i = 1; i < 10; i++) {
            Message message = new Message("myTopicA", "tag1", ("Hi 我是顺序发送!(tag1)" + i + " 时间:" + LocalDateTime.now()).getBytes());
            SendResult sendResult = producer.send(message, new MessageQueueSelector() {
                        @Override
                        public MessageQueue select(
                                // 当前topic 里面包含的所有queue
                                List<MessageQueue> mqs, Message msg,
                                // 对应到 send（） 里的 args，也就是2000前面的那个0
                                // 实际业务中可以把0换成实际业务系统的主键，比如订单号啥的，然后这里做hash进行选择queue等。能做的事情很多，我这里做演示就用第一个queue，所以不用arg。
                                Object arg) {
                            // 向固定的一个queue里写消息，比如这里就是向第一个queue里写消息
                            MessageQueue queue = mqs.get(0);
                            // 选好的queue
                            return queue;
                        }
                    },
                    // 自定义参数：0
                    // 2000代表2000毫秒超时时间
                    0, 2000);
            log.info("线程名称为：{},线程ID：{},发送结果：{}",
                    Thread.currentThread().getName(), Thread.currentThread().getId(), sendResult);
        }
    }
}