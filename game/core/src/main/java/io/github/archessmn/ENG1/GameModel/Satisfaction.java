package io.github.archessmn.ENG1.GameModel;

import io.github.archessmn.ENG1.GameModel.Objects.*;

public class Satisfaction {

    World world;

    // The number of items in the Use enum, this value is used often, so it's stored to prevent repeated calculation.
    public final int useLength = Use.values().length;

    // Between 0 and 100 percent
    //
    // There are 4 factors to the score:
    // * Average building distances (40%) average distance for each pair of building types
    // * Completion (10%) Each counter being > 0 gives 2.5%
    // * Having a total number in the buildings counters, will unlock the full satisfactionScore
    //    For example, having 1 of each building counter may multiply the score by 0.1
    //    Whereas having 5 of each building counter may multiply the score by 1.
    // * Events (30%) Events will each have separate effects on this portion of satisfactionScore
    // * Building values (20%) Different buildings will have different values,
    //    e.g. rent price, this allows for more buildings to be implemented,
    //    and gives them a clear difference in how they effect satisfaction score.
    //    In other words, this is why a user may place accommodation building y,
    //    instead of accommodation building x.
    public float satisfactionScore;

    // Satisfaction score is the sum of the following 4 variables, allowing for easier access to each part of the score
    // This also means when one of these values needs to be reset, or altered, the satisfaction score will only update
    // on screen once when that calculation is complete.
    public float buildingDistancesScore;
    public float completionScore;
    public float eventsScore;
    public float buildingValuesScore;

    // Here the maximum value for each of these scores is set:

    public static final float BUILDING_DISTANCES_SCORE_CAP = 40;
    public static final float COMPLETION_SCORE_CAP = 10;
    public static final float EVENTS_SCORE_CAP = 30;
    public static final float BUILDING_VALUES_SCORE_CAP = 20;

    // Some of these scores need/have more variables to help calculate them:

    // These help with buildingDistancesScore



    // The map currently is 11x9 tiles
    private static final int GRID_WIDTH = 11;
    private static final int GRID_HEIGHT = 9;


    // The maximum possible distance between two buildings, is the diagonal distance 1 less in both x and y,
    // than the number of tiles on the map
    float maxDistance = (float) (Math.sqrt(Math.pow(GRID_WIDTH-1, 2) + Math.pow(GRID_HEIGHT-1, 2)));

    // This is how much of the satisfaction score each building use pair accounts for.
    // The number of undirected use pairs is of the form n + n-1 + n-2... + n-n, as we want the first use connected to
    // all uses, then the second needs to connect to all uses except the first, as that's already been counted, the
    // third use ignores the first and second, and so on. So we use the sum of 1 to n formula for this, which is
    // (n *(n+1)) / 2 We divide the total percent allowed for average distances (40) by this number
    float percentPerUsePair = BUILDING_DISTANCES_SCORE_CAP / (((float) useLength * ((float) useLength + 1)) / 2);

    private static final float THRESHOLD = 0.1f;

    // Allows the user to get the maximum satisfaction for a building use pair, if the pairs' average distance is
    // under 60% of the maximum possible distance. Anything over will give progressively less satisfaction.
    float maxScoreThreshold = maxDistance * THRESHOLD;


    // This 2D array stores the average distance between a pair of building types as an adjacency matrix
    // For example, you could set averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] to 0
    // However, averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] and
    // averageDistances[Use.TEACHING.ordinal()][Use.RECREATION.ordinal()] should hold the same value.
    public float[][] averageDistances = new float[useLength][useLength];

    // When adding a new building, it will need to multiply the old average distance by how many building pairs there
    // were, For example, if the previous average distance between a teaching and accommodation building
    // (5 + 3 + 2 + 6 + 9)/ 5 = 6 (5 teaching buildings 1 accommodation), and we add a new accommodation building,
    // it will need to add 5 new values to the average distance, as the accommodation building will have a distance to
    // each of the 5 teaching buildings. For this reason, it's helpful to keep a count of how many building pairs each
    // average distance is made up of, so that we can know what to multiply the average by, and then increment it and
    // divide the new total distance by the new number of building pairs. This value is stored in this 2D array:
    public int[][] averageDistancesCount = new int[useLength][useLength];

    // Stores the weight for each use pair, allowing different building use pairs to give a bigger or smaller bonus
    // than others.
    public float[][] weightMatrix = new float[useLength][useLength];

