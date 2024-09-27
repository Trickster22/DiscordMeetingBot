package org.example;

import net.dv8tion.jda.api.entities.Member;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * Очередь "приоритетных" людей - если она не пустая, то берет людей из нее
     */
    private final Deque<Member> nextQueue = new ArrayDeque<>();
    /**
     * Очередь людей, кто будет повторно выступать в конце
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
    
    private Set<Member> filterSkipped(Set<Member> currentMembers){
        return currentMembers.stream().filter(member -> !skipped.contains(member)).collect(Collectors.toSet());
    }
    
    private boolean isNoQueueLeft(Set<Member> currentMembers){
        return nextQueue.isEmpty()
            && new HashSet<>(alreadySpokenMembers).containsAll(filterSkipped(currentMembers))
            && lastQueue.isEmpty();
    }
    
    private Optional<Member> getNextMember(Set<Member> currentMembers){
        if(!nextQueue.isEmpty()){
            return Optional.of(nextQueue.removeFirst());
        }
        List<Member> notSpokenMembers = new ArrayList<>(filterSkipped(currentMembers));
        notSpokenMembers.removeAll(alreadySpokenMembers);
        if(!notSpokenMembers.isEmpty()){
            Collections.shuffle(notSpokenMembers);
            return Optional.of(notSpokenMembers.get(0));
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
