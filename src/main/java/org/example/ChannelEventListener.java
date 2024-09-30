package org.example;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        //Чтоб не реагировал на сообщения ботов
        if (event.getAuthor().isBot()) {
            return;
        }
        String message = event.getMessage().getContentRaw();
        if(!message.startsWith(">")){
            return;
        }
        
        List<String> commands = Arrays.stream(message.split("(?=>)"))
            .filter(part -> part.startsWith(">"))
            .map(String::trim)
            .collect(Collectors.toList());
        
        boolean isKnownCommand = false;
        for(String command : commands){
            isKnownCommand = tryDoingCommand(command, event) || isKnownCommand;
        }
        
        if(!isKnownCommand){
            event.getChannel().sendMessage("Я таких команд не знаю").queue();
        }
    }
    
    private boolean tryDoingCommand(String message, MessageReceivedEvent event){
        return Stream.<Supplier<Boolean>>of(
            () -> tryAddToQueue(message, ">next", SessionData::addToNextQueue, event),
            () -> tryAddToQueue(message, ">last", SessionData::addToLastQueue, event),
            () -> tryAddToQueue(message, ">skip", SessionData::addSkipped, event),
            () -> tryDoingSimpleCommand(message, event)
        ).anyMatch(Supplier::get);
    }
    
    private boolean tryDoingSimpleCommand(String message, MessageReceivedEvent event){
        switch (message) {
            case ">ping":
                ping(event.getChannel());
                return true;
            case ">r":
                random(event);
                return true;
            case ">kill":
                kill(event);
                return true;
            default:
                return false;
        }
    }

    private void random(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        Set<Member> currentMembers = getCurrentMembers(event.getGuild());
        if (!currentMembers.isEmpty()) {
            String messageId = event.getMessageId();
            SessionData sessionData = new SessionData(messageId);
            messageIdToSessionDataMap.put(messageId, sessionData);

            channel.sendMessage(sessionData.getMembersMessage(false))
                .addComponents(getNextMemberButton("Поехали", messageId))
                .queue();
            //Вместо того что выше, можно использовать updateAndSendInitial
        } else {
            channel.sendMessage("В голосовом канале никого нет...").queue();
        }
    }
    
    /**
     * Поведение аналогично тому, что было в боте раньше - сразу же определяется первый человек в очереди
     * Сейчас это не используется, чтобы позволить в одном сообщении задавать несколько команд сразу, типа
     * >r >skip Марат
     */
    private void updateAndSendInitial(SessionData sessionData, Set<Member> currentMembers, MessageChannel channel){
            SessionData.UpdateResult updateResult = sessionData.update(currentMembers);

            MessageCreateAction messageCreateAction = channel.sendMessage(updateResult.message);
            if(!updateResult.isFinal){
                messageCreateAction.addComponents(getNextMemberButton("Следующий", sessionData.getSessionId()));
            }
            messageCreateAction.queue();
    }
    
    private LayoutComponent getNextMemberButton(String caption, String messageId){
        return ActionRow.of(Button.success(NEXT_BUTTON_ID + ":" + messageId, caption));
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
        SessionData.UpdateResult updateResult = sessionData.update(getCurrentMembers(event.getGuild()));
        
        MessageEditCallbackAction action = event.deferEdit();
        action.setContent(updateResult.message);
        if(updateResult.isFinal){
            action.setComponents();
            messageIdToSessionDataMap.remove(messageId);
        } else {
            action.setComponents(getNextMemberButton("Следующий", messageId));
        }
        action.queue();
    }
    
    private boolean tryAddToQueue(
        String message,
        String command,
        BiConsumer<SessionData, Member> addToQueue,
        MessageReceivedEvent event
    ){
        if(!message.startsWith(command + " ")){
            return false;
        }
        List<String> memberParts = Arrays.stream(message.substring(command.length() + 1).split("[,;\n]"))
            .map(String::trim)
            .collect(Collectors.toList());
        if(memberParts.isEmpty()){
            return false;
        }
        
        Set<Member> currentMembers = getCurrentMembers(event.getGuild());
        List<String> unknownMembers = new ArrayList<>();
        for(String memberNamePart : memberParts){
            Optional<Member> memberOpt = currentMembers.stream()
                .filter(member -> member.getEffectiveName().toLowerCase().contains(memberNamePart.toLowerCase()))
                .findAny();
            
            if(!memberOpt.isPresent()){
                unknownMembers.add(memberNamePart);
                continue;
            }
            
            messageIdToSessionDataMap.values().forEach(sessionData ->
                addToQueue.accept(sessionData, memberOpt.get())
            );
        }
        
        if(!unknownMembers.isEmpty()){
            event.getChannel().sendMessage("Я таких не знаю: " + String.join(", ", unknownMembers)).queue();
        }
        return true;
    }
    
    private Set<Member> getCurrentMembers(Guild guild){
        VoiceChannel channel = guild.getVoiceChannelById(MAIN_VOICE_CHANNEL_ID);
        if (channel == null) {
            return new HashSet<>();
        }
        return new HashSet<>(channel.getMembers());
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