    public float[][] averageDistanceScores = new float[useLength][useLength];

    // These help with completionScore:


    // Defines how much satisfaction score is gained for having > 0 of each building use type.
    public float completionScorePerUse = COMPLETION_SCORE_CAP / useLength;



    /**
     * Constructor for the Satisfaction object.
     */
    public Satisfaction(World world) {
        this.world = world;
        initialiseWeightMatrix();
    }

    /**
     * When a building is built or demolished, this method calls the relevant functions to update satisfaction score.
     * @param building The building that was just built/demolished.
     * @param placed True if the building was built, false if it was demolished.
     */
    public void updateScore(BuildingObject building, boolean placed) {
        updateAverageDistances(building, placed);
        updateBuildingDistancesScore(building);
        updateCompletionScore();
        calculateSatisfactionScore();
    }

    /**
     * When an event needs to update the score, this method calls the relevant methods.
     */
    public void updateScore() {
        updateEventScore();
        calculateSatisfactionScore();
    }



    /**
     * Gets the satisfaction score value
     * @return The satisfactionScore variable.
     */
    public float getSatisfactionScore() {
        return satisfactionScore;
    }

    /**
     * Updates satisfactionScore by assigning it to the sum of its components.
     */
    public void calculateSatisfactionScore() {

        buildingDistancesScore = applyCap(buildingDistancesScore, BUILDING_DISTANCES_SCORE_CAP);
        completionScore = applyCap(completionScore, COMPLETION_SCORE_CAP);
        eventsScore = applyCap(eventsScore, EVENTS_SCORE_CAP);
        buildingValuesScore = applyCap(buildingValuesScore, BUILDING_VALUES_SCORE_CAP);

        // If no accommodation buildings are placed yet, the buildingDistancesScore is ignored.
        // (Since no one lives on campus to care about it)
        if (world.getBuildingUseCounts().get(Use.ACCOMMODATION) == 0) {
            satisfactionScore = completionScore + eventsScore + buildingValuesScore;
        }
        else {
            satisfactionScore = buildingDistancesScore + completionScore + eventsScore + buildingValuesScore;
        }
    }


    /**
     * Helper method to apply cap each component of satisfactionScore.
     * @param score the current score
     * @param cap the maximum allowed value for the score
     * @return the capped score
     */
    private float applyCap(double score, float cap) {
        return (float) Math.min(score, cap);
    }


    /**
     * Updates the average distance between each building use pair, this is further explained in the methods' comments,
     * and in the description of averageDistances.
     * @param building The building that was added to/removed from the map, its uses are iterated through to know which
     *                 building use pairs need to have their average distances updated.
     */
    public void updateAverageDistances(BuildingObject building, boolean placed) {
        // updateFactor is 1 if the building passed to this method was just placed, -1 if it was removed.

        // When it is 1, the distance found for each use pair is added to the corresponding averageDistance
        // index, the new total distance is divided by (numberOfPairs + 1) to find the new averageDistance, and the
        // corresponding averageDistancesCount is incremented by 1.

        // When it is -1, the distance found for each use pair is subtracted from the corresponding averageDistance
        // index, the new total distance is divided by (numberOfPairs - 1) to find the new averageDistance, and the
        // corresponding averageDistancesCount is decremented by 1.
        int updateFactor;
        if (placed) {
            updateFactor = 1;
        }
        else {
            updateFactor = -1;
        }

        // Iterates through each use the building passed to this method has
        for (Use use1 : building.getUses()) {
            // Iterates through all buildings currently placed on the map
            for (BuildingObject comparisonBuilding : world.getBuildings()) {
                // Distance is the diagonal distance between the building passed to this method, and the current
                // comparisonBuilding.
                float distance = (float) Math.sqrt(Math.pow(building.gridX - comparisonBuilding.gridX, 2) +
                    Math.pow(building.gridY - comparisonBuilding.gridY, 2));

                // Iterates through each use the comparison building has, except when the building passed to this method
                // and the comparison building are the same building.
                if (distance != 0) {
                    for (Use use2 : comparisonBuilding.getUses()) {
                        // The averageDistancesCount array stores average distances, this is calculated as each use
                        // count in the use pair multiplied together. (e.g., 5 teaching * 3 accommodation = 15 pairs).
                        // If we label each pair in the form T1A1 = distance between teaching building 1 and
                        // accommodation building 1, the average distance between all teaching and accommodation
                        // buildings would be: (T1A1 + T1A2 + T1A3 + T2A1 +...+ T5A3)/(5*3)

                        // numberOfPairs is the number of pairs the average distance between the two uses is made up of
                        // For example, if there were 4 teaching and 3 accommodation buildings before the building
                        // passed to this method was added/removed, then numberOfPairs would be 12.
                        int numberOfPairs;

                        // Update the average distance between the current pair of uses.
                        // If statement ensures that only the upper triangle of the adjacency matrix is actually
                        // updated, meaning use pairs are only ever updated and later checked, where the first use
                        // is <= to the second use. (based of the ordinal of the use)
                        if (use1.ordinal() < use2.ordinal()) {
                            numberOfPairs = averageDistancesCount[use1.ordinal()][use2.ordinal()];
                            averageDistances[use1.ordinal()][use2.ordinal()] = (averageDistances[use1.ordinal()][use2.ordinal()]
                                * numberOfPairs + distance * updateFactor) / (numberOfPairs + updateFactor);

                            // Update the number of building pairs used for the average distance between use1 and use2
                            averageDistancesCount[use1.ordinal()][use2.ordinal()] += updateFactor;
                        }
                        else {
                            numberOfPairs = averageDistancesCount[use2.ordinal()][use1.ordinal()];
                            averageDistances[use2.ordinal()][use1.ordinal()] = (averageDistances[use2.ordinal()][use1.ordinal()]
                                * numberOfPairs + distance * updateFactor) / (numberOfPairs + updateFactor);

                            // Update the number of building pairs used for the average distance between use2 and use1
                            averageDistancesCount[use2.ordinal()][use1.ordinal()] += updateFactor;
                        }
                    }
                }
            }
        }
    }


