package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationTaskService {

    private Logger logger = LoggerFactory.getLogger(NotificationTaskService.class);

    @Autowired
    private TelegramBot telegramBot;

    private final NotificationTaskRepository repository;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");

    public NotificationTaskService(NotificationTaskRepository repository, TelegramBot telegramBot) {
        this.repository = repository;
        this.telegramBot = telegramBot;
    }

    @PostConstruct
    public void validateToken() {
        if (telegramBot == null) {
            throw new IllegalStateException("Токен Telegram Bot не настроен. Проверьте конфигурацию!");
        }
    }

    /**
     * Обработка сообщения от пользователя и сохранение задачи в БД.
     */
    public void processAndSaveMessage(Long chatId, String message) {
        Matcher matcher = MESSAGE_PATTERN.matcher(message);

        if (matcher.matches()) {
            String dateTimeString = matcher.group(1);
            String notificationText = matcher.group(3);

            LocalDateTime notificationTime = LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);

            NotificationTask task = new NotificationTask();
            task.setChatId(chatId);
            task.setNotificationText(notificationText);
            task.setNotificationTime(notificationTime);

            repository.save(task);
        } else {
            throw new IllegalArgumentException("Сообщение не соответствует формату: 'dd.MM.yyyy HH:mm Текст напоминания'");
        }
    }

    /**
     * Шедулер: раз в минуту проверяет и отправляет уведомления.
     */
    @Scheduled(cron = "0 0/1 * * * *") // Каждую минуту
    public void checkAndSendNotifications() {
        LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        // Получаем задачи, которые нужно отправить в текущую минуту
        List<NotificationTask> tasksToSend = repository.findByNotificationTime(currentMinute);
        logger.info("Найдено задач для отправки в {}: {}", currentMinute, tasksToSend.size());

        // Отправляем уведомления
        for (NotificationTask task : tasksToSend) {
            try {
                sendNotification(task);

                // Лог успешной отправки
                logger.info("Уведомление успешно отправлено для задачи: {}", task);

                // Удаляем задачу после отправки, если она больше не нужна
                repository.delete(task);
            } catch (Exception e) {
                logger.error("Ошибка при отправке уведомления для задачи {}: {}", task, e.getMessage());
            }
        }
    }

    /**
     * Отправка сообщения в Telegram-чат.
     */
    public void sendMessage(Long chatId, String messageText) {
        // Создаем сообщение
        SendMessage message = new SendMessage(chatId.toString(), messageText);

        try {
            // Отправляем сообщение через execute()
            SendResponse response = telegramBot.execute(message); // 'bot' — экземпляр вашего TelegramBot

            // Проверяем результат отправки
            if (response.isOk()) {
                System.out.printf("Сообщение отправлено в чат %d: %s%n", chatId, messageText);
            } else {
                System.err.printf("Ошибка при отправке сообщения в чат %d. Код ошибки: %d%n",
                        chatId, response.errorCode());
            }
        } catch (Exception e) {
            System.err.printf("Исключение при отправке сообщения в чат %d: %s%n", chatId, e.getMessage());
        }
    }


    /**
     * Отправка уведомления для задачи.
     */
    public void sendNotification(NotificationTask task) {
        // Формируем текст сообщения с датой и временем
        String fullMessage = String.format(
                "Напоминание на %s: %s",
                task.getNotificationTime().format(DATE_TIME_FORMATTER),
                task.getNotificationText()
        );

        // Отправляем сообщение
        sendMessage(task.getChatId(), fullMessage);

        // Логируем отправку
        logger.info("Уведомление отправлено в чат {}: {}", task.getChatId(), fullMessage);
    }
}
