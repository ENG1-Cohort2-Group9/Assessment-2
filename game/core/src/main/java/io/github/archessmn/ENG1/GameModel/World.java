package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainObject;
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

    public Array<BuildingObject> buildings;
    public Array<TerrainObject> terrain;
    public Array<MapObject> mapObjects;
    public MapObject[][] gridLookup; // Stores pointers to the buildings and terrain on the grid so that squares can be queried

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
        mapObjects = new Array<>();
        gridLookup = new MapObject[width][height];

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
        // These can only happen when a building is near these terrain types
        eventManager.disableEvent(GameEvent.Flooding);
        eventManager.disableEvent(GameEvent.TreeDamage);
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
        // These can only happen when a building is near these terrain types
        eventManager.disableEvent(GameEvent.Flooding);
        eventManager.disableEvent(GameEvent.TreeDamage);
    }

    /**
     * Responsible for creating all generated world assets, before it is showcased to the player
     */
    public void createWorldAssets() {
        generateTerrainFeatures(TerrainObject.Feature.LAKE, 0.6f, 100f);
        generateTerrainFeatures(TerrainObject.Feature.ROCK, 0.75f, 200f);
        generateTerrainFeatures(TerrainObject.Feature.TREE, 0.65f, 200f);

        // Places at least one lake tile down on the map - at a randomly generated location - if none were generated in the perlin noise
        if (terrain.size == 0) {
            TerrainObject asset = new TerrainObject(new Random().nextInt(0, width), new Random().nextInt(0, height), TerrainObject.Feature.LAKE);
            addTerrain(asset);
        }
    }

    /**
     * Generates a perlin noise map of a particular terrain feature and places the assets into the world
     * @param feature The type of terrain feature to generate
     * @param acceptedValue The minimum value (-1 to 1) from the perlin noise algorithm that will be accepted
     * @param frequency The frequency for the perlin noise algorithm
     */
    private void generateTerrainFeatures(TerrainObject.Feature feature, float acceptedValue, float frequency) {
        OpenSimplexNoise noise = new OpenSimplexNoise();
        int seed = new Random().nextInt(0, 100000);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width - 60; x++) {
                double value = noise.eval(x / frequency, y / frequency, seed);

                if (value > acceptedValue) {
                    TerrainObject asset = new TerrainObject(x, y, feature);
                    addTerrain(asset);
                }
            }
        }
    }

    /**
     * Adds a building to the world if allowed, updating the building store
     *
     * @param building Building to add to the world
     */
    public void addBuilding(BuildingObject building) {
        if (!doesObjectOverlap(building)) {
            buildings.add(building);
            mapObjects.add(building);
            building.place();
            gridLookup[building.gridX][building.gridY] = building;

            if (!eventManager.isEventEnabled(GameEvent.Flooding) && isBuildingNearTerrain(building, TerrainObject.Feature.LAKE))
                eventManager.enableEvent(GameEvent.Flooding);
            if (!eventManager.isEventEnabled(GameEvent.TreeDamage) && isBuildingNearTerrain(building, TerrainObject.Feature.TREE))
                eventManager.enableEvent(GameEvent.TreeDamage);
        }
    }

    /**
     * Adds a terrain asset to the world if allowed, updating the terrain store
     * @param asset Asset to add to the world
     * @return true if the placement was successful
     */
    public boolean addTerrain(TerrainObject asset) {
        if (!doesObjectOverlap(asset)) {
            terrain.add(asset);
            mapObjects.add(asset);
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
        for (BuildingObject building : buildings) {
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
        // Maintain active events, removing them when necessary
        for (int i = 0; i < activeEventEndTime.length; i++) {
            if (currentTime > activeEventEndTime[i]) {
                activeEvents[i] = null;
                activeEventEndTime[i] = GAME_LENGTH_SECONDS + 1;
                calculatesatisfaction();
            }
        }
        // Maintain active modifiers, removing them when necessary
        for (int i = 0; i < activeModifiers.size; i++) {
            if (currentTime > activeModifiers.get(i).endTime()) {
                BuildingObject building = activeModifiers.get(i).affectedBuilding();
                building.setEfficiency(building.getEfficiency() / activeModifiers.get(i).multiplier());
                activeModifiers.removeIndex(i);
                i--;
                calculatesatisfaction();
            }
        }
    }

    /**
     * Utility method to check if a building overlaps with any others in the world
     * after being snapped to the grid based on its current location
     * @param overlapObject The building to check for overlaps with others
     * @return true if the building overlaps with another, else false
     */
    public boolean doesObjectOverlap(MapObject overlapObject) {
        GridCoordTuple gridCoords = overlapObject.getGridCoords();

        for (MapObject mapObject : mapObjects) {
            if (!mapObject.equals(overlapObject)) {
                if (mapObject.gridX == gridCoords.x && mapObject.gridY == gridCoords.y) {
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
        // Events
        float eventSatisfactionScore = 0f;
        if (activeEvents[GameEvent.TreeHype.ordinal()] != null) {
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.TREE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.ACCOMMODATION) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
        }
        if (activeEvents[GameEvent.LectureLake.ordinal()] != null) {
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.LAKE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.TEACHING) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
        }
        if (activeEvents[GameEvent.RockClimbing.ordinal()] != null) {
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.ROCK)) {
                for (Use use : building.getUses()) {
                    if (use == Use.ACCOMMODATION) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
        }
        if (activeEvents[GameEvent.AColdWinter.ordinal()] != null) {
            eventSatisfactionScore *= 0.8f;
        }
        if (activeEvents[GameEvent.LongBoiSighting.ordinal()] != null) {
            eventSatisfactionScore = 0.3f; // This event is very powerful, but only lasts a short time
        }
        if (eventSatisfactionScore > 0.3f)
            eventSatisfactionScore = 0.3f;
        // -------------------

        return satisfactionScore;
    }

    public float getCurrentTime() {
        return currentTime;
    }

    public void handleEvent(GameEvent event) {
        switch (event) {
            case Flooding:
                for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.LAKE)) {
                    closeBuilding(building, 30f);
                }
                break;
            case Smelly:
                if (buildings.size > 0) {
                    modifyEfficiency(getRandomBuilding(buildings), 0.5f, GAME_LENGTH_SECONDS + 1);
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
                Array<BuildingObject> buildingsNearTrees = getBuildingsNearTerrain(TerrainObject.Feature.TREE);
                if (buildingsNearTrees.size > 0) {
                    destroyBuilding(getRandomBuilding(buildingsNearTrees));
                }
                break;
            case AColdWinter:
                addActiveEvent(GameEvent.AColdWinter, 45);
                break;
            case GooseAttack:
                // Has no effect
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
    public void closeBuilding(BuildingObject building) {
        closeBuilding(building, GAME_LENGTH_SECONDS + 1);
    }

    /**
     * Mark a building as under construction for {@code timeSeconds} seconds
     */
    public void closeBuilding(BuildingObject building, float timeSeconds) {
        if (building.built) {
            for (Use use : building.uses) {
                buildingUseCounts.put(use, buildingUseCounts.get(use) - 1);
            }
        }

        building.buildingCompletionTime = currentTime + timeSeconds;
        building.built = false;
        calculatesatisfaction();
    }

    /**
     * Multiplies a building's efficiency by {@code multiplier} for {@code timeSeconds}, affecting satisfaction
     */
    public void modifyEfficiency(BuildingObject building, float multiplier, float timeSeconds) {
        activeModifiers.add(new EfficiencyModifier(currentTime + timeSeconds, multiplier, building));
        building.setEfficiency(building.getEfficiency() * multiplier);
        calculatesatisfaction();
    }

    public void destroyBuilding(BuildingObject building) {
        // Check if this changes which events can happen
        // Additional check (left hand side of &&) so we don't have to run the longer check every time
        if (isBuildingNearTerrain(building, TerrainObject.Feature.LAKE) && getBuildingsNearTerrain(TerrainObject.Feature.LAKE).size == 1) {
            eventManager.disableEvent(GameEvent.Flooding);
        }
        if (isBuildingNearTerrain(building, TerrainObject.Feature.TREE) && getBuildingsNearTerrain(TerrainObject.Feature.TREE).size == 1) {
            eventManager.disableEvent(GameEvent.TreeDamage);
        }

        gridLookup[building.gridX][building.gridY] = null;
        buildings.removeValue(building, true);
        mapObjects.removeValue(building, true);
        if (building.built) {
            for (Use use : building.uses) {
                buildingUseCounts.put(use, buildingUseCounts.get(use) - 1);
            }
        }

        calculatesatisfaction();
    }

    public void destroyTerrain(TerrainObject terrainObject) {
        // EVENT CHECKS TO BE COMPLETED HERE

        gridLookup[terrainObject.gridX][terrainObject.gridY] = null;
        terrain.removeValue(terrainObject, true);
        mapObjects.removeValue(terrainObject, true);

        calculatesatisfaction();
    }

    public BuildingObject getRandomBuilding(Array<BuildingObject> buildings) {
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

    /**
     * Returns a list of buildings that are in any of the 8 tiles next to a given terrain feature
     * @param feature The type of feature, e.g. Lake
     */
    public Array<BuildingObject> getBuildingsNearTerrain(TerrainObject.Feature feature) {
        Array<BuildingObject> foundBuildings = new Array<>();

        for (BuildingObject building : buildings) {
            if (isBuildingNearTerrain(building, feature)) {
                foundBuildings.add(building);
            }
        }

        return foundBuildings;
    }

    /**
     * @return True if the building is near this type of terrain feature
     */
    public boolean isBuildingNearTerrain(BuildingObject building, TerrainObject.Feature feature) {
        for (int x = Math.max(0, building.gridX - 1); x <= Math.min(width - 1, building.gridX + 1); x++) {
            for (int y = Math.max(0, building.gridY - 1); y <= Math.min(height - 1, building.gridY + 1); y++) {
                if (gridLookup[x][y] instanceof TerrainObject && ((TerrainObject) gridLookup[x][y]).feature == feature) {
                    return true;
                }
            }
        }
        return false;
    }
}