    /**
     * Updates the buildingDistancesScore when a building is added or deleted, assumes the building passed to this
     * method was successfully placed, or successfully deleted.
     * Also assumes that averageDistances has already been updated accordingly.
     *
     * @param building The building that was just placed or deleted.
     */
    public void updateBuildingDistancesScore(BuildingObject building) {
        for (Use use1 : building.getUses()) {
            for (Use use2 : Use.values()) {
                // Only iterates over the upper triangle of the adjacency matrix
                if (use2.ordinal() >= use1.ordinal()) {
                    // score is used to prevent duplicate calculation
                    float score = calculateScoreBonus(getWeight(use1, use2), use1, use2);
                    // Add the new score for this use pair, and subtract the previous score for this use pair
                    // from buildingDistancesScore
                    buildingDistancesScore += score - averageDistanceScores[use1.ordinal()][use2.ordinal()];
                    // Save the new score for this use pair so the above line will work when next the use pair is
                    // next updated.
                    averageDistanceScores[use1.ordinal()][use2.ordinal()] = score;
                }
                // Same logic is used as the if statement, but since only the upper triangle of the adjacency
                // matrices are used, use1 and use2 need to be swapped if use1 is larger than use2.
                else {
                    float score = calculateScoreBonus(getWeight(use2, use1), use2, use1);
                    buildingDistancesScore += score - averageDistanceScores[use2.ordinal()][use1.ordinal()];
                    averageDistanceScores[use2.ordinal()][use1.ordinal()] = score;
                }
            }
        }
    }


    /**
     * Calculates what portion of the maximum score bonus possible for the use pair passed is achieved, based on the
     * difference between the average distance for that pair, the maximum possible distance, and the threshold set.
     *
     * @param multiplier Allows the different pairs to be weighted differently, but requires careful tracking of all
     *                   multipliers, such that the score doesn't go over what is allowed.
     * @param use1 The first use of the use pair.
     * @param use2 The second use of the use pair.
     * @return The score bonus from the average distance between the use pair passed to this method.
     */
    public float calculateScoreBonus(float multiplier, Use use1, Use use2) {
        // Prevents adding to satisfaction score for buildings uses that aren't yet placed.
        if (world.getBuildingUseCounts().get(use1) == 0 || world.getBuildingUseCounts().get(use2) == 0) {
            return 0;
        }
        // If the distance between the use pair is under the maxScoreThreshold, the max satisfaction is given, as long
        // as the distance is greater than 0.
        else if (averageDistances[use1.ordinal()][use2.ordinal()] <= maxScoreThreshold) {
            return percentPerUsePair * multiplier;
        }
        // If the distance between the use pair is not under the maxScoreThreshold, then the score given is calculated
        // as follows. If the max score was 10, and the threshold 60%, anything between 6 and 10 gives progressively
        // less score. In this example, that would mean 7 gives 75% of the score, 8 gives 50%, 8.5 gives 62.5% etc.
        // To calculate this, we would do maxDistance - distance, divided by maxDistance - threshold, which would be
        // (10 - 7) / (10 - 6) = 3/4 = 0.75 (using distance as 7 here).
        else {
            return (percentPerUsePair * multiplier) *
                ((maxDistance - averageDistances[use1.ordinal()][use2.ordinal()]) / (maxDistance - maxScoreThreshold));
        }
    }


