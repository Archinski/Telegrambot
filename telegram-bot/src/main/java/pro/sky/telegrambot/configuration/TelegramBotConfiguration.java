package pro.sky.telegrambot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot("7862736277:AAEmq-m5_kRHBnEIrO2b9IZFwixXOMpuQJo");
    }
}

