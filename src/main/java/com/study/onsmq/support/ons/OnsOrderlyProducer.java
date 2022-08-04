package com.study.onsmq.support.ons;


import com.alibaba.ons.open.trace.core.common.OnsTraceConstants;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.study.onsmq.ConnectionConfig;
import com.study.onsmq.ISerialize;
import com.study.onsmq.RocketMessage;
import com.study.onsmq.filter.MessageFilter;
import com.study.onsmq.filter.TraceMessage;
import com.study.onsmq.producer.ResultCallback;
import com.study.onsmq.support.MqOrderProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * @author dinny-xu
 */
@Slf4j
public class OnsOrderlyProducer implements MqOrderProducer {

    private OrderProducer producer;
    private MessageFilter filter;
    private ISerialize serialize;
    private ConnectionConfig config;

    @Override
    public void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig) {
        log.info("OrderProducer adapter initialize config:{}", connectionConfig);
        this.filter = filter;
        this.serialize = serialize;
        this.config = connectionConfig;
        if (!config.isEnable()) {
            return;
        }
        // start
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.GROUP_ID, config.getGroup());
        if (StringUtils.isNotBlank(connectionConfig.getAccessKey())) {
            properties.setProperty(PropertyKeyConst.AccessKey, connectionConfig.getAccessKey());
        }
        if (StringUtils.isNotBlank(connectionConfig.getSecretKey())) {
            properties.setProperty(PropertyKeyConst.SecretKey, connectionConfig.getSecretKey());
        }
        properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, config.getAddress());
        properties.setProperty(OnsTraceConstants.MaxMsgSize, String.valueOf(config.getMaxMessageSize()));
        for (Map.Entry<String, Object> parameters : config.getParameters().entrySet()) {
            properties.setProperty(parameters.getKey(), String.valueOf(parameters.getValue()));
        }
        this.producer = ONSFactory.createOrderProducer(properties);
        producer.start();
    }

    @Override
    public void send(String tenant, String key, Object obj, Map<String, Object> properties, ResultCallback callback) {
        RocketMessage msg = obj.getClass().getDeclaredAnnotation(RocketMessage.class);
        if (msg == null) {
            throw new RuntimeException("Message must contain @RocketMessage annotation");
        }
        String tags;
        if (StringUtils.isNotBlank(tenant)) {
            tags = String.format(ConnectionConfig.TENANT_RULE, msg.tags(), tenant);
        } else {
            tags = msg.tags();
        }
        send(msg.title(), msg.topic(), tags, key, obj, properties, callback);
    }

    @Override
    public void send(String title, String topic, String tags, String key, Object obj, Map<String, Object> properties, ResultCallback callback) {
        // serialize object
        byte[] body = serialize.serialize(obj);
        boolean trace = obj instanceof TraceMessage;
        String keys = StringUtils.isBlank(key) ? UUID.randomUUID().toString().replace("-", "") : key;
        if (properties == null) {
            properties = new HashMap<>();
        }

        // copy context
        Map<String, String> mdcContext = (MDC.getCopyOfContextMap() == null) ? new HashMap<>() : MDC.getCopyOfContextMap();
        if (!mdcContext.containsKey(ConnectionConfig.TRACE_KEY)) {
            mdcContext.put(ConnectionConfig.TRACE_KEY, UUID.randomUUID().toString().replace("-", ""));
        }
        for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        // build message
        Message message = new Message();
        message.setTopic(topic);
        message.setTag(tags);
        message.setBody(body);
        message.setKey(keys);
        message.putUserProperties(ConnectionConfig.TITLE_KEY, title);
        message.putUserProperties(ConnectionConfig.TRACE_MSG_KEY, String.valueOf(trace));
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            message.putUserProperties(entry.getKey(), String.valueOf(entry.getValue()));
        }
        // 支持延迟消息
        if (properties.containsKey(ConnectionConfig.DELIVER_TIME_KEY)) {
            message.setStartDeliverTime(System.currentTimeMillis() + (long) properties.get(ConnectionConfig.DELIVER_TIME_KEY));
        }

        Map<String, String> attachments = new HashMap<>();
        for (String propertyName : message.getUserProperties().stringPropertyNames()) {
            attachments.put(propertyName, message.getUserProperties(propertyName));
        }
        try {
            SendResult sendResult = producer.send(message, key);
            if (sendResult != null) {
                log.info("SendTime:{}, Send mq message success. Group={}, Topic={}, tags={}, key={}, msgId={}",
                        LocalDateTime.now(), config.getGroup(), message.getTopic(), message.getTag(), key, sendResult.getMessageId());
            }
        } catch (Exception e) {
            // 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。
            log.error("Send exception: group={}, topic={}, tags={}, key={} 异常消息: {}", config.getGroup(), topic, tags, keys, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (producer != null) {
            try {
                producer.shutdown();
            } catch (Exception e) {
                log.error("Close producer exception", e);
            }
        }
    }
}