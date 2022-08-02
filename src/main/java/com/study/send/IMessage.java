package com.study.send;

import lombok.Data;

import java.io.Serializable;

/**
 * IMessage
 *
 * @author lry
 */
@Data
public class IMessage implements Serializable {

    /**
     * 本地MQ连接组
     */
    public static final String LOCAL_CONNECT_GROUP = "local";

    /**
     * 智慧校园到IOT平台的消息
     */
    public static final String AI_IOT_MESSAGE_TOPIC = "AI_IOT_MESSAGE";

    /**
     * ai-app-iot-api转发的电子班牌门禁事件消息tag
     */
    public static final String CLASS_BRAND_ACCESS = "CLASS_BRAND_ACCESS";

    public static final String VACATE_AUDIT = "VACATE_AUDIT";

    public static final String AI_SCHOOL_STUDENT_VACATE_TOPIC = "AI_APP_MESSAGE";

    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 平台类别
     * <p>
     */
    private String platform;
    /**
     * 学校ID
     */
    private String schoolId;

}
