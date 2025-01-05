package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;

import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskService notificationTaskService;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            // Проверка на наличие сообщения
            if (update.message() != null && update.message().text() != null) {
                String messageText = update.message().text();
                Long chatId = update.message().chat().id();

                // Обработка команды /start
                if (messageText.equals("/start")) {
                    sendWelcomeMessage(chatId);
                } else {
                    // Обработка сообщения пользователя
                    processUserMessage(chatId, messageText);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeMessage = "Привет! Отправьте мне сообщение в формате: \n" +
                "01.01.2022 20:00 Сделать домашнюю работу\n" +
                "и я напомню вам об этом в указанное время!";
        SendMessage sendMessage = new SendMessage(chatId, welcomeMessage);

        try {
            telegramBot.execute(sendMessage);
            logger.info("Приветственное сообщение отправлено в чат: {}", chatId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке приветственного сообщения в чат: {}", chatId, e);
        }
    }

    private void processUserMessage(Long chatId, String messageText) {
        try {
            // Передача сообщения в NotificationTaskService для обработки и сохранения
            notificationTaskService.processAndSaveMessage(chatId, messageText);

            // Отправка подтверждения пользователю
            sendConfirmationMessage(chatId);
        } catch (IllegalArgumentException e) {
            // Отправка ошибки пользователю, если формат сообщения неверный
            sendErrorMessage(chatId, e.getMessage());
        }
    }

    private void sendConfirmationMessage(Long chatId) {
        String confirmationMessage = "Напоминание успешно сохранено!";
        SendMessage sendMessage = new SendMessage(chatId, confirmationMessage);

        try {
            telegramBot.execute(sendMessage);
            logger.info("Сообщение о подтверждении отправлено в чат: {}", chatId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке подтверждающего сообщения в чат: {}", chatId, e);
        }
    }

    private void sendErrorMessage(Long chatId, String errorMessage) {
        String errorResponse = "Ошибка: " + errorMessage + "\n" +
                "Пожалуйста, используйте формат: \n" +
                "01.01.2022 20:00 Сделать домашнюю работу";
        SendMessage sendMessage = new SendMessage(chatId, errorResponse);

        try {
            telegramBot.execute(sendMessage);
            logger.info("Сообщение об ошибке отправлено в чат: {}", chatId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения об ошибке в чат: {}", chatId, e);
        }
    }
}

