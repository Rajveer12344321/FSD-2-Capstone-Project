package com.bloodbridge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodic housekeeping: expire overdue requests, process the queue, retry failed deliveries. */
@Component
@ConditionalOnProperty(name = "bloodbridge.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final RequestService requests;
    private final QueueService queue;
    private final InvitationService invitations;

    public SchedulerService(RequestService requests, QueueService queue, InvitationService invitations) {
        this.requests = requests;
        this.queue = queue;
        this.invitations = invitations;
    }

    @Scheduled(initialDelay = 10000, fixedDelayString = "${bloodbridge.scheduler.delay-ms:15000}")
    public void tick() {
        try {
            requests.expireOverdue();
            queue.processPending();
            invitations.retryFailed();
        } catch (Exception e) {
            log.error("Scheduler run failed", e);
        }
    }
}
