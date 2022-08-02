package com.study.send;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.study.mq.RocketMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@RocketMessage(title = "测试异步与同步发送与消费", tags = IMessage.VACATE_AUDIT, topic = IMessage.AI_SCHOOL_STUDENT_VACATE_TOPIC)
public class VacateAuditMessage extends IMessage {

    /**
     * 学生用户ID
     **/
    private String studentId;
    /**
     * 学生学工号
     **/
    private String studentNumber;
    /**
     * 年级ID
     **/
    private String gradeId;
    /**
     * 班级ID
     **/
    private String classId;
    /**
     * 开始时间(UTC+08:00)
     **/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    /**
     * 结束时间(UTC+08:00)
     **/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
