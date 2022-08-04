package com.study.onsmq.support;


import com.study.onsmq.ConnectionConfig;
import com.study.onsmq.ISerialize;
import com.study.onsmq.filter.MessageFilter;
import com.study.onsmq.producer.ResultCallback;

import java.util.Map;

/**
 * MqProducer
 *
 * @author lry
 */
public interface MqProducer {

    /**
     * Initialize the message provider
     *
     * @param filter           {@link MessageFilter}
     * @param serialize        {@link ISerialize}
     * @param connectionConfig {@link ConnectionConfig}
     */
    void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig);

    /**
     * The send message
     *
     * @param tenant     tenant
     * @param key        key
     * @param obj        object body
     * @param properties properties
     * @param callback   {@link ResultCallback}
     */
    void send(String tenant, String key, Object obj, Map<String, Object> properties, ResultCallback callback);

    /**
     * The send message
     *
     * @param title      message title
     * @param topic      topic
     * @param tags       tags
     * @param key        key
     * @param obj        object body
     * @param properties properties
     * @param callback   {@link ResultCallback}
     */
    void send(String title, String topic, String tags, String key, Object obj, Map<String, Object> properties, ResultCallback callback);

    /**
     * The destroy
     */
    void destroy();

}
