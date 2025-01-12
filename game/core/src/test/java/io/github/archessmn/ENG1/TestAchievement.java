package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.github.archessmn.ENG1.GameModel.Achievement.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestAchievement {

    Achievement[] achievements;
    static Satisfaction mockSatisfaction;
    static float[] scoreCaps;

    @BeforeAll
    public static void setCaps() {
        // Creates a mock instance of Satisfaction.
        mockSatisfaction = new Satisfaction(new World(100, 100, new GameEventHandler[] {}, null));
        scoreCaps = mockSatisfaction.getSatisfactionScoreCaps();
    }


    @BeforeEach
    public void setUp() {
        achievements = Achievement.values();
    }


    @Test
    public void testCheckCondition_greaterThanIsTrue() {
        achievements[OVER_EIGHTY.ordinal()].checkCondition(90);
        assertTrue(achievements[OVER_EIGHTY.ordinal()].isAchieved(), "OVER_EIGHTY was set to false when it should not have been");
    }


    @Test
    public void testCheckCondition_greaterThanIsEquals() {
        achievements[OVER_EIGHTY.ordinal()].checkCondition(80);
        assertFalse(achievements[OVER_EIGHTY.ordinal()].isAchieved(), "OVER_EIGHTY was set to true when it should not have been");
    }

    @Test
    public void testCheckCondition_greaterThanIsFalse() {
        achievements[OVER_EIGHTY.ordinal()].checkCondition(70);
        assertFalse(achievements[OVER_EIGHTY.ordinal()].isAchieved(), "OVER_EIGHTY was set to true when it should not have been");
    }


    @Test
    public void testCheckCondition_lessThanIsTrue() {
        achievements[COMPLETION.ordinal()].checkCondition(((scoreCaps[1] - 5) / scoreCaps[1]) * 100);
        assertTrue(achievements[COMPLETION.ordinal()].isAchieved(), "COMPLETION was set to false when it should not have been");
    }


    @Test
    public void testCheckCondition_lessThanIsEquals() {
        achievements[COMPLETION.ordinal()].checkCondition(100);
        assertFalse(achievements[COMPLETION.ordinal()].isAchieved(), "COMPLETION was set to true when it should not have been");
    }


    @Test
    public void testCheckCondition_lessThanIsFalse() {
        // Completion score can't actually go over 100, but this test is added in case a later achievement where the
        // value given can be larger than the condition is added.
        achievements[COMPLETION.ordinal()].checkCondition(((scoreCaps[1] + 5) + 7 / scoreCaps[1]) * 100);
        assertFalse(achievements[COMPLETION.ordinal()].isAchieved(), "COMPLETION was set to true when it should not have been");
    }


    @Test
    public void testCheckCondition_equalsIsUnder() {
        achievements[MINIMALIST.ordinal()].checkCondition(4);
        assertFalse(achievements[MINIMALIST.ordinal()].isAchieved(), "MINIMALIST was set to true when it should not have been");
    }


    @Test
    public void testCheckCondition_equalsIsTrue() {
        achievements[MINIMALIST.ordinal()].checkCondition(5);
        assertTrue(achievements[MINIMALIST.ordinal()].isAchieved(), "MINIMALIST was set to false when it should not have been");
    }


    @Test
    public void testCheckCondition_equalsIsOver() {
        achievements[MINIMALIST.ordinal()].checkCondition(6);
        assertFalse(achievements[MINIMALIST.ordinal()].isAchieved(), "MINIMALIST was set to true when it should not have been");
    }



}
