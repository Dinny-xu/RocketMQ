package com.study.consumer;

import com.study.mq.consumer.MessageListener;
import com.study.send.VacateAuditMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dinny-xu
 */
@RestController
@Slf4j
@Service
public class VacationEventListener implements MessageListener<VacateAuditMessage> {

    @Override
    public void onMessage(VacateAuditMessage message) {
        log.info("线程id为:{},线程名称:{},用户ID为:{}", Thread.currentThread().getId(), Thread.currentThread().getName(), message.getStudentId());
    }

}
