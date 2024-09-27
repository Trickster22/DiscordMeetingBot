package org.example;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Слушатель событий в канале
 *
 * @author Михаил Александров
 * @since 18.05.2024
 */
public class ChannelEventListener extends ListenerAdapter {
    private static final Logger LOGGER = Logger.getLogger("ChannelEventListener");
    private static final long MAIN_VOICE_CHANNEL_ID = 1154392230660952078L;
    private static final String NEXT_BUTTON_ID = "nextbtn";
    private static final String ADMIN_ROLE_ID = "1154393235851063358";
    /**
     * Id сообщения с запуском рандома - информаиця о сессии
     */
    private final Map<String, SessionData> messageIdToSessionDataMap = new HashMap<>();

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
                    break;
                default:
                    channel.sendMessage("Я такой команды не знаю").queue();
            }
        }
    }

    private void random(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (shuffleMembers(event)) {
            String messageId = event.getMessageId();
            SessionData sessionData = messageIdToSessionDataMap.get(messageId);
            String member = sessionData.getMemberList().remove(0);
            MessageBuilder messageBuilder = sessionData.getMessageBuilder();
            channel.sendMessage(messageBuilder + String.format("\n**%s**", member))
                .addActionRow(Button.success(NEXT_BUTTON_ID + ":" + messageId, "Следующий"))
                .queue();
            messageBuilder.appendNewLine(member);
        } else {
            channel.sendMessage("В голосовом канале никого нет...").queue();
        }
    }
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] buttonId = event.getComponentId().split(":");
        if (buttonId[0].equals(NEXT_BUTTON_ID)) {
            chooseNextMember(event, buttonId[1]);
        }
    }

    private void chooseNextMember(ButtonInteractionEvent event, String messageId) {
        SessionData sessionData = messageIdToSessionDataMap.get(messageId);
        MessageBuilder messageBuilder = sessionData.getMessageBuilder();
        MessageEditCallbackAction action = event.deferEdit();
        if (!sessionData.isMemberListEmpty()) {
            String member = sessionData.getNextMember();
            action.setContent(messageBuilder + String.format("\n**%s**", member)).queue();
            messageBuilder.appendNewLine(member);
        } else {
            action.setComponents();
            action.setContent(messageBuilder.finish()).queue();
            messageIdToSessionDataMap.remove(messageId);
        }
    }

    private boolean shuffleMembers(MessageReceivedEvent event) {
        VoiceChannel channel = event.getGuild().getVoiceChannelById(MAIN_VOICE_CHANNEL_ID);
        if (channel == null) {
            return false;
        }
        List<String> joinedMemberList = channel.getMembers().stream().map(Member::getEffectiveName).collect(Collectors.toList());
        if (joinedMemberList.isEmpty()) {
            return false;
        }
        Collections.shuffle(joinedMemberList);
        SessionData sessionData = new SessionData(joinedMemberList, event.getMessageId());
        messageIdToSessionDataMap.put(sessionData.getSessionId(), sessionData);
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

    /**
     * Действия на изменение статуса голосового канала
     * Пока заточен только под наличие одного голосового канала
     */
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion joinedChannel = event.getChannelJoined();
        AudioChannelUnion leftChannel = event.getChannelLeft();
        String memberName = event.getMember().getEffectiveName();
        //Пользователь подключился к голосовому каналу
        if (joinedChannel != null) {
            addMemberToQueue(memberName);
        }
        //Пользователь покинул голосовой канал
        if (leftChannel != null) {
            removeMemberFromQueue(memberName);
        }
    }

    private void removeMemberFromQueue(String memberName) {
        LOGGER.info(String.format("Пользователь %s покинул канал", memberName));
        for (Entry<String, SessionData> entry : messageIdToSessionDataMap.entrySet()) {
            SessionData sessionData = entry.getValue();
            sessionData.removeMember(memberName);
            LOGGER.info(
                String.format(
                    "Seesion id = %s, members = %s",
                    sessionData.getSessionId(),
                    String.join(", ", sessionData.getMemberList())
                )
            );
        }

    }

    private void addMemberToQueue(String memberName) {
        LOGGER.info(String.format("Пользователь %s зашел в канал", memberName));
        for (Entry<String, SessionData> entry : messageIdToSessionDataMap.entrySet()) {
            SessionData sessionData = entry.getValue();
            sessionData.addNewMember(memberName);
            LOGGER.info(
                String.format(
                    "Seesion id = %s, members = %s",
                    sessionData.getSessionId(),
                    String.join(", ", sessionData.getMemberList())
                )
            );
        }
    }
}
