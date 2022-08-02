package com.study.mq.support.ons;


import com.alibaba.fastjson.JSON;
import com.alibaba.ons.open.trace.core.common.OnsTraceConstants;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.OnExceptionContext;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.SendCallback;
import com.aliyun.openservices.ons.api.SendResult;
import com.study.mq.ConnectionConfig;
import com.study.mq.ISerialize;
import com.study.mq.RocketMessage;
import com.study.mq.filter.EventModel;
import com.study.mq.filter.MessageFilter;
import com.study.mq.filter.TraceMessage;
import com.study.mq.producer.ResultCallback;
import com.study.mq.support.MqProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * OnsProducer
 *
 * @author lry
 */
@Slf4j
public class OnsProducer implements MqProducer {

    private Producer producer;
    private MessageFilter filter;
    private ISerialize serialize;
    private ConnectionConfig config;

    @Override
    public void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig) {
        log.info("Producer adapter initialize config:{}", connectionConfig);
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

        this.producer = ONSFactory.createProducer(properties);
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

        producer.sendAsync(message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                try {
                    MDC.setContextMap(mdcContext);
                    if (!trace) {
                        log.info("Send success: group={}, topic={}, tags={}, key={}", config.getGroup(), topic, tags, keys);
                    }
                    if (config.isDebug()) {
                        log.info("Send body： {}", JSON.toJSONString(obj));
                    }

                    if (config.isFilter() && filter != null) {
                        filter.filter(trace, false, EventModel.build(config.getGroup(), topic, tags, keys,
                                attachments, title, "SEND_SUCCESS", "发送成功"));
                    }
                    if (callback != null) {
                        callback.onSuccess(config.getGroup(), topic, tags, message.getKey());
                    }
                } finally {
                    MDC.clear();
                }
            }

            @Override
            public void onException(OnExceptionContext e) {
                try {
                    MDC.setContextMap(mdcContext);
                    if (!trace) {
                        log.error("Send exception: group={}, topic={}, tags={}, key={} {}", config.getGroup(), topic, tags, keys, e.getException());
                    }
                    if (config.isFilter() && filter != null) {
                        filter.filter(trace, false, EventModel.build(config.getGroup(), topic, tags, keys,
                                attachments, title, "SEND_FAILURE", e.getException().getMessage()));
                    }
                    if (callback != null) {
                        callback.onException(config.getGroup(), topic, tags, message.getKey(), e.getException());
                    }
                } finally {
                    MDC.clear();
                }
            }
        });
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
