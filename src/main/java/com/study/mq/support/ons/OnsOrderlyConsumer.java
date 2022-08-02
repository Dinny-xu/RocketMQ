package com.study.mq.support.ons;


import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.order.OrderAction;
import com.aliyun.openservices.ons.api.order.OrderConsumer;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageExt;
import com.study.mq.ConnectionConfig;
import com.study.mq.ISerialize;
import com.study.mq.RocketMessage;
import com.study.mq.consumer.MessageInvoker;
import com.study.mq.consumer.OrderMessageInvoker;
import com.study.mq.consumer.OrderMessageListener;
import com.study.mq.filter.EventModel;
import com.study.mq.filter.MessageFilter;
import com.study.mq.support.MqOrderConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OnsOrderlyConsumer
 *
 * @author xuming
 */
@Slf4j
public class OnsOrderlyConsumer implements MqOrderConsumer {

    private OrderConsumer orderConsumer;
    private MessageFilter filter;
    private ISerialize serialize;
    private ConnectionConfig config;


    @Override
    public void initialize(MessageFilter filter, ISerialize serialize,
                           ConnectionConfig connectionConfig, List<OrderMessageListener> messageListeners) {
        log.info("OrderConsumer adapter initialize config:{}", connectionConfig);
        this.filter = filter;
        this.serialize = serialize;
        this.config = connectionConfig;
        if (!connectionConfig.isEnable() || messageListeners.isEmpty()) {
            return;
        }

        // calculation rules: key=topic, subKey=tags
        Map<String, Map<String, OrderMessageInvoker>> mapping = new ConcurrentHashMap<>();
        for (OrderMessageListener messageListener : messageListeners) {
            // todo  OrderMessageInvoker
            RocketMessage rocketMessage = OrderMessageInvoker.getParameterizedType
                    (messageListener).getDeclaredAnnotation(RocketMessage.class);

            String tags;
            if (StringUtils.isNotBlank(config.getTenant())) {
                tags = String.format(ConnectionConfig.TENANT_RULE, rocketMessage.tags(), config.getTenant());
            } else {
                tags = rocketMessage.tags();
            }

            mapping.computeIfAbsent(rocketMessage.topic(), e ->
                    new ConcurrentHashMap<>()).put(tags, new OrderMessageInvoker<>(messageListener));
        }

        // setter properties
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.GROUP_ID, connectionConfig.getGroup());
        if (StringUtils.isNotBlank(connectionConfig.getAccessKey())) {
            properties.setProperty(PropertyKeyConst.AccessKey, connectionConfig.getAccessKey());
        }
        if (StringUtils.isNotBlank(connectionConfig.getSecretKey())) {
            properties.setProperty(PropertyKeyConst.SecretKey, connectionConfig.getSecretKey());
        }
        properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, connectionConfig.getAddress());
        properties.setProperty(PropertyKeyConst.MessageModel, connectionConfig.getModel());
        for (Map.Entry<String, Object> parameters : config.getParameters().entrySet()) {
            properties.setProperty(parameters.getKey(), String.valueOf(parameters.getValue()));
        }

        // create consumer
        this.orderConsumer = ONSFactory.createOrderedConsumer(properties);

        for (Map.Entry<String, Map<String, OrderMessageInvoker>> tempEntry : mapping.entrySet()) {
            String subExpression = String.join("||", tempEntry.getValue().keySet());
            log.info("OrderConsumer subscribe rule: topic={}, subExpression={}", tempEntry.getKey(), subExpression);
            orderConsumer.subscribe(tempEntry.getKey(), subExpression, (message, context) -> doSubscribe(message, mapping));
        }
        orderConsumer.start();
    }

    /**
     * The do subscribe
     *
     * @param message {@link MessageExt}
     * @param mapping key=topic, subKey=tags, value=listener
     * @return {@link Action}
     */
    private OrderAction doSubscribe(Message message, Map<String, Map<String, OrderMessageInvoker>> mapping) {
        String title = message.getUserProperties(ConnectionConfig.TITLE_KEY);
        boolean traceMsg;
        try {
            traceMsg = Boolean.parseBoolean(message.getUserProperties(ConnectionConfig.TRACE_MSG_KEY));
        } catch (Exception e) {
            traceMsg = false;
        }

        Map<String, String> userProperties = new HashMap<>();
        for (String propertyName : message.getUserProperties().stringPropertyNames()) {
            userProperties.put(propertyName, message.getUserProperties(propertyName));
        }

        try {
            // get and put context
            MDC.setContextMap(userProperties);

            // consumer processor
            if (config.isFilter() && filter != null) {
                filter.filter(traceMsg, true, EventModel.build(config.getGroup(), message.getTopic(),
                        message.getTag(), message.getKey(), userProperties, title, "CONSUMING", "开始消费"));
            }
            doProcessor(message, mapping);
            return OrderAction.Success;
        } catch (Throwable e) {
            log.error("OrderConsume processor exception: " + toMessage(message), e);
            if (config.isFilter() && filter != null) {
                filter.filter(traceMsg, true, EventModel.build(config.getGroup(), message.getTopic(),
                        message.getTag(), message.getKey(), userProperties, title, "CONSUME_FAILURE", e.getMessage()));
            }
            return OrderAction.Suspend;
        } finally {
            MDC.clear();
        }
    }

    /**
     * The destroy
     */
    @Override
    public void destroy() {
        try {
            orderConsumer.shutdown();
        } catch (Exception e) {
            log.error("Close orderConsumer[{}] exception", config.getGroup(), e);
        }
    }

    /**
     * Consumer processing
     *
     * @param message {@link Message}
     * @param mapping {@link MessageInvoker} mapping
     */
    @SuppressWarnings("unchecked")
    private void doProcessor(Message message, Map<String, Map<String, OrderMessageInvoker>> mapping) {
        log.info("顺序 Receive consume:{}", toMessage(message));

        // control the number of retries
        if (config.getMaxReconsumeTimes() == 0 && message.getReconsumeTimes() > 0) {
            log.warn("Retry not allowed: times={}, msg={}", message.getReconsumeTimes(), toMessage(message));
            return;
        } else if (config.getMaxReconsumeTimes() > 0 && message.getReconsumeTimes() > config.getMaxReconsumeTimes()) {
            log.warn("Maximum number of retries exceeded: times={}, msg={}", message.getReconsumeTimes(), toMessage(message));
            return;
        }

        // select invoker by topic
        Map<String, OrderMessageInvoker> invokerMap = mapping.get(message.getTopic());
        if (invokerMap != null) {
            OrderMessageInvoker invoker = invokerMap.get(message.getTag());
            if (invoker != null) {
                Object object;
                if (Objects.equals(invoker.getMessageClass(), Message.class)) {
                    object = message;
                } else if (Objects.equals(invoker.getMessageClass(), byte[].class)) {
                    object = message.getBody();
                } else if (Objects.equals(invoker.getMessageClass(), String.class)) {
                    object = new String(message.getBody(), StandardCharsets.UTF_8);
                } else {
                    object = serialize.deserialize(message.getBody(), invoker.getMessageClass());
                }
                if (config.isDebug()) {
                    log.info("Receive body: {}", JSON.toJSONString(object));
                }

                // notify message
                invoker.getOrderMessageListener().onMessage(config.getGroup(), object);
                return;
            }
        }

        log.warn("OrderConsume not found mapping: {}", toMessage(message));
    }


    /**
     * The build message info
     *
     * @param message {@link Message}
     * @return message info
     */
    private String toMessage(Message message) {
        return String.format("group=%s, topic=%s, tags=%s, keys=%s, reconsumeTimes=%s",
                config.getGroup(), message.getTopic(), message.getTag(), message.getKey(), message.getReconsumeTimes());
    }


}