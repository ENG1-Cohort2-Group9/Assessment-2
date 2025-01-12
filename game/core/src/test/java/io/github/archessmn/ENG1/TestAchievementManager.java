package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class TestAchievementManager {

    // The instance of AchievementManager being used for testing.
    AchievementManager achievementManager;
    // A mock instance of World, used as AchievementManager takes World as a parameter, but it's not needed for testing.
    World mockWorld;

    @BeforeEach
    public void setUp() {
        mockWorld = new World(100, 100, new GameEventHandler[]{}, null);
        achievementManager = new AchievementManager(mockWorld, null);
    }

    @Test
    public void testUpdateThreeMinScoreAvg_firstThreeMinutes() {
        // Add some scores
        achievementManager.updateThreeMinScoreAvg(10, 1);
        achievementManager.updateThreeMinScoreAvg(20, 2);
        achievementManager.updateThreeMinScoreAvg(30, 3);

        // delta of 0.01 prevents false negative with floating-point precision errors.
        assertEquals((10 + 20 + 30) / 3.0, achievementManager.getScoreThreeMinAverage(), 0.01, "The average score for the first 3 minutes is not updating/being calculated correctly.");
    }


    @Test
    public void testUpdateThreeMinScoreAvg_atThreeMins() {
        // This does test with scores over 100, but there are no checks for that in AchievementManager, as that is the
        // responsibility of Satisfaction.
        for (int i = 1; i <= 180; i++) {
            achievementManager.updateThreeMinScoreAvg(i, i);
        }
        assertEquals((180f * (180+1)/2) / 180, achievementManager.getScoreThreeMinAverage(), 0.01, "The average score for the first 180 seconds is not updating/being calculated correctly.");
    }


    @Test
    public void testUpdateThreeMinScoreAvg_afterThreeMins() {
        for (int i = 1; i <= 400; i++) {
            achievementManager.updateThreeMinScoreAvg(i, i);
        }
        // The expected result should be the average of the last 180 scores added. Which in this test is score entries
        // 221 to 400.
        assertEquals((400f * (400+1)/2 - 220f * (220+1)/2) / 180, achievementManager.getScoreThreeMinAverage(), 0.01, "The average score for the most recent 180 seconds is not updating/being calculated correctly.");
    }

}


