package com.study.mq.consumer;

/**
 * MessageListener
 *
 * @param <M> {@link M}
 * @author lry
 */
public interface MessageListener<M> {

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
