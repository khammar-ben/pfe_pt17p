package com.example.demo.web;

import com.example.demo.service.ScheduledNotificationService;
import com.example.demo.service.ScheduledNotificationService.NotificationRunResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final ScheduledNotificationService scheduledNotificationService;

    public NotificationController(ScheduledNotificationService scheduledNotificationService) {
        this.scheduledNotificationService = scheduledNotificationService;
    }

    @PostMapping("/run/prets")
    public NotificationRunResult runPrets() {
        return scheduledNotificationService.executerRappelsPrets();
    }

    @PostMapping("/run/pannes")
    public NotificationRunResult runPannes() {
        return scheduledNotificationService.executerEscaladesPannes();
    }

    @PostMapping("/run/stock")
    public NotificationRunResult runStock() {
        return scheduledNotificationService.executerAlertesStock();
    }

    @PostMapping("/run/all")
    public NotificationRunResult runAll() {
        return scheduledNotificationService.executerTout();
    }
}
