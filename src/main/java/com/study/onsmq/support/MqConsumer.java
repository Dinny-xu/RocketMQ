package com.study.onsmq.support;

import com.study.onsmq.ConnectionConfig;
import com.study.onsmq.ISerialize;
import com.study.onsmq.consumer.MessageListener;
import com.study.onsmq.filter.MessageFilter;

import java.util.List;

/**
 * MqProducer
 *
 * @author lry
 */
public interface MqConsumer {

    /**
     * The initialize
     *
     * @param filter           {@link MessageFilter}
     * @param serialize        {@link ISerialize}
     * @param connectionConfig {@link ConnectionConfig}
     * @param messageListeners {@link List < MessageListener >}
     */
    void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig, List<MessageListener> messageListeners);

    /**
     * The destroy
     */
    void destroy();

}
