package io.github.archessmn.ENG1.GameModel;

import java.util.LinkedList;
import java.util.Queue;
import static io.github.archessmn.ENG1.GameModel.Achievement.*;


public class AchievementManager {
    // Stores which achievements have been met this game, uses the Achievement enum ordinal values for accessing.
    private final Achievement[] achievements;

    private final World world;

    private float totalScoreBonus;

    // Stores the average score for the last 3 minutes
    private float scoreThreeMinAverage;

    private Queue<Float> scoreQueue;
    private float scoreThreeMinTotal;

    private int time;

    private int buildingsBuilt;
    private int buildingsDemolished;
    private int pauseCount;

    private boolean fullMap;

    public AchievementManager(World world) {
        this.world = world;
        // Gets the list of all achievements in the Achievement enum.
        achievements = Achievement.values();
        time = (int) Math.floor(world.getCurrentTime());

        scoreQueue = new LinkedList<>();
        scoreThreeMinTotal = 0;
        scoreThreeMinAverage = 0;
        totalScoreBonus = 0;
    }


    public void updateAchievements(int currentTime) {
        float[] scores = world.getSatisfaction().getSatisfactionScoreBreakdown();
        float[] scoreCaps = world.getSatisfaction().getSatisfactionScoreCaps();
        // Allows for checks once per second
        if (currentTime > time) {
            time = currentTime;
            updateThreeMinScoreAvg(world.getSatisfaction().getSatisfactionScore(), time);
            getAchievement(OVER_EIGHTY).checkCondition(scoreThreeMinAverage);
        }

        // Checks here happen everytime updateAchievements is called.
        getAchievement(EVENTFUL).checkCondition((scores[2] / scoreCaps[2]) * 100);
        if (world.isMapFull()) {
            getAchievement(JAM_PACKED).achieve();
        }


        // Any achievements that were achieved in the above checks, are displayed, and isFinished is set to true, this
        // prevents achievements from showing multiple times.
        for (Achievement achievement : achievements) {
            if (achievement.isAchieved() && !achievement.isFinished()) {
                System.out.println(achievement.getTitle() + ": " + achievement.getDescription());
                achievement.finish();
                totalScoreBonus += achievement.getScoreBonus();
            }
        }

        if (world.getGameEnded()) {
            // [DISTANCES.ordinal()].checkCondition(satisfaction.getSatisfactionScoreBreakdown()[0]);
            // [COMPLETION.ordinal()].checkCondition(satisfaction.getSatisfactionScoreBreakdown()[1]);
            if (world.getSatisfaction().checkExactCapacity()) {
                // achievements[FULL_CAPACITY.ordinal()].achieve();
            }
            // achievements[MINIMALIST.ordinal()].checkCondition((buildingsBuilt));
            // achievements[DESTRUCTION.ordinal()].checkCondition((buildingsDemolished));
            // achievements[BUILDER.ordinal()].checkCondition((buildingsBuilt));
            // achievements[PLANNING.ordinal()].checkCondition((pauseCount));
            for (Achievement achievement : achievements) {
                if (achievement.isAchieved() && !achievement.isFinished()) {
                    totalScoreBonus += achievement.getScoreBonus();
                    System.out.println(achievement.getTitle() + ": " + achievement.getDescription());
                    achievement.finish();
                }
            }
        }
    }


    public void updateThreeMinScoreAvg(float score, int time) {
        if (time <= 180) {
            scoreQueue.add(score);
            scoreThreeMinTotal += score;
            scoreThreeMinAverage = scoreThreeMinTotal / time;
        }
        else {
            float removed = scoreQueue.remove();
            scoreQueue.add(score);
            scoreThreeMinTotal += score - removed;
            scoreThreeMinAverage = scoreThreeMinTotal / 180;
        }
    }


    /**
     * Takes the static name of an achievement, and returns the instance of that achievement stored in achievements.
     * @param achievement The achievement being fetched
     * @return The instance of the achievement passed to the method, that is stored in achievements.
     */
    private Achievement getAchievement(Achievement achievement) {
        return achievements[achievement.ordinal()];
    }


    public float getTotalScoreBonus() {
        return totalScoreBonus;
    }

    public void incrementBuildingsBuilt() {
        buildingsBuilt += 1;
    }

    public void incrementBuildingsDemolished() {
        buildingsDemolished += 1;
    }

    public void incrementPauseCount() {
        pauseCount += 1;
    }


}
