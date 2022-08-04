package com.study.onsmq;

/**
 * The Data ISerialize.
 *
 * @author lry
 */
public interface ISerialize {

    /**
     * The serialize object
     *
     * @param object object implements java.io.Serializable
     * @return byte[] data
     */
    byte[] serialize(Object object);

    /**
     * The deserialize object
     *
     * @param bytes byte[] data
     * @param clz   {@link T} class
     * @return {@link T}
     */
    <T> T deserialize(byte[] bytes, Class<T> clz);

}
