package com.study.mq.spring;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.mq.ConnectionConfig;
import com.study.mq.ISerialize;
import com.study.mq.consumer.MessageListener;
import com.study.mq.consumer.OrderMessageListener;
import com.study.mq.filter.MessageFilter;
import com.study.mq.support.MqConsumer;
import com.study.mq.support.MqOrderConsumer;
import com.study.mq.support.MqOrderProducer;
import com.study.mq.support.MqProducer;
import com.study.mq.support.ons.OnsConsumer;
import com.study.mq.support.ons.OnsOrderlyConsumer;
import com.study.mq.support.ons.OnsOrderlyProducer;
import com.study.mq.support.ons.OnsProducer;
import com.study.mq.support.rocketmq.RocketConsumer;
import com.study.mq.support.rocketmq.RocketProducer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AiMqConfiguration
 *
 * @author lry
 */
@Slf4j
@Getter
public class AiMqConfiguration implements ISerialize {

    private MqProducer producer;
    private MqConsumer consumer;
    private ObjectMapper objectMapper;
    private MqOrderProducer onsOrderlyProducer;
    private MqOrderConsumer onsOrderlyConsumer;

    /**
     * The initialize
     *
     * @param key                key
     * @param springMqProperties {@link SpringMqProperties}
     * @param applicationContext {@link ApplicationContext}
     * @param messageFilter      {@link MessageFilter}
     */
    public void initialize(String key, SpringMqProperties springMqProperties, ApplicationContext applicationContext, MessageFilter messageFilter) {
        this.objectMapper = applicationContext.getBean(ObjectMapper.class);
        if ((!springMqProperties.isEnable()) || springMqProperties.getConnections().isEmpty()) {
            return;
        }

        // create connection config
        ConnectionConfig connectionConfig = springMqProperties.select(key);
        if (StringUtils.isNotBlank(connectionConfig.getTenant())) {
            connectionConfig.setGroup(connectionConfig.getGroup() + "_" + connectionConfig.getTenant());
        }

        log.info("Initialize connection[{}] config:{}", key, JSON.toJSONString(connectionConfig));
        List<MessageListener> messageListeners = selectMessageListener(connectionConfig, applicationContext);
        List<OrderMessageListener> orderMessageListeners = selectOrderMessageListener(connectionConfig, applicationContext);

        // initialize producer
        if (ConnectionConfig.ONS_TYPE.equals(connectionConfig.getType()) || ConnectionConfig.ONS_ORDERLY_TYPE.equals(
                connectionConfig.getType())) {
            this.producer = new OnsProducer();
            this.onsOrderlyProducer = new OnsOrderlyProducer();
        } else if (ConnectionConfig.ROCKET_TYPE.equals(connectionConfig.getType())) {
            this.producer = new RocketProducer();
        }
        producer.initialize(messageFilter, this, connectionConfig);
        onsOrderlyProducer.initialize(messageFilter, this, connectionConfig);

        // initialize consumer
        if (ConnectionConfig.ONS_TYPE.equals(connectionConfig.getType())) {
            this.consumer = new OnsConsumer();
            this.onsOrderlyConsumer = new OnsOrderlyConsumer();
        } else if (ConnectionConfig.ROCKET_TYPE.equals(connectionConfig.getType())) {
            this.consumer = new RocketConsumer();
        } else if (ConnectionConfig.ONS_ORDERLY_TYPE.equals(connectionConfig.getType())) {
            this.onsOrderlyConsumer = new OnsOrderlyConsumer();
        }
        consumer.initialize(messageFilter, this, connectionConfig, messageListeners);
        onsOrderlyConsumer.initialize(messageFilter, this, connectionConfig, orderMessageListeners);
    }

    @Override
    public byte[] serialize(Object object) {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (Exception e) {
            throw new RuntimeException("Serialize exception", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) {
        try {
            return objectMapper.readValue(bytes, clz);
        } catch (Exception e) {
            throw new RuntimeException("Deserialize exception", e);
        }
    }

    /**
     * The destroy
     */
    public void destroy() {
        try {
            if (producer != null) {
                producer.destroy();
            }
         /*   if (onsOrderlyProducer != null) {
                onsOrderlyProducer.destroy();
            }*/
            if (consumer != null) {
                consumer.destroy();
            }
      /*      if (onsOrderlyConsumer != null) {
                onsOrderlyConsumer.destroy();
            }*/
        } catch (Exception e) {
            log.error("Producer or consumer destroy exception", e);
        }
    }

    /**
     * The select message listener
     *
     * @param connectionConfig   {@link ConnectionConfig}
     * @param applicationContext {@link ApplicationContext}
     * @return {@link List<MessageListener>}
     */
    private List<MessageListener> selectMessageListener(ConnectionConfig connectionConfig, ApplicationContext applicationContext) {
        List<MessageListener> messageListeners = new ArrayList<>();
        Map<String, MessageListener> map = applicationContext.getBeansOfType(MessageListener.class);
        for (Map.Entry<String, MessageListener> entry : map.entrySet()) {
            Type[] types = entry.getValue().getClass().getGenericInterfaces();
            if (types.length > 0) {
                if (types[0] instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) types[0]).getActualTypeArguments();
                    if (args[0] instanceof Class) {
                        for (String listenerPackage : connectionConfig.getListenerPackages()) {
                            String packageName = entry.getValue().getClass().getPackage().getName();
                            if (packageName.startsWith(listenerPackage)) {
                                messageListeners.add(entry.getValue());
                            }
                        }
                    }
                }
            }
        }

        return messageListeners;
    }


    private List<OrderMessageListener> selectOrderMessageListener(ConnectionConfig connectionConfig, ApplicationContext applicationContext) {
        List<OrderMessageListener> messageListeners = new ArrayList<>();
        Map<String, OrderMessageListener> map = applicationContext.getBeansOfType(OrderMessageListener.class);
        for (Map.Entry<String, OrderMessageListener> entry : map.entrySet()) {
            Type[] types = entry.getValue().getClass().getGenericInterfaces();
            if (types.length > 0) {
                if (types[0] instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) types[0]).getActualTypeArguments();
                    if (args[0] instanceof Class) {
                        for (String listenerPackage : connectionConfig.getListenerPackages()) {
                            String packageName = entry.getValue().getClass().getPackage().getName();
                            if (packageName.startsWith(listenerPackage)) {
                                messageListeners.add(entry.getValue());
                            }
                        }
                    }
                }
            }
        }

        return messageListeners;
    }

}
