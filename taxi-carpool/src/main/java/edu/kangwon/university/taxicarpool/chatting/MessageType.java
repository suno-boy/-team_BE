package edu.kangwon.university.taxicarpool.chatting;

public enum MessageType {
    ENTER("입장"),
    LEAVE("퇴장"),
    TALK("대화");

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
