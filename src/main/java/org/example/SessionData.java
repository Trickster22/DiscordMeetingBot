package org.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Информация по сессии
 *
 * @author Михаил Александров
 * @since 19.09.2024
 */
public class SessionData {
    private final List<String> memberList;
    private final MessageBuilder messageBuilder;
    private final String sessionId;

    private final Set<String> alreadySpokenMembers;

    public SessionData(List<String> memberList, String sessionId) {
        this.memberList = memberList;
        messageBuilder = new MessageBuilder();
        this.sessionId = sessionId;
        alreadySpokenMembers = new HashSet<>();
    }

    public String getNextMember() {
        String member = memberList.remove(0);
        alreadySpokenMembers.add(member);
        return member;
    }

    public void addNewMember(String member) {
        if (!alreadySpokenMembers.contains(member)) {
            memberList.add(member);
        }
    }

    public void removeMember(String memberToRemove) {
        memberList.removeIf(member -> member.equals(memberToRemove));
    }

    public List<String> getMemberList() {
        return memberList;
    }

    public MessageBuilder getMessageBuilder() {
        return messageBuilder;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isMemberListEmpty() {
        return memberList == null || memberList.isEmpty();
    }
}
