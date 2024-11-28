package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public Integer width, height;

    public Array<Building> buildings;

    public HashMap<Building.Use, Integer> buildingUseCounts = new HashMap<>();

    /**
     * Initialises an empty world and loads assets.
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    public World(Integer worldWidth, Integer worldHeight) {
        this.width = worldWidth;
        this.height = worldHeight;


        for (Building.Use use : Building.Use.values()) {
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
        if (doesBuildingOverlap(building)) {
            return false;
        }
        buildings.add(building);
        building.place();
        return true;
    }

    /**
     * Run the tick() method on each building in the world building store
     * and update the counts for buildings of each use.
     */
    public void tickBuildings(float deltaTime) {

        for (Building.Use use : Building.Use.values()) {
            buildingUseCounts.put(use, 0);
        }

        for (Building building : buildings) {
            buildingUseCounts.put(building.getBuildingUse(), buildingUseCounts.get(building.getBuildingUse()) + 1);
            building.tick(deltaTime);
        }
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
}
