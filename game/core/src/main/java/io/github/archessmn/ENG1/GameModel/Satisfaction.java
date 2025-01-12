package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.*;


import static io.github.archessmn.ENG1.GameModel.GridUtils.*;
import static io.github.archessmn.ENG1.GameModel.Objects.Use.*;
import static io.github.archessmn.ENG1.GameModel.GameEvent.*;
import static io.github.archessmn.ENG1.GameModel.Objects.TerrainObject.Feature.*;
import static io.github.archessmn.ENG1.GameModel.Objects.TerrainObject.Feature;
import static io.github.archessmn.ENG1.GameModel.World.*;

public class Satisfaction {

    public final World world;

    // The number of items in the Use enum, this value is used often, so it's stored to prevent repeated calculation.
    public static final int USE_LENGTH = Use.values().length;

    // Between 0 and 100 percent
    //
    // There are 4 factors to the score:
    // * Average building distances (40%) average distance for each pair of building types
    // * Completion (10%) Each counter being > 0 gives 2.5%
    // * Events (30%) Events will each have separate effects on this portion of satisfactionScore
    // * Building Capacities (20%) Different buildings have different capacities, the player needs to ensure there is
    // * enough room for all the students on the campus.

    // The final score is also then multiplied depending on whether the total number of buildings placed is within a
    // certain range.
    private float satisfactionScore;

    // Satisfaction score is the sum of the following 4 variables, allowing for easier access to each part of the score
    // This also means when one of these values needs to be reset, or altered, the satisfaction score will only update
    // on screen once when that calculation is complete.
    private float buildingDistancesScore;
    private float completionScore;
    private float eventsScore; // This value can go below 0 and above the cap
    private float buildingCapacityScore;

    // Here the maximum value for each of these scores is set:

    private static final float BUILDING_DISTANCES_SCORE_CAP = 40;
    private static final float COMPLETION_SCORE_CAP = 10;
    private static final float EVENTS_SCORE_CAP = 30;
    private static final float BUILDING_CAPACITY_SCORE_CAP = 20;


    // The coverage goal is the target for what % of the map should have a building on it.
    public static final float MAP_COVERAGE_GOAL = 0.25f;
    // The coverage allowance gives a set leeway for the coverage, so that getting the maximum satisfaction isn't
    // practically impossible.
    public static final float MAP_COVERAGE_ALLOWANCE = 0.05f;
    // The lower limit, is the lowest number of tiles required to be filled with a building, for the user to be able to
    // get the maximum satisfaction score.
    public static final int LOWER_BUILDING_LIMIT = (int) ((MAP_COVERAGE_GOAL - MAP_COVERAGE_ALLOWANCE) * GRID_WIDTH * GRID_HEIGHT);
    // The upper limit, is the highest number of tiles allowed to be filled with a building, for the user to be able to
    // get the maximum satisfaction score.
    public static final int UPPER_BUILDING_LIMIT = MathUtils.ceil((MAP_COVERAGE_GOAL + MAP_COVERAGE_ALLOWANCE) *
                                                                        GRID_WIDTH * GRID_HEIGHT);
    // The lower limit discourages players from placing one singular cluster of buildings, while the upper limit
    // discourages the player from placing buildings in as many tiles as possible.

    // Used with the multiplier to effect how strong the multiplier is.
    public static final float BALANCE_FACTOR = 0.25f;


    // Some of these scores need/have more variables to help calculate them:

    // These help with buildingDistancesScore

    // The maximum possible distance between two buildings, is the diagonal distance 1 less in both x and y,
    // than the number of tiles on the map
    public static final float maxDistance = (float) (Math.sqrt(Math.pow(GRID_WIDTH-1, 2) + Math.pow(GRID_HEIGHT-1, 2)));

