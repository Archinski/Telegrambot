package pro.sky.telegrambot.contoller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.service.NotificationTaskService;

@RestController
public class NotificationTaskController {

    private Logger logger = LoggerFactory.getLogger(NotificationTaskController.class);

    @Autowired
    private NotificationTaskService notificationTaskService;

    @PostMapping("/processMessage")
    public String processMessage(@RequestParam Long chatId, @RequestParam String message) {
        try {
            notificationTaskService.processAndSaveMessage(chatId, message);
            logger.info("Напоминание успешно сохранено для чата {}: {}", chatId, message);
            return "Напоминание успешно сохранено!";
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка при сохранении напоминания: {}", e.getMessage());
            return "Ошибка: " + e.getMessage();
        }
    }
}
