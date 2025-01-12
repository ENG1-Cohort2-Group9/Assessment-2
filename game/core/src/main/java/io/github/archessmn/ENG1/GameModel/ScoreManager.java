package io.github.archessmn.ENG1.GameModel;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages how the scores are stored in scores.txt and how they are received out of the file.
 */
public class ScoreManager {

    /**
     * Save a user's score with the name into a file. If the name is already in the file, the higher score is kept
     * @param name The university name
     * @param score The satisfaction score, between 0.0 and 100.0
     * @param filePath The path (relative to assets/..) of the file to create or save to.
     */
    public static void saveScore(String name, float score, String filePath) {
        HashMap<String, Float> scores = loadScores(filePath);

        assert scores != null;
        // Assume all satisfaction scores are positive and replace any score with the same name with the maximum score.
        scores.put(name, Math.max(score, scores.getOrDefault(name, 0.0f)));

        // Output file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(scores);
            objectOutputStream.flush();
            objectOutputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns all the scores stored in the given file in descending order of score.
     * @param filePath The path (relative to assets/..) of the file to load from.
     * @return a list of Map.Entry<String, Float> where the String is the university name and the float is the
     * satisfaction score from 0.0 to 1.0
     */
    public static ArrayList<Map.Entry<String, Float>> getTopScores(String filePath) {
        HashMap<String, Float> scores = loadScores(filePath);

        // Sort scores descending
        assert scores != null;
        ArrayList<Map.Entry<String, Float>> list = new ArrayList<>(scores.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Collections.reverse(list);

        return list;
    }

    /**
     * Load the HashMap of scores from the file
     * @param filePath The path (relative to assets/..) of the file to load from.
     * @return A HashMap of names : scores
     */
    @SuppressWarnings("unchecked") // Try catch will prevent errors if the file is incorrect
    public static HashMap<String, Float> loadScores(String filePath) {
        HashMap<String, Float> scores;

        // Read file
        try {
            FileInputStream fileInput = new FileInputStream(filePath);

            ObjectInputStream objectInput = new ObjectInputStream(fileInput);

            scores = (HashMap<String, Float>)objectInput.readObject();

            objectInput.close();
            fileInput.close();
        }
        catch (IOException ioException) {
            scores = new HashMap<>();
        }
        catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
            return null;
        }

        return scores;
    }
}
