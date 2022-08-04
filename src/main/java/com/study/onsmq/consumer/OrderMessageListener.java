package com.study.onsmq.consumer;

/**
 * OrderMessageListener
 *
 * @param <M> {@link M}
 * @author lry
 */
public interface OrderMessageListener<M> {

    /**
     * The notify message
     *
     * @param message {@link M}
     */
    default void onMessage(M message) {

    }

    /**
     * The notify message
     *
     * @param consumerGroup consumer group
     * @param message       {@link M}
     */
    default void onMessage(String consumerGroup, M message) {
        onMessage(message);
    }

}
