package com.study.mq.filter;

/**
 * MessageFilter
 *
 * @author lry
 */
public interface MessageFilter {

    /**
     * The filter
     *
     * @param trace      true is trace message
     * @param consumer   true is consumer message
     * @param eventModel {@link EventModel}
     */
    default void filter(boolean trace, boolean consumer, EventModel eventModel) {

    }

}
