package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.NotificationRepository;
import vn.fpt.seima.seimaserver.service.NotificationService;

@Service
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private NotificationRepository notificationRepository;
} 