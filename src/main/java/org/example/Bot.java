package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Properties;

/**
 * TODO Class Description
 *
 * @author Михаил Александров
 * @since 19.07.2024
 */
public class Bot {
    /**
     * Путь к файлу с пропертями
     */
    private static final String PROPERTY_FILE_PATH = "src/main/resources/config.properties";
    /**
     * Список прав бота
     */
    private static final EnumSet<GatewayIntent> INTENTS = EnumSet.of(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_VOICE_STATES,
        GatewayIntent.MESSAGE_CONTENT
    );
    /**
     * Проперти проекта
     */
    private Properties properties;

    public Bot() {
        initProperties();
    }

    public void run() {
        JDABuilder.create(properties.getProperty("token"), INTENTS).addEventListeners(new ChannelEventListener()).build();
        openApplicationWindow();
    }

    private void initProperties() {
        try {
            File propertyFile = new File(PROPERTY_FILE_PATH);
            properties = new Properties();
            properties.load(new FileReader(propertyFile));
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке пропертей\n" + e);
        }
    }

    /**
     * Открываем окошко, при закрытии котрого будет выключаться приложение
     */
    private void openApplicationWindow() {
        JFrame frame = new JFrame("DiscorMeetingBot");
        JLabel label = new JLabel("Бот запущен", JLabel.CENTER);
        frame.add(label);
        frame.setSize(300, 300);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