    // This is how much of the satisfaction score each building use pair accounts for.
    // The number of undirected use pairs is of the form n + n-1 + n-2... + n-n, as we want the first use connected to
    // all uses, then the second needs to connect to all uses except the first, as that's already been counted, the
    // third use ignores the first and second, and so on. So we use the sum of 1 to n formula for this, which is
    // (n *(n+1)) / 2 We divide the BUILDING_DISTANCES_SCORE_CAP by this number
    public static final float percentPerUsePair = BUILDING_DISTANCES_SCORE_CAP / (((float) USE_LENGTH * ((float) USE_LENGTH + 1)) / 2);

    public static final float THRESHOLD = 0.4f;

    // Allows the user to get the maximum satisfaction for a building use pair, if the pairs' average distance is
    // under 60% of the maximum possible distance. Anything over will give progressively less satisfaction.
    public static final float maxScoreThreshold = maxDistance * THRESHOLD;


    // This 2D array stores the average distance between a pair of building types as an adjacency matrix
    // For example, you could set averageDistances[RECREATION.ordinal()][TEACHING.ordinal()] to 0
    // However, averageDistances[RECREATION.ordinal()][TEACHING.ordinal()] and
    // averageDistances[TEACHING.ordinal()][RECREATION.ordinal()] should hold the same value.
    private float[][] averageDistances = new float[USE_LENGTH][USE_LENGTH];

    // When adding a new building, it will need to multiply the old average distance by how many building pairs there
    // were, For example, if the previous average distance between a teaching and accommodation building
    // (5 + 3 + 2 + 6 + 9)/ 5 = 6 (5 teaching buildings 1 accommodation), and we add a new accommodation building,
    // it will need to add 5 new values to the average distance, as the accommodation building will have a distance to
    // each of the 5 teaching buildings. For this reason, it's helpful to keep a count of how many building pairs each
    // average distance is made up of, so that we can know what to multiply the average by, and then increment it and
    // divide the new total distance by the new number of building pairs. This value is stored in this 2D array:
    private int[][] averageDistancesCount = new int[USE_LENGTH][USE_LENGTH];

    // Stores the weight for each use pair, allowing different building use pairs to give a bigger or smaller bonus
    // than others.
    private float[][] weightMatrix = new float[USE_LENGTH][USE_LENGTH];

    private float[][] averageDistanceScores = new float[USE_LENGTH][USE_LENGTH];

    // These help with completionScore:


    // Defines how much satisfaction score is gained for having > 0 of each building use type.
    public final float completionScorePerUse = COMPLETION_SCORE_CAP / USE_LENGTH;

    // These help with eventsScore:

    private float cappedEventsScore; // This is equal to eventsScore with its range restricted

    // These help with buildingValuesScore:

    // Stores the total capacities for each building use, where the 1st value in the Use enum (TEACHING as of writing),
    // Is stored as the first value in useCapacities. i.e. the total capacity of teaching buildings
    private int[] useCapacities = new int[USE_LENGTH];


    /**
     * Constructor for the Satisfaction object.
     */
    public Satisfaction(World world) {
        this.world = world;
        initialiseWeightMatrix();
    }

    /**
     * When a mapObject is built or demolished, this method calls the relevant functions to update satisfaction score.
     * @param mapObject The mapObject that was just built/demolished.
     * @param placed True if the mapObject was built, false if it was demolished.
     */
    public void updateScore(MapObject mapObject, boolean placed) {
        // updateFactor the integer equivalent of placed, it is explained in each of the methods using it.
        int updateFactor = -1;
        if (placed) {updateFactor = 1;}
        if (mapObject instanceof BuildingObject building) {
            updateAverageDistances(building, updateFactor);
            updateBuildingDistancesScore(building);
            updateCompletionScore(updateFactor);
            updateUseCapacities(building, updateFactor);
            calculateBuildingCapacityScore();
        }

        updateEventScore(mapObject, placed);
        calculateSatisfactionScore();
    }

