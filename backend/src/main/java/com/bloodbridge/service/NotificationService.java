package com.bloodbridge.service;

import com.bloodbridge.config.AppProperties;
import com.bloodbridge.domain.Notification;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.NotificationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    public NotificationService(NotificationRepository notifications, ObjectProvider<JavaMailSender> mailSender,
                               AppProperties props) {
        this.notifications = notifications;
        this.mailSender = mailSender;
        this.props = props;
    }

    /** Creates an in-app notification record. */
    @Transactional
    public void notifyUser(User user, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        notifications.save(n);
    }

    /**
     * Sends an email when email delivery is enabled. When it is disabled the in-app notification is the
     * delivery channel and this returns normally. Throws if email is enabled but sending fails.
     */
    public void sendEmail(User user, String subject, String body) {
        if (!props.notifications().emailEnabled()) return;
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) throw new IllegalStateException("Email is enabled but no mail server is configured");
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.notifications().from());
        msg.setTo(user.getEmail());
        msg.setSubject(subject);
        msg.setText(body);
        sender.send(msg);
    }
}
