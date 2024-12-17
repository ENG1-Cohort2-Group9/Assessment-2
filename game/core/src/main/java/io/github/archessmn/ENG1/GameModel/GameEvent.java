package io.github.archessmn.ENG1.GameModel;

public enum GameEvent {
    Flooding("Flooding", "Your buildings near lakes are flooding! They will be closed for some time", 0.4f),
    Smelly("Smelly building!", "A random accommodation building has become permanently smelly!", 0.1f),
    Seagull("Seagulls have invaded", "A random building has been permanently closed due to seagull nesting", 0.05f),
    TreeDamage("A tree has fallen on a building", "A new one will need to be constructed", 0.1f),
    LongBoiSighting("Long boi sighting", "LONG BOI HAS BEEN SPOTTED!!!", 0.02f),
    TreeHype("Tree hype!", "The latest trend is living near trees!", 0.4f),
    RockClimbing("Rock climbing", "Rock climbing is the sport of the year!", 0.1f),
    LectureView("A pleasant view","Students want nice scenery to look at while \"focusing\" in lectures", 0.7f),
    GooseAttack("Goose attack", "A student has been attacked by a goose!", 0.02f),
    TooMuchHousing("Too much housing!", "There are more students than teaching buildings can accommodate, some students are missing out!", 0.5f),
    TooMuchTeaching("Too many teaching buildings!", "Nobody can remember which building they're supposed to be in!", 0.3f),
    GymHype("Upcoming sports tournament", "Students want more gyms to practice!", 0.1f),
    SportsWon("Sports tournament won!", "We won the university tournament!", 0.1f),
    TooManyPubs("Too many pubs!", "There are more pubs than teaching buildings! Students' marks are suffering.", 0.5f);

    public final String title;
    public final String description;
    public final float chance; // Relative chance of occurring compared with all other events. Total chance of all events can be any value > 0

    private GameEvent(String title, String description, float chance) {
        this.title = title;
        this.description = description;
        this.chance = chance;
    }
}
