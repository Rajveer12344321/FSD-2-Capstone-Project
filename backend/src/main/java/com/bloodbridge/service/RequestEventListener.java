package com.bloodbridge.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Event-driven trigger: after a request commits, run the priority queue immediately. */
@Component
public class RequestEventListener {
    private final QueueService queue;

    public RequestEventListener(QueueService queue) {
        this.queue = queue;
    }

    @TransactionalEventListener
    public void onRequestCreated(RequestService.RequestCreatedEvent event) {
        queue.processPending();
    }
}
