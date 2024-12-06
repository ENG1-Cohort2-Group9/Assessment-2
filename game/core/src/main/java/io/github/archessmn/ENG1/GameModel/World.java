package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.Building;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainAsset;
import io.github.archessmn.ENG1.OpenSimplexNoise;

import java.util.HashMap;
import java.util.Random;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public Integer width, height;

    public Array<Building> buildings;

    public HashMap<Building.Use, Integer> buildingUseCounts = new HashMap<>();

    private float currentTime;

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

        createWorldAssets();
    }

    public void createWorldAssets() {
        generateTerrainFeatures(TerrainAsset.Feature.LAKE, 0.7f, 100f);
        generateTerrainFeatures(TerrainAsset.Feature.ROCK, 0.65f, 200f);

        boolean terrainAssetsPlaced = false;
        for (Building building : buildings) {
            if (building.uses[0] == Building.Use.TERRAIN) {terrainAssetsPlaced = true;}
        }

        if (!terrainAssetsPlaced) {
            TerrainAsset asset = new TerrainAsset(new Random().nextInt(0, width), new Random().nextInt(0, height), 0, true, TerrainAsset.Feature.LAKE);
            if (!doesBuildingOverlap(asset)) {addBuilding(asset);}
        }
    }

    private void generateTerrainFeatures(TerrainAsset.Feature feature, float acceptedValue, float frequency) {
        OpenSimplexNoise noise = new OpenSimplexNoise();
        int seed = new Random().nextInt(0, 100000);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width - 60; x++) {
                double value = noise.eval(x / frequency, y / frequency, seed);

                if (value > acceptedValue) {
                    TerrainAsset asset = new TerrainAsset(x, y, 0, true, feature);
                    if (!doesBuildingOverlap(asset)) {addBuilding(asset);}
                }
            }
        }
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

                for (Building.Use use : building.getUses()) {
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

    public float getCurrentTime() {
        return currentTime;
    }
}
