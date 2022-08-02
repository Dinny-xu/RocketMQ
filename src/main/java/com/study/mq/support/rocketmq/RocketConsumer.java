package com.study.mq.support.rocketmq;

import com.study.mq.ConnectionConfig;
import com.study.mq.ISerialize;
import com.study.mq.RocketMessage;
import com.study.mq.consumer.MessageInvoker;
import com.study.mq.consumer.MessageListener;
import com.study.mq.filter.EventModel;
import com.study.mq.filter.MessageFilter;
import com.study.mq.support.MqConsumer;
import com.study.mq.utils.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OnsConsumer
 *
 * @author lry
 */
@Slf4j
public class RocketConsumer implements MessageListenerConcurrently, MqConsumer {

    private MessageFilter filter;
    private ISerialize serialize;
    private ConnectionConfig config;
    private DefaultMQPushConsumer consumer;
    private Map<String, Map<String, MessageInvoker>> mapping = new ConcurrentHashMap<>();

    @Override
    public void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig, List<MessageListener> messageListeners) {
        log.info("Consumer adapter initialize config:{}", connectionConfig);
        this.filter = filter;
        this.serialize = serialize;
        this.config = connectionConfig;
        if (!connectionConfig.isEnable() || messageListeners.isEmpty()) {
            return;
        }

        // calculation rules: key=topic, subKey=tags
        for (MessageListener messageListener : messageListeners) {
            RocketMessage rocketMessage = MessageInvoker.getParameterizedType
                    (messageListener).getDeclaredAnnotation(RocketMessage.class);

            String tags;
            if (StringUtils.isNotBlank(config.getTenant())) {
                tags = String.format(ConnectionConfig.TENANT_RULE, rocketMessage.tags(), config.getTenant());
            } else {
                tags = rocketMessage.tags();
            }

            mapping.computeIfAbsent(rocketMessage.topic(), e ->
                    new ConcurrentHashMap<>()).put(tags, new MessageInvoker<>(messageListener));
        }

        // authenticate
        RPCHook rpcHook = null;
        if (connectionConfig.getAccessKey() != null) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(connectionConfig.getAccessKey(),
                    connectionConfig.getSecretKey(), connectionConfig.getSecurityToken()));

        }
        // create consumer
        try {
            this.consumer = startPushConsumer(rpcHook);
        } catch (Exception e) {
            throw new RuntimeException("Start push consumer exception", e);
        }
    }

    /**
     * The start push consumer
     *
     * @param rpcHook {@link RPCHook}
     * @return {@link DefaultMQPushConsumer}
     * @throws Exception exception
     */
    private DefaultMQPushConsumer startPushConsumer(RPCHook rpcHook) throws Exception {
        // create consumers
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(config.getGroup(), rpcHook, new AllocateMessageQueueAveragely());
        consumer.setInstanceName(config.getGroup());
        consumer.setNamesrvAddr(config.getAddress());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.setMessageModel(MessageModel.valueOf(config.getModel()));

        // setter custom configuration
        if (!config.getParameters().isEmpty()) {
            BeanUtils.copyMapToObj(config.getParameters(), consumer);
        }
        consumer.registerMessageListener(this);
        for (Map.Entry<String, Map<String, MessageInvoker>> tempEntry : mapping.entrySet()) {
            String subExpression = String.join("||", tempEntry.getValue().keySet());
            consumer.subscribe(tempEntry.getKey(), subExpression);
            log.info("Consumer subscribe rule: topic={}, subExpression={}", tempEntry.getKey(), subExpression);
        }
        consumer.start();
        return consumer;
    }


    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt messageExt : msgs) {
            ConsumeConcurrentlyStatus status = doSubscribe(messageExt);
            if (ConsumeConcurrentlyStatus.RECONSUME_LATER == status) {
                return status;
            }
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    /**
     * The do subscribe
     *
     * @param message {@link MessageExt}
     * @return {@link ConsumeConcurrentlyStatus}
     */
    private ConsumeConcurrentlyStatus doSubscribe(MessageExt message) {
        String title = message.getUserProperty(ConnectionConfig.TITLE_KEY);
        boolean traceMsg;
        try {
            traceMsg = Boolean.parseBoolean(message.getUserProperty(ConnectionConfig.TRACE_MSG_KEY));
        } catch (Exception e) {
            traceMsg = false;
        }

        try {
            // get and put context
            MDC.setContextMap(message.getProperties());

            // consumer processor
            if (config.isFilter() && filter != null) {
                filter.filter(traceMsg, true, EventModel.build(config.getGroup(), message.getTopic(),
                        message.getTags(), message.getKeys(), message.getProperties(), title, "CONSUMING", "开始消费"));
            }
            doProcessor(traceMsg, message);
            if (config.isFilter() && filter != null) {
                filter.filter(traceMsg, true, EventModel.build(config.getGroup(), message.getTopic(),
                        message.getTags(), message.getKeys(), message.getProperties(), title, "CONSUME_SUCCESS", "消费成功"));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Throwable e) {
            log.error("Consume processor exception: " + toMessage(message), e);
            if (config.isFilter() && filter != null) {
                filter.filter(traceMsg, true, EventModel.build(config.getGroup(), message.getTopic(),
                        message.getTags(), message.getKeys(), message.getProperties(), title, "CONSUME_FAILURE", e.getMessage()));
            }
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
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
            consumer.shutdown();
        } catch (Exception e) {
            log.error("Close consumer[{}] exception", config.getGroup(), e);
        }
    }

    /**
     * Consumer processing
     *
     * @param traceMsg traceMsg
     * @param message  {@link MessageExt}
     */
    @SuppressWarnings("unchecked")
    private void doProcessor(boolean traceMsg, MessageExt message) {
        if (!traceMsg) {
            log.info("Receive consume:{}", toMessage(message));
        }

        // control the number of retries
        if (config.getMaxReconsumeTimes() == 0 && message.getReconsumeTimes() > 0) {
            log.warn("Retry not allowed: times={}, msg={}", message.getReconsumeTimes(), toMessage(message));
            return;
        } else if (config.getMaxReconsumeTimes() > 0 && message.getReconsumeTimes() > config.getMaxReconsumeTimes()) {
            log.warn("Maximum number of retries exceeded: times={}, msg={}", message.getReconsumeTimes(), toMessage(message));
            return;
        }

        // select invoker by topic
        Map<String, MessageInvoker> invokerMap = mapping.get(message.getTopic());
        if (invokerMap != null) {
            MessageInvoker invoker = invokerMap.get(message.getTags());
            if (invoker != null) {
                Object object;
                if (Objects.equals(invoker.getMessageClass(), MessageExt.class)) {
                    object = message;
                } else if (Objects.equals(invoker.getMessageClass(), byte[].class)) {
                    object = message.getBody();
                } else if (Objects.equals(invoker.getMessageClass(), String.class)) {
                    object = new String(message.getBody(), StandardCharsets.UTF_8);
                } else {
                    object = serialize.deserialize(message.getBody(), invoker.getMessageClass());
                }

                // notify message
                invoker.getListener().onMessage(config.getGroup(), object);
                return;
            }
        }

        log.warn("Consume not found mapping: {}", toMessage(message));
    }

    /**
     * The build message info
     *
     * @param message {@link MessageExt}
     * @return message info
     */
    private String toMessage(MessageExt message) {
        return String.format("group=%s, topic=%s, tags=%s, keys=%s, reconsumeTimes=%s",
                config.getGroup(), message.getTopic(), message.getTags(), message.getKeys(), message.getReconsumeTimes());
    }

}
