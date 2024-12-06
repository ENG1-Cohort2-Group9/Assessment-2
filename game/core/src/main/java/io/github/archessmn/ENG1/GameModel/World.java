package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.Building;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainAsset;
import io.github.archessmn.ENG1.OpenSimplexNoise;
import io.github.archessmn.ENG1.GameModel.Objects.Use;

import java.util.HashMap;
import java.util.Random;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public int width, height;
    public final float GAME_LENGTH_SECONDS = 300;

    public Array<Building> buildings;
    public Array<TerrainAsset> terrain;
    public Building[][] gridLookup; // Stores pointers to the buildings and terrain on the grid so that squares can be queried

    public HashMap<Use, Integer> buildingUseCounts = new HashMap<>();

    // Between 0 and 100 percent
    //
    // There are n factors to the score:
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


    // Stores events that have prolonged effects. Indices are preset for quicker lookup, even though instantaneous events are never stored here so the array can never be full.
    GameEvent[] activeEvents = new GameEvent[GameEvent.values().length];
    float[] activeEventEndTime = new float[GameEvent.values().length];
    Array<EfficiencyModifier> activeModifiers = new Array<>();

    // This 2D array stores the average distance between a pair of building types,
    // For example, you may set averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] to 0
    public int[][] averageDistances = new int[Use.values().length][Use.values().length];

    private float currentTime;
    private EventManager eventManager;
    private Random random = new Random();


    private void setUpWorld(int worldWidth, int worldHeight) {
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
        terrain = new Array<>();
        gridLookup = new Building[width][height];

        createWorldAssets();
    }

    /**
     * Initialises the game world
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    public World(int worldWidth, int worldHeight) {
        setUpWorld(worldWidth, worldHeight);

        eventManager = new EventManager(new GameEventListener[] { new GameEventListener(this::handleEvent) }, GAME_LENGTH_SECONDS);
    }

    /**
     * Initialises the game world with an extra event listener for event handling outside of this class
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     * @param additionalEventListener an extra event listener for event handling outside of this class. Can be used for rendering effects
     */
    public World(int worldWidth, int worldHeight, GameEventListener additionalEventListener) {
        setUpWorld(worldWidth, worldHeight);

        eventManager = new EventManager(new GameEventListener[] { new GameEventListener(this::handleEvent), additionalEventListener }, GAME_LENGTH_SECONDS);
    }

    public void createWorldAssets() {
        generateTerrainFeatures(TerrainAsset.Feature.LAKE, 0.7f, 100f);
        generateTerrainFeatures(TerrainAsset.Feature.ROCK, 0.65f, 200f);

        boolean terrainAssetsPlaced = false;
        for (Building building : buildings) {
            if (building.uses[0] == Use.TERRAIN) {terrainAssetsPlaced = true;}
        }

        if (!terrainAssetsPlaced) {
            TerrainAsset asset = new TerrainAsset(new Random().nextInt(0, width), new Random().nextInt(0, height), 0, true, TerrainAsset.Feature.LAKE);
            addTerrain(asset);
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
                    addTerrain(asset);
                }
            }
        }
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
            gridLookup[building.gridX][building.gridY] = building;
            return true;
        }
        return false;
    }

    /**
     * Adds a terrain asset to the world if allowed, updating the terrain store
     * @param asset Asset to add to the world
     * @return true if the placement was successful
     */
    public boolean addTerrain(TerrainAsset asset) {
        if (!doesBuildingOverlap(asset)) {
            terrain.add(asset);
            asset.place();
            gridLookup[asset.gridX][asset.gridY] = asset;
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
        eventManager.processEvents(currentTime);
        for (int i = 0; i < activeEventEndTime.length; i++) {
            if (currentTime > activeEventEndTime[i]) {
                activeEvents[i] = null;
                activeEventEndTime[i] = GAME_LENGTH_SECONDS + 1;
            }
        }
        for (int i = 0; i < activeModifiers.size; i++) {
            if (currentTime > activeModifiers.get(i).endTime()) {
                activeModifiers.removeIndex(i);
                i--;
            }
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
        for (Building building : terrain) {
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
        switch (event) {
            case Flooding:
                float floodingTimeSeconds = 30;
                for (TerrainAsset asset : terrain) {
                    // Iterate through lakes
                    if (asset.feature == TerrainAsset.Feature.LAKE) {
                        // Iterate through objects around the lake to find buildings
                        for (int x = Math.max(0, asset.gridX - 1); x <= Math.min(width - 1, asset.gridX + 1); x++) {
                            for (int y = Math.max(0, asset.gridY - 1); y <= Math.min(height - 1, asset.gridY + 1); y++) {
                                if (!(gridLookup[x][y] instanceof TerrainAsset)) {
                                    closeBuilding(gridLookup[x][y], floodingTimeSeconds);
                                }
                            }
                        }
                    }
                }
                break;
            case Smelly:
                if (buildings.size > 0) {
                    modifyEfficiency(getRandomBuilding(buildings), 1.2f, GAME_LENGTH_SECONDS + 1);
                }
                break;
            case Seagull:
                if (buildings.size > 0) {
                    closeBuilding(getRandomBuilding(buildings));
                }
                break;
            case TreeHype:
                addActiveEvent(GameEvent.TreeHype, 120);
                break;
            case TreeDamage:
                Array<Building> buildingsNearTrees = new Array<>();
                for (TerrainAsset asset : terrain) {
                    // Iterate through trees
                    if (asset.feature == TerrainAsset.Feature.TREE) {
                        // Iterate through objects around the lake to find buildings
                        for (int x = Math.max(0, asset.gridX - 1); x <= Math.min(width - 1, asset.gridX + 1); x++) {
                            for (int y = Math.max(0, asset.gridY - 1); y <= Math.min(height - 1, asset.gridY + 1); y++) {
                                if (!(gridLookup[x][y] instanceof TerrainAsset)) {
                                    buildingsNearTrees.add(gridLookup[x][y]);
                                }
                            }
                        }
                    }
                }
                if (buildingsNearTrees.size > 0) {
                    destroyBuilding(getRandomBuilding(buildingsNearTrees));
                }
                break;
            case AColdWinter:
                addActiveEvent(GameEvent.AColdWinter, 45);
                break;
            case GooseAttack:
                break;
            case LectureLake:
                addActiveEvent(GameEvent.LectureLake, GAME_LENGTH_SECONDS + 1);
                break;
            case RockClimbing:
                addActiveEvent(GameEvent.RockClimbing, 120);
                break;
            case LongBoiSighting:
                addActiveEvent(GameEvent.LongBoiSighting, 10);
                break;
            default:
                throw new RuntimeException("Unknown event type: " + event);
        }
    }

    /**
     * Mark a building as under construction indefinitely
     */
    public void closeBuilding(Building building) {
        building.buildingCompletionTime = GAME_LENGTH_SECONDS + 1;
        building.built = false;
        calculatesatisfaction();
    }

    /**
     * Mark a building as under construction for {@code timeSeconds} seconds
     */
    public void closeBuilding(Building building, float timeSeconds) {
        building.buildingCompletionTime = currentTime + timeSeconds;
        building.built = false;
        calculatesatisfaction();
    }

    /**
     * Multiplies a building's efficiency by {@code multiplier} for {@code timeSeconds}, affecting satisfaction
     */
    public void modifyEfficiency(Building building, float multiplier, float timeSeconds) {
        activeModifiers.add(new EfficiencyModifier(currentTime + timeSeconds, multiplier, building));
        building.setEfficiency(building.getEfficiency() * multiplier);
        calculatesatisfaction();
    }

    public void destroyBuilding(Building building) {

    }

    public Building getRandomBuilding(Array<Building> buildings) {
        if (buildings.size == 0)
            return null;
        return buildings.get(random.nextInt(buildings.size));
    }

    /**
     * Adds an effect to the current game for {@code timeSeconds} seconds
     * @param event The event associated with the effect
     */
    public void addActiveEvent(GameEvent event, float timeSeconds) {
        activeEvents[event.ordinal()] = event;
        activeEventEndTime[event.ordinal()] = currentTime + timeSeconds;
        calculatesatisfaction();
    }
}
