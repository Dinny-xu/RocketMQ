package com.study.mq.support;

import com.study.mq.ConnectionConfig;
import com.study.mq.ISerialize;
import com.study.mq.consumer.OrderMessageListener;
import com.study.mq.filter.MessageFilter;

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