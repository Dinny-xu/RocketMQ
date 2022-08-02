package com.study.mq.producer;

/**
 * ResultCallback
 *
 * @author lry
 */
public interface ResultCallback {

    /**
     * Send success
     *
     * @param producerGroup producerGroup
     * @param topic         topic
     * @param tags          tags
     * @param keys          keys
     */
    void onSuccess(String producerGroup, String topic, String tags, String keys);

    /**
     * Send exception
     *
     * @param producerGroup producerGroup
     * @param topic         topic
     * @param tags          tags
     * @param keys          keys
     * @param e             {@link Throwable}
     */
    void onException(String producerGroup, String topic, String tags, String keys, Throwable e);

}
