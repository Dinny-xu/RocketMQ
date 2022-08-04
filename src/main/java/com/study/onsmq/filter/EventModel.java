package com.study.onsmq.filter;

import com.alibaba.fastjson.annotation.JSONField;
import com.aliyun.openservices.shade.org.apache.commons.codec.digest.DigestUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.study.onsmq.ConnectionConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * EventModel
 *
 * @author lry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventModel implements Serializable {

    /**
     * 事件名称
     */
    private String title;
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

    /**
     * CONSUMER/PRODUCER
     */
    private String mqType;
    /**
     * 生产组/消费组
     */
    private String groups;
    /**
     * Topic
     */
    private String topic;
    /**
     * Tags
     */
    private String tags;
    /**
     * Keys
     */
    private String keys;

    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 租户名称
     */
    private String tenantName;
    /**
     * Attachment
     */
    private Map<String, String> attachments;

    public static EventModel build(String groups, String topic, String tags, String keys,
                                   Map<String, String> attachments, String title, String state, String msg) {
        EventModel eventModel = new EventModel();
        eventModel.setTitle(title);
        eventModel.setEventId(DigestUtils.md5Hex(String.format("%s@%s@%s@%s@%s", groups, topic, tags, keys, state)));
        eventModel.setEventTime(LocalDateTime.now());
        eventModel.setState(state);
        eventModel.setMsg(msg);
        eventModel.setGroups(groups);
        eventModel.setTopic(topic);
        eventModel.setTags(tags);
        eventModel.setKeys(keys);
        eventModel.setTenantId(attachments != null ? attachments.get(ConnectionConfig.TENANT_ID_KEY) : null);
        eventModel.setTenantName(attachments != null ? attachments.get(ConnectionConfig.TENANT_NAME_KEY) : null);
        eventModel.setAttachments(attachments);
        return eventModel;
    }

}