    /**
     * When an event needs to update the score, this method calls the relevant methods.
     * @param activeEvent The event in question
     * @param added True if the activeEvent has been added, false if removed
     */
    public void updateScore(GameEvent activeEvent, boolean added) {
        updateEventScore(activeEvent, added);
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
     * Gets the satisfaction score break-down
     * Return order: [0] Building Distance | [1] Completion | [2] Events | [3] Building Values
     * @return An array with the 4 summary variables.
     */
    public float[] getSatisfactionScoreBreakdown() {
        return new float[] { buildingDistancesScore, completionScore, cappedEventsScore, buildingCapacityScore };
    }

    /**
     * Gets the cap for all satisfaction scores
     * Return order: [0] Building Distance | [1] Completion | [2] Events | [3] Building Values
     * @return An array with the 4 summary variable caps.
     */
    public float[] getSatisfactionScoreCaps() {
        return new float[] { BUILDING_DISTANCES_SCORE_CAP, COMPLETION_SCORE_CAP, EVENTS_SCORE_CAP, BUILDING_CAPACITY_SCORE_CAP};
    }

    /**
     * Updates satisfactionScore by assigning it to the sum of its components.
     */
    public void calculateSatisfactionScore() {

        buildingDistancesScore = MathUtils.clamp(buildingDistancesScore, 0f, BUILDING_DISTANCES_SCORE_CAP);
        completionScore = MathUtils.clamp(completionScore, 0f, COMPLETION_SCORE_CAP);
        cappedEventsScore = MathUtils.clamp(eventsScore, 0f, EVENTS_SCORE_CAP);
        buildingCapacityScore = MathUtils.clamp(buildingCapacityScore, 0f, BUILDING_CAPACITY_SCORE_CAP);

        // If no accommodation buildings are placed, the buildingDistancesScore and buildingCapacityScore are ignored.
        // (Since no one lives on campus to care about them)
        if (world.getBuildingUseCount(ACCOMMODATION) == 0) {
            satisfactionScore = completionScore + cappedEventsScore;
        }
        else {
            satisfactionScore = buildingDistancesScore + completionScore + cappedEventsScore + buildingCapacityScore;
        }

        int number_of_buildings = world.getBuildings(true).size;

        satisfactionScore *= calculateMultiplier(number_of_buildings);

    }

    /**
     * The final satisfactionScore is multiplied relative to the amount of buildings expected on the map, as shown
     * in the 2 examples below.
     * <p>
     * Example 1: lower limit = 30, upper limit = 40, buildings placed = 25, Balance = 0.25:
     * Multiplier = min(1-((30-25)/30)*0.25,1-((25-40)/40)*0.25) = min(1-1/24,1--3/32) = min(23/24,35/32) = 23/24
     * Example 2: lower limit = 30, upper limit = 40, buildings placed = 60, Balance = 0.25:
     * Multiplier = min(1-((30-60)/30)*0.25,1-((60-40)/40)*0.25) = min(1--1/4,1-1/8) = min(5/4, 7/8) = 7/8
     * @param number_of_buildings The number of buildings currently built on the map.
     * @return The calculated multiplier to be used on the overall score.
     */
    public float calculateMultiplier(int number_of_buildings) {
        if (number_of_buildings >= LOWER_BUILDING_LIMIT && number_of_buildings <= UPPER_BUILDING_LIMIT ) {
            return 1;
        }
        return Math.min(1 - (((float)(LOWER_BUILDING_LIMIT - number_of_buildings) / LOWER_BUILDING_LIMIT)* BALANCE_FACTOR),
            1 - (((float)(number_of_buildings - UPPER_BUILDING_LIMIT) / UPPER_BUILDING_LIMIT))* BALANCE_FACTOR);
    }


    /**
     * Updates the average distance between each building use pair, this is further explained in the methods' comments,
     * and in the description of averageDistances.
     * @param building The building that was added to/removed from the map, its uses are iterated through to know which
     *                 building use pairs need to have their average distances updated.
     * @param updateFactor is 1 if the building passed to this method was just placed, -1 if it was removed.
     * <p>
     *                     When it is 1, the distance found for each use pair is added to the corresponding
     *                     averageDistance index, the new total distance is divided by (numberOfPairs + 1) to find the
     *                     new averageDistance, and the corresponding averageDistancesCount is incremented by 1.
     * <p>
     *                     When it is -1, the distance found for each use pair is subtracted from the corresponding
     *                     averageDistance index, the new total distance is divided by (numberOfPairs - 1) to find the
     *                     new averageDistance, and the corresponding averageDistancesCount is decremented by 1.
     */
    public void updateAverageDistances(BuildingObject building, int updateFactor) {
        // Iterates through each use the building passed to this method has
        for (Use use1 : building.getUses()) {
            // Iterates through all buildings currently placed on the map
            for (BuildingObject comparisonBuilding : world.getBuildings(true)) {
                // Iterates through each use the comparison building has, except when the building passed to this method
                // and the comparison building are the same building.
                if (building.getSnappedScreenPosition().x != comparisonBuilding.getSnappedScreenPosition().x ||
                    building.getSnappedScreenPosition().y != comparisonBuilding.getSnappedScreenPosition().y) {
                    // Distance is the diagonal distance between the building passed to this method, and the current
                    // comparisonBuilding.
                    float distance = (float) Math.sqrt(Math.pow(building.getGridCoords().x - comparisonBuilding.getGridCoords().x, 2) +
                        Math.pow(building.getGridCoords().y - comparisonBuilding.getGridCoords().y, 2));
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
                        // is <= to the second use. (based off the ordinal of the use)
                        if (use1.ordinal() < use2.ordinal()) {
                            numberOfPairs = averageDistancesCount[use1.ordinal()][use2.ordinal()];
                            if (numberOfPairs + updateFactor > 0) {
                                averageDistances[use1.ordinal()][use2.ordinal()] = (averageDistances[use1.ordinal()][use2.ordinal()]
                                    * numberOfPairs + distance * updateFactor) / (numberOfPairs + updateFactor);
                                // Update the number of building pairs used for the average distance between use1 and use2
                                averageDistancesCount[use1.ordinal()][use2.ordinal()] += updateFactor;
                            }
                            else {
                                averageDistances[use1.ordinal()][use2.ordinal()] = 0;
                                averageDistancesCount[use1.ordinal()][use2.ordinal()] = 0;
                            }
                        }
                        else {
                            numberOfPairs = averageDistancesCount[use2.ordinal()][use1.ordinal()];
                            if (numberOfPairs + updateFactor > 0) {
                                averageDistances[use2.ordinal()][use1.ordinal()] = (averageDistances[use2.ordinal()][use1.ordinal()]
                                    * numberOfPairs + distance * updateFactor) / (numberOfPairs + updateFactor);
                                // Update the number of building pairs used for the average distance between use2 and use1
                                averageDistancesCount[use2.ordinal()][use1.ordinal()] += updateFactor;
                            }
                            else {
                                averageDistances[use2.ordinal()][use1.ordinal()] = 0;
                                averageDistancesCount[use2.ordinal()][use1.ordinal()] = 0;
                            }
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
                // Same logic is used as above, but since only the upper triangle of the adjacency
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
        // Prevents adding to satisfaction score for buildings uses that aren't yet placed, or for the distance between
        // buildings of the same use, when only building with that use is placed down.
        if (world.getBuildingUseCount(use1) == 0 || world.getBuildingUseCount(use2) == 0 ||
            (use1 == use2 && world.getBuildingUseCount(use1) == 1)) {
            return 0;
        }

        // If the distance between the use pair is under the maxScoreThreshold, the max satisfaction is given.
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
                    if (use1 == ACCOMMODATION && use2 == ACCOMMODATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    }
                    else if (use1 == TEACHING && use2 == TEACHING) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.25f;
                    }
                    else if (use1 == use2) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 0f;
                    }
                    else if (use1 == TEACHING && use2 == ACCOMMODATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.25f;
                    }
                     else if (use1 == ACCOMMODATION && use2 == CAFETERIA) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 2.25f;
                    }
                     else if (use1 == ACCOMMODATION && use2 == RECREATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    }
                     else if (use1 == CAFETERIA && use2 == RECREATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 0.25f;
                    }
                    else {
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
    public float getWeight(Use use1, Use use2) {
        return weightMatrix[use1.ordinal()][use2.ordinal()];
    }


    /**
     * For each useCount > 0 the completionScore is incremented by completionScorePerUse
     * @param updateFactor Used to skip recalculation when a building was added, and completionScore is already maxed.
     */
    public void updateCompletionScore(int updateFactor) {
        // If a building was deleted, or completionScore isn't currently maxed, then it is recalculated.
        // But if completionScore is maxed, and a building was added, then it must still be maxed, so the calculation
        // can be skipped.
        if (completionScore < COMPLETION_SCORE_CAP || updateFactor == -1) {
            // Reset completionScore to 0, and add the completionScorePerUse for each use > 0
            completionScore = 0;
            for (Use use : Use.values()) {
                if (world.getBuildingUseCount(use) != 0) {
                    completionScore += completionScorePerUse;
                }
            }
        }
    }


    /**
     * Gets the satisfaction score bonus for an active event in which buildings of use {@code use} near any of a group
     * of terrain features adds +{@code scoreBonus} for each. If the event is not active, the result will be 0.
     * @param mapObject The mapObject that has just been placed or removed
     * @param wasPlaced True if the mapObject has been placed, false if removed
     * @param event The event in question.
     * @param features The features the building type must be near to produce a bonus. If a building is near multiple
     *                 terrain features, the satisfaction is increased for each one.
     * @param use The type of building that will provide the bonus.
     * @param scoreBonus The bonus to add for each building satisfying the conditions. The overall maximum event score is
     *                   {@value EVENTS_SCORE_CAP}
     * @return The total satisfaction score increase.
     */
    private float getEventScoreBonus(MapObject mapObject, boolean wasPlaced, GameEvent event, Feature[] features, Use use, float scoreBonus) {
        float total = 0;
        if (world.hasActiveEvent(event)) {
            if (mapObject instanceof BuildingObject building) {
                for (Feature feature : features) {
                    if (building.hasUse(use) && world.isBuildingNearTerrain(building, feature)) {
                        total += wasPlaced ? scoreBonus : -scoreBonus;
                    }
                }
            } else if (mapObject instanceof TerrainObject terrain) {
                Array<MapObject> neighbours = world.getMapObjectsAroundPosition(terrain.getGridCoords());
                for (MapObject neighbour : neighbours) {
                    if (neighbour instanceof BuildingObject building && building.hasUse(use)) {
                        total += wasPlaced ? scoreBonus : -scoreBonus;
                    }
                }
            }
        }
        return total;
    }

    /**
     * Updates the stored event score. The aim is that only actions taken while an event is active can affect the score.
     * For instance, if the event TreeHype occurs, a permanent bonus can be gained by placing buildings near trees during
     * that time. If these buildings are later removed, the bonus is not affected
     * @param mapObject The mapObject that has just been placed or removed
     * @param wasPlaced True if the mapObject has been placed, false if removed
     */
    public void updateEventScore(MapObject mapObject, boolean wasPlaced) {
        // Events
        eventsScore += getEventScoreBonus(mapObject, wasPlaced, TREE_HYPE, new Feature[]{TREE}, ACCOMMODATION, 1f );
        eventsScore += getEventScoreBonus(mapObject, wasPlaced, LECTURE_VIEW, new Feature[]{TREE, LAKE}, TEACHING, 0.25f );
        eventsScore += getEventScoreBonus(mapObject, wasPlaced, ROCK_CLIMBING, new Feature[]{ROCK}, ACCOMMODATION, 1f );

        float gymHypeBonus = 0.5f;
        if (world.hasActiveEvent(GYM_HYPE) && mapObject instanceof BuildingObject building && building.getType() == BuildingName.GYM) {
            eventsScore += wasPlaced ? gymHypeBonus : -gymHypeBonus;
        }

    }

    /**
     * Apply a one-time update to the event score when the {@code activeEvent} occurs
     * @param wasAdded True if the event has just occurred, false if it has just been removed
     */
    public void updateEventScore(GameEvent activeEvent, boolean wasAdded) {
        float debuffPerBuilding = 2f;
        if (wasAdded) {
            switch (activeEvent) {
                case TOURNAMENT_WON -> eventsScore += 7.5f;
                case TOO_MANY_BUILDINGS ->
                    eventsScore -= ((world.getBuildingUseCount(TEACHING) - TOO_MANY_LECTURE_BUILDINGS) * debuffPerBuilding);
                case LONG_BOI_SIGHTING -> eventsScore += EVENTS_SCORE_CAP;
            }
        } else {
            switch (activeEvent) {
                case LONG_BOI_SIGHTING -> eventsScore -= EVENTS_SCORE_CAP;
            }
        }
    }


    /**
     * Calculates the buildingCapacityScore using the average capacity of all non ACCOMMODATION uses, and comparing that
     * to the ACCOMMODATION capacity.
     */
    public void calculateBuildingCapacityScore() {
        // The number of students attending the university.
        int students = useCapacities[ACCOMMODATION.ordinal()];
        // Set the averageCapacity to students negated, as it will be added back on in the for loop.
        float averageCapacity = -1 * students;
        for (int capacity : useCapacities) {
            // Adds the minimum value between capacity and students, prevents excess capacity for one use benefiting
            // the score.
            averageCapacity += Math.min(capacity, students);
        }
        // This gives the average capacity of the building uses, ignoring the accommodation capacity.
        averageCapacity /= Use.values().length - 1;

        // Sets buildingCapacityScore to its cap if there is enough space for the students
        if (averageCapacity < students) {
            buildingCapacityScore = BUILDING_CAPACITY_SCORE_CAP * (averageCapacity / students);
        }
        else if (students != 0){
            buildingCapacityScore = BUILDING_CAPACITY_SCORE_CAP;
        }
    }


    /**
     * Updates the total capacity for each use the building passed to this method has.
     * @param building The building just added or removed from the map.
     * @param updateFactor 1 if the building was added to the map, -1 if it was removed. This toggles whether the
     *                     capacity is added or subtracted from the total capacity for each specific use.
     */
    public void updateUseCapacities(BuildingObject building, int updateFactor) {
        for(Use use : building.getUses()) {
            useCapacities[use.ordinal()] += updateFactor * building.getUseCapacity(use);
        }
    }


    /**
     * Checks whether the capacity for each building use is exactly equal to the others, this is used to check if the
     * player has achieved the FULL_CAPACITY achievement.
     * @return true if all capacities are equal, false if not, also false if there is 0 accommodation buildings.
     */
    public boolean checkExactCapacity() {
        int[] exactCapacities = new int[USE_LENGTH];
        for (BuildingObject building : world.getBuildings()) {
            for (Use use : building.getUses()) {
                exactCapacities[use.ordinal()] += building.getUseCapacity(use);
            }
        }
        // The number of students attending the university.
        int students = useCapacities[ACCOMMODATION.ordinal()];
        if (students == 0) {
            return false;
        }
        for (int capacity : useCapacities) {
            if (students != capacity) {
                return false;
            }
        }
        return true;
    }


    public float[][] getAverageDistanceScores() {
        return averageDistanceScores;
    }

    public int[][] getAverageDistancesCount() {
        return averageDistancesCount;
    }

    public float[][] getAverageDistances() {
        return averageDistances;
    }

    public float[][] getWeightMatrix() {
        return weightMatrix;
    }
}
