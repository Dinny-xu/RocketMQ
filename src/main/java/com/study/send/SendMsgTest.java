package com.study.send;


import com.study.MqConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dinny-xu
 */
@RestController
@Slf4j
public class SendMsgTest {

    @PostMapping("normal/send")
    public void normalSendMsg(@RequestBody VacateAuditMessage message) {
        // 普通发送
        MqConfiguration.defaultSend(message.getStudentId(), message);
    }

    @PostMapping("order/test")
    public void orderSendMsg(@RequestBody VacateAuditMessage message) {
        // 顺序发送
        MqConfiguration.defaultOrderlySend(message.getStudentId(), message);
    }
}
