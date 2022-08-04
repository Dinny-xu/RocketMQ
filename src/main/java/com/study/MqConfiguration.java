package com.study;


import com.alibaba.fastjson.JSON;
import com.study.onsmq.ConnectionConfig;
import com.study.onsmq.RocketMessage;
import com.study.onsmq.filter.EventModel;
import com.study.onsmq.filter.MessageFilter;
import com.study.onsmq.spring.AiMqConfiguration;
import com.study.onsmq.spring.SpringMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * MqConfiguration
 *
 * @author lry
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SpringMqProperties.class)
@ConditionalOnProperty(name = "rocket-mq.enable", havingValue = "true")
public class MqConfiguration implements ApplicationContextAware, DisposableBean, MessageFilter {

    public static AiMqConfiguration defaultAiMq;

    @Autowired
    private SpringMqProperties springMqProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // default local mq
        defaultAiMq = new AiMqConfiguration();
        defaultAiMq.initialize(ConnectionConfig.DEFAULT_CONNECT_GROUP, springMqProperties, applicationContext, this);
    }

    public static void defaultSend(String key, Object message) {
        try {
            Map<String, Object> properties = new HashMap<>();
            defaultAiMq.getProducer().send(null, key, message, properties, null);
        } catch (Exception e) {
            RocketMessage rocketMessage = message.getClass().getAnnotation(RocketMessage.class);
            log.error("mq平台[{}]发送MQ异常:{}", rocketMessage.title(), JSON.toJSONString(message), e);
        }
    }

    public static void defaultOrderlySend(String key, Object message) {
        try {
            Map<String, Object> properties = new HashMap<>();
            defaultAiMq.getOnsOrderlyProducer().send(null, key, message, properties, null);
        } catch (Exception e) {
            RocketMessage rocketMessage = message.getClass().getAnnotation(RocketMessage.class);
            log.error("mq平台[{}]发送MQ异常:{}", rocketMessage.title(), JSON.toJSONString(message), e);
        }
    }

    @Override
    public void filter(boolean trace, boolean consumer, EventModel eventModel) {

    }

    @Override
    public void destroy() throws Exception {
        if (defaultAiMq != null) {
            defaultAiMq.destroy();
        }
    }

}
