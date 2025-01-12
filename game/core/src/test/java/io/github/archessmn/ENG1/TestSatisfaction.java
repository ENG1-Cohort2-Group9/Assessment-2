package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.*;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingName;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.Use;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.HashSet;
import java.util.Set;

import static io.github.archessmn.ENG1.GameModel.GridUtils.*;
import static io.github.archessmn.ENG1.GameModel.Satisfaction.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.archessmn.ENG1.GameModel.Objects.BuildingName.*;

public class TestSatisfaction {

    Satisfaction satisfaction;
    World world;


    @BeforeEach
    public void setUp() {
        world = new World(960, 540, new GameEventHandler[]{}, null);
        satisfaction = world.getSatisfaction();

        // Clears the map of any terrain generated.
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (world.getMapObjectAt(new GridCoordTuple(x, y)) != null) {
                    world.destroyMapObject(world.getMapObjectAt(new GridCoordTuple(x, y)));
                }
            }
        }

    }

    /**
     * Finds an empty space on the map, and returns it as a GridCoordTuple.
     *
     * @return The space as a GridCoordTuple, or null if the map is full.
     */
    private GridCoordTuple getEmptySpace() {
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (world.getMapObjectAt(new GridCoordTuple(x, y)) == null) {
                    return new GridCoordTuple(x, y);
                }
            }
        }
        return null;
    }


    /**
     * Places n buildings down of the given type in the next n free grid squares.
     * @param n The number of buildings to be placed.
     * @param type The type of building to be placed n times.
     */
    private void placeNBuildingsOfTypeX(int n, BuildingName type) {
        GridCoordTuple emptySpace;
        for (int x = 0; x < n; x++) {
            emptySpace = getEmptySpace();
            if (emptySpace == null) {
                break;
            }
            world.addMapObject(new BuildingObject(emptySpace, 0, type));
        }
        // Lets the buildings finish building
        world.process(type.getBuildingConstructionDuration() + 1);
    }

    private void fillMapEvenly() {
        // Place an even distribution of buildings to fill the map.
        for (int i = 0; i < (GRID_HEIGHT * GRID_WIDTH) / BuildingName.values().length; i++) {
            for (int j = 0; j < BuildingName.values().length; j++) {
                placeNBuildingsOfTypeX(1, BuildingName.values()[j]);
            }
        }
    }


    @Test
    public void testCalculateMultiplier() {
        // As of writing the test the map is 11*9 tiles, while this may change in the future, these tests work with the
        // assumption that the map will never get smaller.

        // Initial test with 0 buildings.
        assertEquals(1 - BALANCE_FACTOR, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        placeNBuildingsOfTypeX(LOWER_BUILDING_LIMIT - 5, HALLS);
        assertEquals(1 - ((5f / LOWER_BUILDING_LIMIT) * BALANCE_FACTOR),
            satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        checkMultiplier(LOWER_BUILDING_LIMIT,  1);
        checkMultiplier(LOWER_BUILDING_LIMIT + 5,  1);
        checkMultiplier(UPPER_BUILDING_LIMIT,  1);
        checkMultiplier(UPPER_BUILDING_LIMIT + 5,  1 - ((5f / UPPER_BUILDING_LIMIT)) * BALANCE_FACTOR);
        checkMultiplier(GRID_HEIGHT * GRID_WIDTH,  1 - ((float) (GRID_HEIGHT * GRID_WIDTH - UPPER_BUILDING_LIMIT) / UPPER_BUILDING_LIMIT) * BALANCE_FACTOR);
    }

    /**
     * Helper method for testCalculateMultiplier, used to reduce code duplication.
     * @param number_of_buildings How many buildings are to be added to the map
     * @param expected The expected multiplier.
     */
    public void checkMultiplier(int number_of_buildings, float expected) {
        setUp(); // Reset the map.
        placeNBuildingsOfTypeX(number_of_buildings, HALLS); // Place the requested number of buildings.
        assertEquals(expected, satisfaction.calculateMultiplier(world.getBuildings(true).size),
            "The calculated multiplier is not as expected.");
    }

    @Test
    public void testBuildingDistances() {
        // This test set doesn't make any direct calls to satisfaction.updateAverageDistances(), but instead is also
        // testing whether adding and removing buildings from world is correctly calling the method.

        for (int x = 0; x < USE_LENGTH; x++) {
            for (int y = 0; y < USE_LENGTH; y++) {
                assertEquals(0, satisfaction.getAverageDistances()[x][y], "AverageDistances is not initialised correctly");
            }
        }
        // Place 5 of each building use down
        placeNBuildingsOfTypeX(5, HALLS);
        placeNBuildingsOfTypeX(5, PIAZZA);
        placeNBuildingsOfTypeX(5, GYM);
        checkAverageDistances(calculateExpectedAverageDistances());

        // Remove the buildings that were just added that are placed on an odd y grid value. This is to test
        // updateAverageDistances works when some buildings are removed.
        removeBuildingsByCondition(coord -> coord.y % 2 == 1);
        checkAverageDistances(calculateExpectedAverageDistances());

        fillMapEvenly();
        checkAverageDistances(calculateExpectedAverageDistances());

        // Remove the buildings that were just added that are placed on an odd x grid value. This is to test
        // updateAverageDistances works when some buildings are removed.
        removeBuildingsByCondition(coord -> coord.x % 2 == 1);
        checkAverageDistances(calculateExpectedAverageDistances());

        // Gives a predicate that is always true, so removes all buildings.
        removeBuildingsByCondition(coord -> coord.x != -1);
        checkAverageDistances(calculateExpectedAverageDistances());

    }

    /**
     * UpdateAverageDistances updates AverageDistances whenever a building is added or deleted, to test it is working,
     * this method does a brute force calculation for what AverageDistances should contain.
     *
     * @return The expected averageDistances as a float[][].
     */
    public float[][] calculateExpectedAverageDistances() {
        float[][] expectedAverageDistances = new float[USE_LENGTH][USE_LENGTH];
        int[][] counts = new int[USE_LENGTH][USE_LENGTH];
        // Once the average distances from a building to all others has been calculated, the building is added to
        // this array, so that distances to it aren't counted twice.
        Set<BuildingObject> calculated = new HashSet<>();

        for (BuildingObject building : world.getBuildings(true)) {
            for (BuildingObject comparisonBuilding : world.getBuildings(true)) {
                if ((building.getSnappedScreenPosition().x != comparisonBuilding.getSnappedScreenPosition().x ||
                    building.getSnappedScreenPosition().y != comparisonBuilding.getSnappedScreenPosition().y) && !calculated.contains(comparisonBuilding)) {
                    for (Use use1 : building.getUses()) {
                        float distance = (float) Math.sqrt(Math.pow(building.getGridCoords().x - comparisonBuilding.getGridCoords().x, 2) +
                            Math.pow(building.getGridCoords().y - comparisonBuilding.getGridCoords().y, 2));

                        for (Use use2 : comparisonBuilding.getUses()) {
                            if (use1.ordinal() < use2.ordinal()) {
                                counts[use1.ordinal()][use2.ordinal()] += 1;
                                expectedAverageDistances[use1.ordinal()][use2.ordinal()] += distance;
                            }
                            else {
                                counts[use2.ordinal()][use1.ordinal()] += 1;
                                expectedAverageDistances[use2.ordinal()][use1.ordinal()] += distance;
                            }
                        }
                    }
                }
            }
            calculated.add(building);
        }
        for (int x = 0; x < USE_LENGTH; x++) {
            for (int y = 0; y < USE_LENGTH; y++) {
                if (expectedAverageDistances[x][y] != 0) {
                    expectedAverageDistances[x][y] /= counts[x][y];
                }
            }
        }
        checkAverageDistanceScores(calculateExpectedAverageDistanceScores(expectedAverageDistances));
        return expectedAverageDistances;
    }

    /**
     * Checks if AverageDistances is equal to the expected AverageDistances.
     * @param expectedAverageDistances The brute force calculated AverageDistances.
     */
    public void checkAverageDistances(float[][] expectedAverageDistances) {
        for (int x = 0; x < USE_LENGTH; x++) {
            for (int y = 0; y < USE_LENGTH; y++) {
                assertEquals(expectedAverageDistances[x][y], satisfaction.getAverageDistances()[x][y],0.001,  "AverageDistances is not calculated correctly.");
            }
        }
    }

    /**
     * Removes buildings from the map.
     * @param condition Needs to be met to remove a building. Used to remove buildings at odd/even x/y coordinates.
     *                  If the condition is always met, all buildings are removed.
     */
    public void removeBuildingsByCondition(java.util.function.Predicate<GridCoordTuple> condition) {
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                GridCoordTuple coord = new GridCoordTuple(x, y);
                if (world.getMapObjectAt(coord) != null && condition.test(coord)) {
                    world.destroyMapObject(world.getMapObjectAt(coord));
                }
            }
        }
    }


    /**
     * Does a brute force sum of the average distance scores.
     * @param expectedAverageDistances The current expected average distances.
     * @return The expected average distance scores.
     */
    public float[][] calculateExpectedAverageDistanceScores(float[][] expectedAverageDistances) {
        float[][] expectedAverageDistanceScores = new float[USE_LENGTH][USE_LENGTH];
        for (Use use1 : Use.values()) {
            for (Use use2 : Use.values()) {
                if (expectedAverageDistances[use1.ordinal()][use2.ordinal()] != 0) {
                    expectedAverageDistanceScores[use1.ordinal()][use2.ordinal()] =
                        satisfaction.calculateScoreBonus(satisfaction.getWeight(use1, use2), use1, use2);
                }
            }
        }
        return expectedAverageDistanceScores;
    }

    /**
     * Checks if AverageDistanceScores is equal to the expected AverageDistanceScores.
     * @param expectedAverageDistanceScores The brute force calculated AverageDistanceScores.
     */
    public void checkAverageDistanceScores(float[][] expectedAverageDistanceScores) {
        for (int x = 0; x < USE_LENGTH; x++) {
            for (int y = 0; y < USE_LENGTH; y++) {
                assertEquals(expectedAverageDistanceScores[x][y], satisfaction.getAverageDistanceScores()[x][y],0.001,  "AverageDistanceScores is not calculated correctly.");
            }
        }
    }


    @Test
    public void testUpdateCompletionScore() {
        // What buildings exist, and what uses they have is subject to change, so this test will need manual updating
        // if existing buildings have their uses changed, or additional uses are added.
        // Add 1 of each Building use, checking the score after each.
        placeNBuildingsOfTypeX(1, HALLS);
        assertEquals(satisfaction.completionScorePerUse, satisfaction.getSatisfactionScoreBreakdown()[1]);
        placeNBuildingsOfTypeX(1, GYM);
        assertEquals(satisfaction.completionScorePerUse * 2, satisfaction.getSatisfactionScoreBreakdown()[1]);
        placeNBuildingsOfTypeX(1, LECTURE_HALL);
        assertEquals(satisfaction.completionScorePerUse * 3, satisfaction.getSatisfactionScoreBreakdown()[1]);
        placeNBuildingsOfTypeX(1, PUB);
        assertEquals(satisfaction.getSatisfactionScoreCaps()[1], satisfaction.getSatisfactionScoreBreakdown()[1]);
        // Fill the map and check completionScore doesn't go past its cap.
        fillMapEvenly();
        assertEquals(satisfaction.getSatisfactionScoreCaps()[1], satisfaction.getSatisfactionScoreBreakdown()[1]);
        // Remove all buildings and check it resets to 0.
        removeBuildingsByCondition(coord -> coord.x != -1);
        assertEquals(0, satisfaction.getSatisfactionScoreBreakdown()[1]);
    }
}
