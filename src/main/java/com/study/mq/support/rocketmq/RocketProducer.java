package com.study.mq.support.rocketmq;

import com.study.mq.ConnectionConfig;
import com.study.mq.ISerialize;
import com.study.mq.RocketMessage;
import com.study.mq.filter.EventModel;
import com.study.mq.filter.MessageFilter;
import com.study.mq.filter.TraceMessage;
import com.study.mq.producer.ResultCallback;
import com.study.mq.support.MqProducer;
import com.study.mq.utils.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OnsProducer
 *
 * @author lry
 */
@Slf4j
public class RocketProducer implements MqProducer {

    private MessageFilter filter;
    private ISerialize serialize;
    private ConnectionConfig config;
    private DefaultMQProducer producer;

    @Override
    public void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig) {
        log.info("Producer adapter initialize config:{}", connectionConfig);
        this.filter = filter;
        this.serialize = serialize;
        this.config = connectionConfig;
        if (!config.isEnable()) {
            return;
        }

        // authenticate
        RPCHook rpcHook = null;
        if (connectionConfig.getAccessKey() != null) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(connectionConfig.getAccessKey(),
                    connectionConfig.getSecretKey(), connectionConfig.getSecurityToken()));
        }

        // start producer
        try {
            producer = new DefaultMQProducer(config.getGroup(), rpcHook);
            producer.setMaxMessageSize(connectionConfig.getMaxMessageSize());
            producer.setNamesrvAddr(connectionConfig.getAddress());
            producer.setInstanceName(config.getGroup());
            if (!connectionConfig.getParameters().isEmpty()) {
                BeanUtils.copyMapToObj(connectionConfig.getParameters(), producer);
            }
            producer.start();
        } catch (Exception e) {
            throw new RuntimeException("Start producer exception", e);
        }
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
        String keys = StringUtils.isBlank(key) ? UUID.randomUUID().toString() : key;
        if (properties == null) {
            properties = new HashMap<>();
        }

        // copy context
        Map<String, String> mdcContext = (MDC.getCopyOfContextMap() == null) ? new HashMap<>() : MDC.getCopyOfContextMap();
        if (!mdcContext.containsKey(ConnectionConfig.TRACE_KEY)) {
            mdcContext.put(ConnectionConfig.TRACE_KEY, UUID.randomUUID().toString());
        }
        for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        // build message
        Message message = new Message();
        message.setTopic(topic);
        message.setTags(tags);
        message.setBody(body);
        message.setKeys(keys);
        message.putUserProperty(ConnectionConfig.TITLE_KEY, title);
        message.putUserProperty(ConnectionConfig.TRACE_MSG_KEY, String.valueOf(trace));
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            message.putUserProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }

        // send message
        try {
            if (config.isFilter() && filter != null) {
                filter.filter(trace, false, EventModel.build(config.getGroup(), topic, tags, keys,
                        message.getProperties(), title, "SENDING", "开始发送"));
            }
            producer.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    try {
                        MDC.setContextMap(mdcContext);
                        if (!trace) {
                            log.info("Send success: group={}, topic={}, tags={}, key={}", config.getGroup(), topic, tags, keys);
                        }
                        if (config.isFilter() && filter != null) {
                            filter.filter(trace, false, EventModel.build(config.getGroup(), topic, tags, keys,
                                    message.getProperties(), title, "SEND_SUCCESS", "发送成功"));
                        }
                        if (callback != null) {
                            callback.onSuccess(config.getGroup(), topic, tags, keys);
                        }
                    } finally {
                        MDC.clear();
                    }
                }

                @Override
                public void onException(Throwable e) {
                    try {
                        MDC.setContextMap(mdcContext);
                        if (!trace) {
                            log.error("Send failure: group={}, topic={}, tags={}, key={} {}", config.getGroup(), topic, tags, keys, e);
                        }
                        if (config.isFilter() && filter != null) {
                            filter.filter(trace, false, EventModel.build(config.getGroup(), topic, tags, keys,
                                    message.getProperties(), title, "SEND_FAILURE", e.getMessage()));
                        }
                        if (callback != null) {
                            callback.onException(config.getGroup(), topic, tags, keys, e);
                        }
                    } finally {
                        MDC.clear();
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Producer send exception", e);
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
