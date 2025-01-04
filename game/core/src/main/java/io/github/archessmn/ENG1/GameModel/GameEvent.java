package io.github.archessmn.ENG1.GameModel;

public enum GameEvent {
    FLOODING("Flooding", "Your buildings near lakes are flooding! They will be closed for some time.", 0.4f, "Flooding.png"),
    SEAGULL("Seagulls have invaded", "A random building has been permanently closed due to seagull nesting.", 0.05f),
    TREE_DAMAGE("A tree has fallen on a building", "A new one will need to be constructed", 0.1f),
    LONG_BOI_SIGHTING("Long boi sighting", "LONG BOI HAS BEEN SPOTTED!!!", 0.02f, "LongBoi.png"),
    TREE_HYPE("Tree hype!", "The latest trend is living near trees!", 0.4f, "TreeHype.png"),
    ROCK_CLIMBING("Rock climbing", "Rock climbing is the sport of the year! Students want to live near rocks.", 0.1f, "RockClimbing.png"),
    LECTURE_VIEW("A pleasant view","Students want nice scenery to look at while \"focusing\" in lectures.", 0.7f, "LectureView.png"),
    TOO_MANY_BUILDINGS("Too many buildings!", "Nobody can remember where they're supposed to be!", 0.2f, "TooManyBuildings.png"),
    GYM_HYPE("Upcoming tournament", "Everyone wants to practise in the gym for the big game", 0.4f, "GymHype.png"),
    TOURNAMENT_WON("Our team won!", "The university is victorious in the tournament!", 0.5f, "TournamentWon.png"),
    GOOSE_ATTACK("Goose attack", "A student has been attacked by a goose!", 0.02f);

    public final String title;
    public final String description;
    public final float chance; // Relative chance of occurring compared with all other events. Total chance of all events can be any value > 0
    public final String iconName; // File name of this event's icon

    private GameEvent(String title, String description, float chance) {
        this.title = title;
        this.description = description;
        this.chance = chance;
        this.iconName = "missingTexture.png";
    }

    private GameEvent(String title, String description, float chance, String iconName) {
        this.title = title;
        this.description = description;
        this.chance = chance;
        this.iconName = iconName;
    }
}
