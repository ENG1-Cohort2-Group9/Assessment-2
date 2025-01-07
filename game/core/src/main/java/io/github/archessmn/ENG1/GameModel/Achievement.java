package io.github.archessmn.ENG1.GameModel;

import java.util.Objects;
import java.util.function.BiPredicate;

public enum Achievement {
    OVER_EIGHTY("I <3 Uni","You maintained a satisfaction score over 80% for the last 3 minutes.",
        5f, 80, (value, condition) -> value > condition),

    MINIMALIST("Minimalist","You only placed 5 buildings throughout the entire game.",
        -10f, 5, Objects::equals),

    JAM_PACKED("Jam Packed","You filled the whole map!", -10f),

    DISTANCES("A Short Walk","You finished the game with 100% for your building distances.",
        2f, 100, Objects::equals),

    COMPLETION("Not so complete","You finished the game with less that 100% completion score.",
        -10f, 100, (value, condition) -> value < condition),

    EVENTFUL("Eventful","You reached 100% event response score.",
        + 5f, 100, Objects::equals),

    FULL_CAPACITY("Full Capacity","You finished the game with exactly enough capacity for your students.",
        10f),

    PLANNING("Poor Planning", "You never chose to pause the game.",
        -2f, 0, Objects::equals),

    DESTRUCTION("Demolition Derby","You demolished over 50 buildings this game.",
        -2f, 50, (value, condition) -> value > condition),

    BUILDER("The Builders' Best Frenemy","You built over 60 buildings this game.",
        +1f, 60, (value, condition) -> value > condition);


    private final String title; // Displayed as a title when the achievement is achieved.
    private final String description; // Displayed as a description when the achievement is achieved.
    private final Float scoreBonus; // The effect the achievement has on the score at the end of the game.
    private boolean achieved; // Whether the player has got the achievement or not
    private boolean finished; // Whether the achievement has been displayed to the user, and its score bonus counted
    // once achieved == true
    private float condition;
    private BiPredicate<Float, Float> predicate;

    Achievement( String title, String description, Float scoreBonus) {
        this.description = description;
        this.title = title;
        this.scoreBonus = scoreBonus;
        achieved = false;
        finished = false;
    }

    Achievement(String title, String description, Float scoreBonus, float condition, BiPredicate<Float, Float> predicate) {
        this.description = description;
        this.title = title;
        this.scoreBonus = scoreBonus;
        achieved = false;
        this.condition = condition;
        this.predicate = predicate;

    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Float getScoreBonus() {
        return scoreBonus;
    }

    public boolean isAchieved() {
        return achieved;
    }

    public void achieve() {
        achieved = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
    }

    public float getCondition() {
        return condition;
    }

    public void checkCondition(float value) {
        if (predicate.test(value, condition)) {
            achieve();
        }
    }
}
