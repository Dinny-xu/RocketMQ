package com.study.onsmq.support;

import com.study.onsmq.ConnectionConfig;
import com.study.onsmq.ISerialize;
import com.study.onsmq.consumer.OrderMessageListener;
import com.study.onsmq.filter.MessageFilter;

import java.util.List;

/**
 * MqProducer
 *
 * @author lry
 */
public interface MqOrderConsumer {

    /**
     * The initialize
     *
     * @param filter           {@link MessageFilter}
     * @param serialize        {@link ISerialize}
     * @param connectionConfig {@link ConnectionConfig}
     * @param messageListeners {@link List < MessageListener >}
     */
    void initialize(MessageFilter filter, ISerialize serialize, ConnectionConfig connectionConfig, List<OrderMessageListener> messageListeners);

    /**
     * The destroy
     */
    void destroy();

}