    /**
     * Initialises the weight matrix according to values selected by the developer(s).
     */
    private void initialiseWeightMatrix() {
        // This needs to be set manually, but doing so here avoids needing this if else statement to be used to find the
        // weight for a use pair, instead the weightMatrix can be referenced.
        for (Use use1 : Use.values()) {
            for (Use use2 : Use.values()) {
                // Only iterates over the upper triangle of the adjacency matrix
                if (use2.ordinal() >= use1.ordinal()) {
                    if (use1 == Use.TEACHING && use2 == Use.ACCOMMODATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    } else if (use1 == Use.TEACHING && use2 == Use.TEACHING) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    } else if (use1 == Use.ACCOMMODATION && use2 == Use.ACCOMMODATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    } else if (use1 == Use.ACCOMMODATION && use2 == Use.CAFETERIA) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 2.25f;
                    } else if (use1 == Use.ACCOMMODATION && use2 == Use.RECREATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.75f;
                    } else if (use1 == Use.CAFETERIA && use2 == Use.RECREATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 0.5f;
                    } else {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.0f; // Default weight
                    }
                }
            }
        }
    }


    /**
     * Allows for neater access to the weighting of a use pair, assumes order of the two uses was checked before being
     * passed to this method.
     * @param use1 The first use in the pair, should be smaller (listed earlier in the Use enum) than use2.
     * @param use2 The second use in the pair, should be larger (listed later in the Use enum) than use1.
     * @return The weighting associated with the use pair use1 --> use2.
     */
    private float getWeight(Use use1, Use use2) {
        return weightMatrix[use1.ordinal()][use2.ordinal()];
    }


    /**
     * For each building placed the completionScore is incremented by completionScorePerUse
     */
    public float updateCompletionScore() {

        // Reset completionScore to 0, and add the completionScorePerUse for each use > 0
        // Unfortunately this calculation cannot be skipped once the COMPLETION_SCORE_CAP is reached, as building
        // demolition allows the user to go back down to 0 for a building use.
        completionScore = 0;
        for (Use use : Use.values()) {
            if (world.getBuildingUseCounts().get(use) != 0) {
                completionScore += completionScorePerUse;
            }
        }
        return completionScore;
    }

    public void updateEventScore() {
        // Events
        if (world.getActiveEvents()[GameEvent.TreeHype.ordinal()] != null) {
            for (BuildingObject building : world.getBuildingsNearTerrain(TerrainObject.Feature.TREE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.ACCOMMODATION) {
                        eventsScore += 0.05f;
                    }
                }
            }
        }
        if (world.getActiveEvents()[GameEvent.LectureLake.ordinal()] != null) {
            for (BuildingObject building : world.getBuildingsNearTerrain(TerrainObject.Feature.LAKE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.TEACHING) {
                        eventsScore += 0.05f;
                    }
                }
            }
        }
        if (world.getActiveEvents()[GameEvent.RockClimbing.ordinal()] != null) {
            for (BuildingObject building : world.getBuildingsNearTerrain(TerrainObject.Feature.ROCK)) {
                for (Use use : building.getUses()) {
                    if (use == Use.ACCOMMODATION) {
                        eventsScore += 0.05f;
                    }
                }
            }
        }
        if (world.getActiveEvents()[GameEvent.AColdWinter.ordinal()] != null) {
            eventsScore *= 0.8f;
        }
        if (world.getActiveEvents()[GameEvent.LongBoiSighting.ordinal()] != null) {
            eventsScore = 0.3f; // This event is very powerful, but only lasts a short time
        }
        if (eventsScore > 0.3f)
            eventsScore = 0.3f;
    }

}
