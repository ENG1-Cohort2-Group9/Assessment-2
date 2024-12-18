package io.github.archessmn.ENG1.GameModel;

public enum GameEvent {
    Flooding("Flooding", "Your buildings near lakes are flooding! They will be closed for some time.", 0.4f),
    Smelly("Smelly building!", "A random accommodation building has become permanently smelly!", 0.1f),
    Seagull("Seagulls have invaded", "A random building has been permanently closed due to seagull nesting.", 0.05f),
    TreeDamage("A tree has fallen on a building", "A new one will need to be constructed", 0.1f),
    LongBoiSighting("Long boi sighting", "LONG BOI HAS BEEN SPOTTED!!!", 0.02f),
    TreeHype("Tree hype!", "The latest trend is living near trees!", 0.4f),
    RockClimbing("Rock climbing", "Rock climbing is the sport of the year! Students want to live near rocks.", 0.1f),
    LectureView("A pleasant view","Students want nice scenery to look at while \"focusing\" in lectures.", 0.7f),
    TooManyBuildings("Too many buildings!", "Nobody can remember where they're supposed to be!", 0.2f),
    GymHype("Upcoming tournament", "Everyone wants to practise in the gym for the big game", 0.4f),
    TournamentWon("Our team won!", "The university is victorious in the tournament!", 0.5f),
    GooseAttack("Goose attack", "A student has been attacked by a goose!", 0.02f);

    public final String title;
    public final String description;
    public final float chance; // Relative chance of occurring compared with all other events. Total chance of all events can be any value > 0

    private GameEvent(String title, String description, float chance) {
        this.title = title;
        this.description = description;
        this.chance = chance;
    }
}
