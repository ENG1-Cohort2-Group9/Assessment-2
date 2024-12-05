package io.github.archessmn.ENG1.GameModel;

public enum GameEvent {
    Flooding("Flooding", "Your buildings near lakes are flooding!", 0.4f),
    Smelly("Smelly building!", "A random accommodation building has become smelly!", 0.1f),
    Seagull("Seagulls have invaded", "A random building has been permanently closed due to seagull nesting", 0.05f),
    AColdWinter("A cold winter", "This winter is a particularly cold one.", 0.2f),
    TreeDamage("A tree has fallen on a building", "This will take some time to repair", 0.1f),
    LongBoiSighting("Long boi sighting", "LONG BOI HAS BEEN SPOTTED!!!", 0.02f),
    TreeHype("Tree hype!", "The latest trend is studying near trees!", 0.4f),
    RockClimbing("Rock climbing", "Rock climbing is the sport of the year!", 0.1f),
    LectureLake("A pleasant view","Students want to look at water while \"focusing\" in lectures", 0.7f),
    GooseAttack("Goose attack", "A student has been attacked by a goose!", 0.02f);

    public final String title;
    public final String description;
    public final float rarity;

    private GameEvent(String title, String description, float rarity) {
        this.title = title;
        this.description = description;
        this.rarity = rarity;
    }
}
