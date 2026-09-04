package in.bulletbeats.domain.waiting.entity;

public enum WaitingActivityCategory {
    GAME("Games", "🎲"),
    BOOK_COMIC("Books & Comics", "📚"),
    OTHER("More", "✨");

    private final String displayName;
    private final String emoji;

    WaitingActivityCategory(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }
}
