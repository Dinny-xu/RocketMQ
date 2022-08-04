package com.study.onsmq.filter;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * TraceMessage
 *
 * @author lry
 */
@Data
public class TraceMessage implements Serializable {

    /**
     * 事件名称
     */
    private String title;
    /**
     * 类别
     */
    private String category;
    /**
     * 事件ID
     */
    private String eventId;
    /**
     * 事件时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;
    /**
     * 状态
     */
    private String state;
    /**
     * 描述
     */
    private String msg;

    // === App

    /**
     * 应用ID
     */
    private String appId;
    /**
     * 应用名称
     */
    private String appName;

    // === Tenant

    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 租户名称
     */
    private String tenantName;

}
