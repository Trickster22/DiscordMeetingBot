package org.example;

/**
 * Строитель сообщений
 *
 * @author Михаил Александров
 * @since 19.09.2024
 */
public class MessageBuilder {
    private static final String GREETINGS = "Всем привет! Начинаем очередь:";
    private static final String BYE = "Очередь завершена. Всем пока!";
    private final StringBuilder messageBuilder;

    public MessageBuilder() {
        this.messageBuilder = new StringBuilder(GREETINGS);
    }

    public String append(String text) {
        return messageBuilder.append(text).toString();
    }

    public String appendBold(String text) {
        return messageBuilder.append(String.format("**%s**", text)).toString();
    }

    public String appendBoldNewLine(String text) {
        return messageBuilder.append("\n").append(String.format("**%s**", text)).toString();

    }

    public String appendNewLine(String text) {
        return messageBuilder.append("\n").append(text).toString();
    }

    public String finish() {
        return appendBoldNewLine(BYE);
    }

    @Override
    public String toString() {
        return messageBuilder.toString();
    }
}
