package org.example;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Слушатель событий в канале
 *
 * @author Михаил Александров
 * @since 18.05.2024
 */
public class ChannelEventListener extends ListenerAdapter {
    private static final String GREETINGS = "Всем привет! Начинаем очередь:\n";
    private static final String BYE = "Очередь завершена. Всем пока!";
    private static final String MAIN_VOICE_CHANNEL_NAME = "Основной";
    private static final long MAIN_VOICE_CHANNEL_ID = 1154392230660952078L;
    private static final String NEXT_BUTTON_ID = "nextbtn";
    public static final String ADMIN_ROLE_ID = "1154393235851063358";
    private final StringBuilder randomMessageBuilder = new StringBuilder(GREETINGS);
    private List<String> joinedMemberList;

    /**
     * реакиця на получение сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        //Чтоб не реагировал на сообщения ботов
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentRaw();
        if (message.startsWith(">")) {
            switch (message) {
                case ">ping":
                    ping(channel);
                    break;
                case ">r":
                    random(event);
                    break;
                case ">kill":
                    kill(event);
                default:
                    channel.sendMessage("Я такой команды не знаю").queue();
            }
        }
    }

    private void random(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (shuffleMembers(event)) {
            String member = joinedMemberList.remove(0);
            channel.sendMessage(randomMessageBuilder + String.format("**%s**", member))
                .addActionRow(Button.success(NEXT_BUTTON_ID, "Следующий"))
                .queue();
            randomMessageBuilder.append(member);
            randomMessageBuilder.append("\n");
        } else {
            channel.sendMessage("В голосовом канале никого нет...").queue();
        }
    }
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals(NEXT_BUTTON_ID)) {
            chooseNextMember(event);
        }
    }

    private void chooseNextMember(ButtonInteractionEvent event) {
        boolean isMemberListEmpty = joinedMemberList.isEmpty();
        String member = isMemberListEmpty ? BYE : joinedMemberList.remove(0);
        MessageEditCallbackAction action = event.deferEdit();
        if (isMemberListEmpty) {
            action.setComponents();
        }
        action.setContent(randomMessageBuilder + String.format("**%s**", member)).queue();
        if (!isMemberListEmpty) {
            randomMessageBuilder.append(member);
            randomMessageBuilder.append("\n");
        } else {
            randomMessageBuilder.setLength(0);
            randomMessageBuilder.append(GREETINGS);
        }
    }

    private boolean shuffleMembers(MessageReceivedEvent event) {
        VoiceChannel channel = event.getGuild().getVoiceChannelById(MAIN_VOICE_CHANNEL_ID);
        if (channel == null) {
            return false;
        }
        joinedMemberList = channel.getMembers().stream().map(Member::getEffectiveName).collect(Collectors.toList());
        if (joinedMemberList.isEmpty()) {
            return false;
        }
        Collections.shuffle(joinedMemberList);
        return true;
    }

    private void ping(MessageChannel channel) {
        channel.sendMessage("Понг!").queue();
    }

    private void kill(MessageReceivedEvent event) {
        Member member = event.getMember();
        MessageChannel channel = event.getChannel();
        if (member == null) {
            channel.sendMessage("Произошло что-то непонятное. Попробуйте еще раз").queue();
            return;
        }
        boolean isAdmin = member.getRoles()
            .stream()
            .anyMatch(role -> ADMIN_ROLE_ID.equalsIgnoreCase(role.getId()));
        if (isAdmin) {
            channel.sendMessage("Зачем же ты меня убил...").queue();
            System.exit(0);
        } else {
            channel.sendMessage("У вас недостаточно прав для выполнения этой команды").queue();
        }
    }
}
