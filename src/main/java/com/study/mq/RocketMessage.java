package com.study.mq;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RocketMessage
 *
 * @author lry
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RocketMessage {

    /**
     * The message title
     */
    String title() default "";

    /**
     * The consumer tags(or prefix)
     */
    String tags() default "";

    /**
     * The consumer topic
     */
    String topic() default "";

}
