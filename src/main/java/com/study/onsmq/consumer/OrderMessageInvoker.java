package com.study.onsmq.consumer;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Message Invoker
 *
 * @author lry
 */
@Data
public class OrderMessageInvoker<M> implements Serializable {

    private Class<M> messageClass;
    private OrderMessageListener<M> orderMessageListener;

    @SuppressWarnings("unchecked")
    public OrderMessageInvoker(OrderMessageListener<M> listener) {
        this.orderMessageListener = listener;
        Type type = listener.getClass().getGenericInterfaces()[0];
        this.messageClass = (Class<M>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    public static <C> Class<C> getParameterizedType(Object listener) {
        Type[] types = listener.getClass().getGenericInterfaces();
        if (types.length > 0) {
            if (types[0] instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) types[0]).getActualTypeArguments();
                if (args[0] instanceof Class) {
                    return (Class<C>) args[0];
                }
            }
        }

        throw new RuntimeException("Not found parameterized type:" + listener.getClass());
    }

}