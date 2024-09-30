package org.example;

import net.dv8tion.jda.api.entities.Member;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Информация по сессии
 *
 * @author Михаил Александров
 * @since 19.09.2024
 */
public class SessionData {
    private final String sessionId;
    /**
     * Кто уже говорил
     */
    private final List<Member> alreadySpokenMembers = new ArrayList<>();
    
    /**
     * Кого пропустить
     */
    private final Set<Member> skipped = new HashSet<>();
    
    /**
     * Очередь "приоритетных" людей - если она не пустая, то в первую очередь берет людей из нее
     */
    private final Deque<Member> nextQueue = new ArrayDeque<>();
    /**
     * Очередь людей, кто будет выступать в конце - начнет выбирать людей из нее, когда общий пул закончится
     */
    private final Deque<Member> lastQueue = new ArrayDeque<>();

    public SessionData(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public class UpdateResult {
        public final String message;
        public final boolean isFinal;
        
        public UpdateResult(boolean isFinal) {
            this.message = getMembersMessage(isFinal);
            this.isFinal = isFinal;
        }
    }
    
    /**
     * Выбрать следующего участника и перегенерировать сообщение
     * @param currentMembers участники которые сейчас в канале
     */
    public UpdateResult update(Set<Member> currentMembers){
        return getNextMember(currentMembers)
            .map(nextMember -> {
                alreadySpokenMembers.add(nextMember);
                return new UpdateResult(isNoQueueLeft(currentMembers));
            })
            .orElseGet(() -> new UpdateResult(true));
    }
    
    private List<Member> getNotSpokenMembers(Set<Member> currentMembers){
        Set<Member> alreadySpokenMembersSet = new HashSet<>(alreadySpokenMembers);
        return currentMembers.stream()
            .filter(member ->
                Stream.of(alreadySpokenMembersSet, skipped, lastQueue)
                    .noneMatch(set -> set.contains(member))
                //nextQueue не учитываем, т.к. она первая перетечет в alreadySpokenMembersSet
            )
            .collect(Collectors.toList());
    }
    
    private boolean isNoQueueLeft(Set<Member> currentMembers){
        return nextQueue.isEmpty()
            && getNotSpokenMembers(currentMembers).isEmpty()
            && lastQueue.isEmpty();
    }
    
    private Optional<Member> getNextMember(Set<Member> currentMembers){
        if(!nextQueue.isEmpty()){
            return Optional.of(nextQueue.removeFirst());
        }
        List<Member> notSpokenMembers = getNotSpokenMembers(currentMembers);
        if(!notSpokenMembers.isEmpty()){
            int randomIndex = ThreadLocalRandom.current().nextInt(notSpokenMembers.size());
            return Optional.of(notSpokenMembers.get(randomIndex));
        }
        if(!lastQueue.isEmpty()){
            return Optional.of(lastQueue.removeFirst());
        }
        return Optional.empty();
    }
    
    public String getMembersMessage(boolean isFinishMessage){
        MessageBuilder messageBuilder = new MessageBuilder();
        IntStream.range(0, alreadySpokenMembers.size()).forEach(i -> {
            String memberText = getMemberDisplayText(alreadySpokenMembers.get(i));
            if(!isFinishMessage && i == alreadySpokenMembers.size() - 1){
                //Последний жирным
                messageBuilder.appendBoldNewLine(memberText);
            } else {
                messageBuilder.appendNewLine(memberText);
            }
        });
        if(isFinishMessage){
            messageBuilder.finish();
        }
        return messageBuilder.toString();
    }
    
    private String getMemberDisplayText(Member member){
        //Чтоб можно было легко поменять в будущем
        return member.getEffectiveName();
    }

    public String getSessionId() {
        return sessionId;
    }
    
    public void addToNextQueue(Member member){
        nextQueue.add(member);
    }
    
    public void addToLastQueue(Member member){
        lastQueue.add(member);
    }
    
    public void addSkipped(Member member){
        skipped.add(member);
    }
}
