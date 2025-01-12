package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.*;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingName;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static io.github.archessmn.ENG1.GameModel.GridUtils.*;
import static io.github.archessmn.ENG1.GameModel.Satisfaction.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.archessmn.ENG1.GameModel.Objects.BuildingName.*;

public class TestSatisfaction {

    Satisfaction satisfaction;
    World world;

    @BeforeEach
    public void setUp() {
        world = new World(960, 540, new GameEventHandler[] {}, null);
        satisfaction = new Satisfaction(world);

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


    private void placeNBuildingsOfTypeX(int n, BuildingName type) {
        GridCoordTuple emptySpace = getEmptySpace();
        for (int x = 0; x < n; x++) {
            emptySpace = getEmptySpace();
            if (emptySpace == null) {
                break;
            }
            world.addMapObject(new BuildingObject(emptySpace, 0, type));
        }
    }



    @Test
    public void testCalculateMultiplier() {
        // As of writing the test the map is 11*9 tiles, while this may change in the future, these tests work with the
        // assumption that the map will never get smaller.

        // Initial test with 0 buildings.
        assertEquals(1 - BALANCE_FACTOR, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        placeNBuildingsOfTypeX(LOWER_BUILDING_LIMIT - 5, HALLS);
        world.process(HALLS.getBuildingConstructionDuration()  + 1);
        assertEquals(1 - ((5f / LOWER_BUILDING_LIMIT)* BALANCE_FACTOR), satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        setUp(); // Reset the map
        placeNBuildingsOfTypeX(LOWER_BUILDING_LIMIT, HALLS);
        world.process(HALLS.getBuildingConstructionDuration()  + 1);
        assertEquals(1, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        setUp(); // Reset the map
        placeNBuildingsOfTypeX(LOWER_BUILDING_LIMIT + 5, HALLS);
        world.process(HALLS.getBuildingConstructionDuration()  + 1);
        assertEquals(1, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        setUp(); // Reset the map
        placeNBuildingsOfTypeX(UPPER_BUILDING_LIMIT, HALLS);
        world.process(HALLS.getBuildingConstructionDuration()  + 1);
        assertEquals(1, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        setUp(); // Reset the map
        placeNBuildingsOfTypeX(UPPER_BUILDING_LIMIT + 5, HALLS);
        world.process(HALLS.getBuildingConstructionDuration()  + 1);
        assertEquals(1 - ((5f / UPPER_BUILDING_LIMIT)) * BALANCE_FACTOR, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");

        setUp(); // Reset the map
        placeNBuildingsOfTypeX(GRID_HEIGHT * GRID_WIDTH, HALLS);
        world.process(HALLS.getBuildingConstructionDuration()  + 10);
        assertEquals(1 - ((float) (GRID_HEIGHT * GRID_WIDTH - UPPER_BUILDING_LIMIT) / UPPER_BUILDING_LIMIT) * BALANCE_FACTOR, satisfaction.calculateMultiplier(world.getBuildings(true).size), "The calculated multiplier is not as expected.");
    }

    @Test
    public void testUpdateAverageDistances() {
        int count = 0;
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (world.getMapObjectAt(new GridCoordTuple(x, y)) != null) {
                    System.out.println(x + ", "  + y);
                }
            }
        }
        System.out.println(count);

        placeNBuildingsOfTypeX(2, HALLS);
        placeNBuildingsOfTypeX(2, PIAZZA);
        placeNBuildingsOfTypeX(94, GYM);
        world.process(HALLS.getBuildingConstructionDuration()  + 10);

        System.out.println(world.getBuildings(true).size);
    }
}
