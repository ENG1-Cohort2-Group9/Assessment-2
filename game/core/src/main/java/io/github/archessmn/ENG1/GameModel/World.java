package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Buildings.Building;
import io.github.archessmn.ENG1.GameModel.Buildings.Use;

import java.util.HashMap;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public Integer width, height;
    public final float GAME_LENGTH_SECONDS = 300;

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




    // This 2D array stores the average distance between a pair of building types,
    // For example, you may set averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] to 0
    public int[][] averageDistances = new int[Use.values().length][Use.values().length];

    private float currentTime;
    private EventManager eventManager = new EventManager(new GameEventListener(this::handleEvent), GAME_LENGTH_SECONDS);

    /**
     * Initialises an empty world and loads assets.
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    public World(Integer worldWidth, Integer worldHeight) {
        this.width = worldWidth;
        this.height = worldHeight;


        for (Use use : Use.values()) {
            buildingUseCounts.put(use, 0);
        }


        satisfactionScore = 0;
        // Creates an adjacency matrix for each building use pair
        for (Use use1 : Use.values()) {
            for (Use use2 : Use.values()) {
                averageDistances[use1.ordinal()][use2.ordinal()] = 0;
            }
        }

        buildings = new Array<>();
    }

    /**
     * Adds a building to the world if allowed, updating the building store
     * @param building Building to add to the world
     * @return true if the placement was successful
     */
    public void addBuilding(Building building) {
        if (!doesBuildingOverlap(building)) {
            buildings.add(building);
            building.place();
        }
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
        eventManager.processEvents(currentTime);
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

    public boolean getGameEnded() {
        return currentTime >= GAME_LENGTH_SECONDS;
    }

    public float calculatesatisfaction() {

        return satisfactionScore;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void handleEvent(GameEvent event) {
        System.out.println(event.title);
    }
}
