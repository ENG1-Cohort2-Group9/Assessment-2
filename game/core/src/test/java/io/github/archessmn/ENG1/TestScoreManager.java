package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.ScoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestScoreManager {

    private final String TEST_FILE_NAME = "test_scores.txt";

    @BeforeEach
    public void setUp() {
        try {
            new FileOutputStream(TEST_FILE_NAME).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSaveScores() {
        ScoreManager.saveScore("Alice", 0f, TEST_FILE_NAME);
        ScoreManager.saveScore("Bob", 50f, TEST_FILE_NAME);
        ScoreManager.saveScore("Charlie", 30f, TEST_FILE_NAME);
        ScoreManager.saveScore("David", 100f, TEST_FILE_NAME);

        HashMap<String, Float> scores = ScoreManager.loadScores(TEST_FILE_NAME);
        assertNotNull(scores, "Scores was null");
        assertEquals(0f, scores.get("Alice"), "Incorrect score");
        assertEquals(50f, scores.get("Bob"), "Incorrect score");
        assertEquals(30f, scores.get("Charlie"), "Incorrect score");
        assertEquals(100f, scores.get("David"), "Incorrect score");
        assertEquals(4, scores.size(), "Incorrect number of entries");
    }

    @Test
    public void testSaveSameName() {
        ScoreManager.saveScore("Alice", 40f, TEST_FILE_NAME);
        ScoreManager.saveScore("Alice", 5f, TEST_FILE_NAME);

        HashMap<String, Float> scores = ScoreManager.loadScores(TEST_FILE_NAME);

        assertEquals(40f, scores.get("Alice"), "Wrong score was saved");
        assertEquals(1, scores.size(), "Wrong number of entries");
    }

    @Test
    public void testLoadScoresEmpty() {
        HashMap<String, Float> scores = ScoreManager.loadScores(TEST_FILE_NAME);

        assertEquals(0, scores.size(), "Score list was not empty");
    }

    @Test
    public void testGetTopScores() {
        ScoreManager.saveScore("Alice", 0f, TEST_FILE_NAME);
        ScoreManager.saveScore("Bob", 50f, TEST_FILE_NAME);
        ScoreManager.saveScore("Charlie", 30f, TEST_FILE_NAME);
        ScoreManager.saveScore("David", 100f, TEST_FILE_NAME);

        ArrayList<Map.Entry<String, Float>> topScores = ScoreManager.getTopScores(TEST_FILE_NAME);

        assertAll(
            "Scores were not in the correct order",
            () -> assertEquals(100f, topScores.get(0).getValue()),
            () -> assertEquals(50f, topScores.get(1).getValue()),
            () -> assertEquals(30f, topScores.get(2).getValue()),
            () -> assertEquals(0f, topScores.get(3).getValue()),

            () -> assertEquals("David", topScores.get(0).getKey()),
            () -> assertEquals("Bob", topScores.get(1).getKey()),
            () -> assertEquals("Charlie", topScores.get(2).getKey()),
            () -> assertEquals("Alice", topScores.get(3).getKey())
        );
    }
}
