package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Buildings.Building;
import io.github.archessmn.ENG1.GameModel.Buildings.*;

import java.util.Arrays;
import java.util.HashMap;

import static io.github.archessmn.ENG1.Interface.GameScreen.VIEWPORT_HEIGHT;
import static io.github.archessmn.ENG1.Interface.GameScreen.VIEWPORT_WIDTH;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public Integer width, height;

    public Array<Building> buildings;

    public HashMap<Use, Integer> buildingUseCounts = new HashMap<>();

    // Between 0 and 100 percent
    // There are n factors to the score:
    // Average building distances (40%) average distance for each pair of building types
    // Completion (10%) Each counter being > 0 gives 2.5%
    // Having a total number in the buildings counters, will unlock the full satisfactionScore
    // For example, having 1 of each building counter may multiply the score by 0.1
    // Whereas having 5 of each building counter may multiply the score by 1.
    // Events (30%) Events will each have separate effects on this portion of satisfactionScore
    // Building values (20%) Different buildings will have different values,
    // e.g. rent price, this allows for more buildings to be implemented,
    // and gives them a clear difference in how they effect satisfaction score.
    // In other words, this is why a user may place accommodation building y,
    // instead of accommodation building x.
    public float satisfactionScore;

    // Satisfaction score is the sum of the following 4 variables, allowing for easier access to each part of the score
    // This also means when one of these values needs to be reset, or altered, the satisfaction score will only update
    // on screen once when that calculation is complete.
    public float buildingDistancesScore;
    public float completionScore;
    public float eventsScore;
    public float buildingValuesScore;

    // The map currently is 11x9 tiles
    // The maximum possible distance between two buildings, is the diagonal distance 1 less in both x and y,
    // than the number of tiles on the map
    float maxDistance = (float) Math.sqrt(Math.pow(11-1, 2) + Math.pow(9-1, 2));

    // This is how much of the satisfaction score each building use pair accounts for.
    // The number of undirected use pairs is of the form n + n-1 + n-2... + n-n, as we want the first use connected to
    // all uses, then the second needs to connect to all uses except the first, as that's already been counted, the
    // third use ignores the first and second, and so on. So we use the sum of 1 to n formula for this, which is
    // (n *(n+1)) / 2 We divide the total percent allowed for average distances (40) by this number
    float percentPerUsePair = 40 / (((float) Use.values().length * ((float) Use.values().length + 1)) / 2);

    // Allows the user to get the maximum satisfaction for a building use pair, if the pairs' average distance is
    // under 60% of the maximum possible distance. Anything over will give progressively less satisfaction.
    float maxScoreThreshold = maxDistance * 0.6f;


    // This 2D array stores the average distance between a pair of building types as an adjacency matrix
    // For example, you could set averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] to 0
    // However, averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] and
    // averageDistances[Use.TEACHING.ordinal()][Use.RECREATION.ordinal()] should hold the same value.
    public float[][] averageDistances = new float[Use.values().length][Use.values().length];

    // When adding a new building, it will need to multiply the old average distance by how many building pairs there
    // were, For example, if the previous average distance between a teaching and accommodation building
    // (5 + 3 + 2 + 6 + 9)/ 5 = 6 (5 teaching buildings 1 accommodation), and we add a new accommodation building,
    // it will need to add 5 new values to the average distance, as the accommodation building will have a distance to
    // each of the 5 teaching buildings. For this reason, it's helpful to keep a count of how many building pairs each
    // average distance is made up of, so that we can know what to multiply the average by, and then increment it and
    // divide the new total distance by the new number of building pairs. This value is stored in this 2D array:
    public int[][] averageDistancesCount = new int[Use.values().length][Use.values().length];

    private float currentTime;

    /**
     * Initialises an empty world and loads assets.
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    public World(Integer worldWidth, Integer worldHeight) {
        width = worldWidth;
        height = worldHeight;



        for (Use use : Use.values()) {
            buildingUseCounts.put(use, 0);
        }

        buildings = new Array<>();
    }


    /**
     * Adds a building to the world if allowed, updating the building store
     * @param building Building to add to the world
     * @return true if the placement was successful
     */
    public boolean addBuilding(Building building) {
        if (!doesBuildingOverlap(building)) {
            buildings.add(building);
            building.place();
            // Update satisfaction score
            updateAverageDistances(building);
            calculateBuildingDistancesScore(building);
            updateSatisfactionScore();
            System.out.println(satisfactionScore);
            return true;
        }
        return false;
    }

    /**
     * Update all buildings' states
     */
    public void updateBuildings(float deltaTime) {
        for (Building building : buildings) {
            if (building.placed && !building.built && currentTime > building.buildingCompletionTime) {
                // This will only trigger once (see '&& !building.built')
                building.built = true;

                for (Use use : building.getUses()) {
                    buildingUseCounts.put(use, buildingUseCounts.get(use) + 1);
                }
            }
        }
    }

    /**
     * Keeps the world running, updating its internal clock and buildings
     * @param deltaTime time since the last frame in seconds
     */
    public void worldProcess(float deltaTime) {
        currentTime += deltaTime;
        updateBuildings(deltaTime);
    }

    /**
     * Utility method to check if a building overlaps with any others in the world
     * after being snapped to the grid based on its current location
     * @param overlapBuilding The building to check for overlaps with others
     * @return true if the building overlaps with another, else false
     */
    public boolean doesBuildingOverlap(Building overlapBuilding) {
        GridCoordTuple gridCoords = overlapBuilding.getGridCoords();

        for (Building building : buildings) {
            if (!building.equals(overlapBuilding)) {
                if (building.gridX == gridCoords.x && building.gridY == gridCoords.y) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateSatisfactionScore() {
        satisfactionScore = buildingDistancesScore + completionScore + eventsScore + buildingValuesScore;
    }

    public void updateAverageDistances(Building building) {
        // Iterates through each use the building passed to this method has
        for (Use use1 : building.getUses()) {
            // Iterates through all buildings currently placed on the map
            for (Building comparisonBuilding : buildings) {
                // Distance is the diagonal distance between the building passed to this method, and the current
                // comparisonBuilding.
                float distance = (float) Math.sqrt(Math.pow(building.gridX - comparisonBuilding.gridX, 2) +
                    Math.pow(building.gridY - comparisonBuilding.gridY, 2));

                // Iterates through each use the comparison building has, except when the building passed to this method
                // and the comparison building are the same building.
                if (distance != 0) {
                    for (Use use2 : comparisonBuilding.getUses()) {
                        // The number of building distances in each average stored in averageDistances is the counter for
                        // both building types multiplied together. For example if there are 5 teaching buildings, and 3
                        // accommodation buildings, then there are 15 pairs of the two types.
                        // If we label each pair in the form T1A1 = distance between teaching building 1 and accommodation
                        // building 1, the average distance between all teaching and accommodation buildings would be:
                        // (T1A1 + T1A2 + T1A3 + T2A1 +...+ T5A3)/(5*3)

                        // numberOfPairs is the number of pairs the average distance between the two uses is made up of
                        // For example, if there were 4 teaching and 3 accommodation buildings before the building
                        // passed to this method was added, then numberOfPairs would be 12.
                        int numberOfPairs = averageDistancesCount[use1.ordinal()][use2.ordinal()];

                        // Update the average distance between the current pair of uses.
                        averageDistances[use1.ordinal()][use2.ordinal()] = (averageDistances[use1.ordinal()][use2.ordinal()]
                            * numberOfPairs + distance) / (numberOfPairs+1);

                        // Increment the number of building pairs used for the average distance between use1 and use2
                        averageDistancesCount[use1.ordinal()][use2.ordinal()] += 1;

                    }
                }
            }
        }
    }

    /**
     *  Updates the buildingDistancesScore when a building is added
     */
    public void calculateBuildingDistancesScore(Building building) {

        // resets the score to 0, and then adds up the new scores for each use pair.
        buildingDistancesScore = 0;
        // Sorts the buildings uses to make sure the right direction is used for the use pairs, as if 
        Arrays.sort(building.getUses());
        for (Use use1 : building.getUses()) {
            for (Use use2 : Use.values()) {
                // Only iterates over the upper triangle of the adjacency matrix
                if (use2.ordinal() >= use1.ordinal()) {
                    if (use1 == Use.TEACHING && use2 == Use.ACCOMMODATION) {
                        buildingDistancesScore += calculateScoreBonus(1.5f, use1, use2);
                    }
                    else if (use1 == Use.TEACHING && use2 == Use.TEACHING) {
                        buildingDistancesScore += calculateScoreBonus(1.5f, use1, use2);
                    }
                    else if (use1 == Use.ACCOMMODATION && use2 == Use.ACCOMMODATION) {
                        buildingDistancesScore += calculateScoreBonus(1.5f, use1, use2);
                    }
                    else if (use1 == Use.ACCOMMODATION && use2 == Use.CAFETERIA) {
                        buildingDistancesScore += calculateScoreBonus(2.25f, use1, use2);
                    }
                    else if (use1 == Use.ACCOMMODATION && use2 == Use.RECREATION) {
                        buildingDistancesScore += calculateScoreBonus(1.75f, use1, use2);
                    }
                    else if (use1 == Use.CAFETERIA && use2 == Use.RECREATION) {
                        buildingDistancesScore += calculateScoreBonus(0.5f, use1, use2);
                    }
                    else if (use1 == use2) {
                        buildingDistancesScore += 0;
                    }
                    else {
                        buildingDistancesScore += calculateScoreBonus(1f, use1, use2);

                    }
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
        if (buildingUseCounts.get(use1) == 0 || buildingUseCounts.get(use2) == 0) {
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
        // To calculate this, we would do maxDistance - threshold, divided by maxDistance - distance, which would be
        // (10 - 6) / (10 - 7) = 3/4 = 0.75 (using distance as 7 here).
        else {
            return (percentPerUsePair * multiplier) *
                (maxDistance - maxScoreThreshold) / (maxDistance - averageDistances[use1.ordinal()][use2.ordinal()]);
        }
    }


    public float getCurrentTime() {
        return currentTime;
    }
}
