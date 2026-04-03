package com.loopers.application.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class);

    private final QueueService queueService;

    public QueueScheduler(QueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * 100ms마다 실행하여 Thundering Herd 완화.
     * 1초에 175 TPS 기준, 100ms당 ~18명씩 토큰 발급.
     */
    @Scheduled(fixedDelayString = "${queue.scheduler.interval-ms:100}")
    public void issueEntryTokens() {
        int issued = queueService.issueTokens();
        if (issued > 0) {
            log.info("대기열 토큰 발급: {}명", issued);
        }
    }
}
