package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainObject;
import io.github.archessmn.ENG1.OpenSimplexNoise;
import io.github.archessmn.ENG1.GameModel.Objects.*;

import static io.github.archessmn.ENG1.GameModel.GridUtils.*;
import static io.github.archessmn.ENG1.GameModel.Objects.Use.*;
import static io.github.archessmn.ENG1.GameModel.Objects.BuildingName.*;
import static io.github.archessmn.ENG1.GameModel.GameEvent.*;
import static io.github.archessmn.ENG1.GameModel.Objects.TerrainObject.Feature.*;
import static io.github.archessmn.ENG1.GameModel.Objects.TerrainObject.Feature;

import java.util.Random;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public final int WIDTH, HEIGHT;
    public static final float GAME_LENGTH_SECONDS = 300;

    private final MapObjectHolder mapObjects = new MapObjectHolder(GRID_WIDTH, GRID_HEIGHT);

    // An instance of the satisfaction class, this handles the satisfaction score, and all relevant calculations.
    private final Satisfaction satisfaction;

    // Stores events that have prolonged effects. Indices are preset for quicker lookup, even though instantaneous events are never stored here so the array can never be full.
    private final GameEvent[] activeEvents = new GameEvent[GameEvent.values().length];
    private final float[] activeEventEndTime = new float[GameEvent.values().length];

    private float currentTime;
    private final EventManager eventManager;
    private final Random random = new Random();

    private final AchievementManager achievementManager;

    public static final int TOO_MANY_LECTURE_BUILDINGS = 15; // How many lecture buildings are needed to allow the "too many buildings" event to occur
    private static final int GYMS_FOR_TOURNAMENT_WIN = 10; // The number of gyms needed to allow the university to win a sports event.
    public static final int DEMOLITION_TIME = 3; // How many in game days it will take for an object to be demolished

    /**
     * Initialises the game world with optional extra event listeners for event handling outside of this class
     * @param worldWidth Pixel width to use for the usable world space
     * @param worldHeight Pixel height to use for the usable world space
     * @param additionalEventHandlers Extra event handlers for event handling outside of this class. Can be used for rendering effects
     */
    public World(int worldWidth, int worldHeight, GameEventHandler[] additionalEventHandlers, AchievementHandler achievementHandler) {
        GameEventHandler[] eventHandlers = new GameEventHandler[additionalEventHandlers.length + 1];
        eventHandlers[0] = this::handleEvent;
        System.arraycopy(additionalEventHandlers, 0, eventHandlers, 1, additionalEventHandlers.length);
        eventManager = new EventManager(eventHandlers, GAME_LENGTH_SECONDS);

        achievementManager = new AchievementManager(this, achievementHandler);

        this.WIDTH = worldWidth;
        this.HEIGHT = worldHeight;

        // These can only happen when a building is near these terrain types
        eventManager.disableEvent(FLOODING);
        eventManager.disableEvent(TREE_DAMAGE);
        // These are conditional on the player's actions
        eventManager.disableEvent(TOURNAMENT_WON);
        eventManager.disableEvent(TOO_MANY_BUILDINGS);

        satisfaction = new Satisfaction(this);

        createWorldAssets();
    }


    /**
     * Responsible for creating all generated world assets, before it is shown to the player
     */
    private void createWorldAssets() {
        generateTerrainFeatures(LAKE, 0.6f, 100f);
        generateTerrainFeatures(ROCK, 0.75f, 150f);
        generateTerrainFeatures(TREE, 0.65f, 150f);

        // Places at least one lake tile down on the map - at a randomly generated location - if none were generated in the perlin noise
        if (mapObjects.getTerrainObjects().size == 0) {
            TerrainObject asset = new TerrainObject(new Random().nextInt(0, WIDTH), new Random().nextInt(0, HEIGHT), LAKE);
            addMapObject(asset);
        }
    }

    /**
     * Generates a perlin noise map of a particular terrain feature and places the assets into the world
     * @param feature The type of terrain feature to generate
     * @param acceptedValue The minimum value (-1 to 1) from the perlin noise algorithm that will be accepted
     * @param frequency The frequency for the perlin noise algorithm
     */
    private void generateTerrainFeatures(Feature feature, float acceptedValue, float frequency) {
        OpenSimplexNoise noise = new OpenSimplexNoise();
        int seed = new Random().nextInt(0, 100000);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH - 60; x++) {
                double value = noise.eval(x / frequency, y / frequency, seed);

                if (value > acceptedValue) {
                    TerrainObject asset = new TerrainObject(x, y, feature);
                    addMapObject(asset);
                }
            }
        }
    }

    /**
     * Adds a mapObject to the world if allowed, updating the relevant store(s)
     *
     * @param mapObject Object to add to the world
     * @return true if the placement was successful
     */
    public boolean addMapObject(MapObject mapObject) {
        mapObject.updateGridCoords();
        if (mapObject.getGridCoords().x < GRID_WIDTH && mapObject.getGridCoords().y < GRID_HEIGHT && !doesObjectOverlap(mapObject)) {
            mapObject.place();
            if (mapObject instanceof BuildingObject buildingObject) {
                buildingObject.resetConstruction(currentTime);
                mapObjects.add(buildingObject);
            } else if (mapObject instanceof TerrainObject terrainAsset) {
                mapObjects.add(terrainAsset);
                // Only do this for terrain since buildings update when construction is completed
                updateWorldState(terrainAsset, false);
            }
            else {
                throw new IllegalArgumentException("Unknown MapObject type" + mapObject.getClass().getSimpleName());
            }

            return true;
        }
        return false;
    }

    /**
     * Removes the given map object from the world and updates any world processes relating to the destruction of the
     * map object.
     *
     * @param mapObject Object to remove from the world
     */
    public void destroyMapObject(MapObject mapObject) {
        if (mapObject instanceof BuildingObject buildingObject){
            mapObjects.remove(buildingObject);
            if (buildingObject.built) updateWorldState(buildingObject, true);
        }
        else if (mapObject instanceof TerrainObject terrainObject) {
            mapObjects.remove(terrainObject);
            updateWorldState(terrainObject, true);
        }
    }

    /**
     * Utility method to check if a building overlaps with any others in the world
     * after being snapped to the grid based on its current location
     * @param overlapObject The building to check for overlaps with others
     * @return True if the building overlaps with another, else false
     * @throws IndexOutOfBoundsException If the mapObject is outside the grid
     */
    public boolean doesObjectOverlap(MapObject overlapObject) {
        GridCoordTuple gridCoords = overlapObject.getGridCoords();

        return mapObjects.spaceIsOccupied(gridCoords.x, gridCoords.y);
    }

    /**
     * Update all MapObjects' states
     */
    private void updateMapObjects(float currentTime) {
        for (MapObject mapObject : mapObjects.getAll()) {
            if (mapObject instanceof BuildingObject buildingObject) {
                if (!buildingObject.built && buildingObject.isComplete(currentTime)) {
                    // This will only trigger once (see '&& !building.built')
                    buildingObject.built = true;
                    updateWorldState(buildingObject, false);
                }
            }

            if (mapObject.toBeDemolished && mapObject.isDemolished(currentTime)) {
                destroyMapObject(mapObject);
            }
        }
    }

    /**
     * Called when a world object has been added or removed to provide more information to the update the world's state.
     * Note that closing a building has the same effect on satisfaction as removing it.
     * @param building The object in question.
     * @param wasRemoved if true, the building has just been removed. If false, the building has just been added.
     */
    private void updateWorldState(BuildingObject building, boolean wasRemoved) {
        // Check if this changes which events can happen
        // Additional check (left hand side of &&) so we don't have to run the longer check every time
        if (wasRemoved) {
            if (isBuildingNearTerrain(building, LAKE) && getCountOfTerrainNearBuildings(LAKE) <= 1) {
                eventManager.disableEvent(FLOODING);
            }
            if (isBuildingNearTerrain(building, TREE) && getCountOfTerrainNearBuildings(TREE) <= 1) {
                eventManager.disableEvent(TREE_DAMAGE);
            }
            if (eventManager.isEventEnabled(TOO_MANY_BUILDINGS) && mapObjects.getUseCount(TEACHING) <= TOO_MANY_LECTURE_BUILDINGS) {
                eventManager.disableEvent(TOO_MANY_BUILDINGS);
                // Disable the effect of the event as well if it has occurred
                activeEvents[TOO_MANY_BUILDINGS.ordinal()] = null;
            }
            achievementManager.incrementBuildingsDemolished();
        } else {
            if (!eventManager.isEventEnabled(FLOODING) && isBuildingNearTerrain(building, LAKE)) {
                eventManager.enableEvent(FLOODING);
            }
            if (!eventManager.isEventEnabled(TREE_DAMAGE) && isBuildingNearTerrain(building, TREE)) {
                eventManager.enableEvent(TREE_DAMAGE);
            }
            if (!eventManager.isEventEnabled(TOO_MANY_BUILDINGS) && mapObjects.getUseCount(TEACHING) >= TOO_MANY_LECTURE_BUILDINGS - 1) {
                eventManager.enableEvent(TOO_MANY_BUILDINGS);
            }
            achievementManager.incrementBuildingsBuilt();
        }

        satisfaction.updateScore(building, !wasRemoved);
    }

    /**
     * Called when a world object has been added or removed to provide more information to the update the world's state.
     * Note that closing a building has the same effect on satisfaction as removing it.
     * @param terrain The object in question.
     * @param wasRemoved if true, the mapObject has just been removed. If false, the mapObject has just been added.
     */
    private void updateWorldState(TerrainObject terrain, boolean wasRemoved) {
        // Check if this changes which events can happen if this object is the first or last object next to a building
        if (wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 1) {
            if (terrain.feature == LAKE) {
                eventManager.disableEvent(FLOODING);
            } else if (terrain.feature == TREE) {
                eventManager.disableEvent(TREE_DAMAGE);
            }
        } else if (!wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 0) {
            if (terrain.feature == LAKE && getCountOfTerrainNearBuildings(LAKE) > 0) {
                eventManager.enableEvent(FLOODING);
            } else if (terrain.feature == TREE && getCountOfTerrainNearBuildings(TREE) > 0) {
                eventManager.enableEvent(TREE_DAMAGE);
            }
        }

        satisfaction.updateScore(terrain, !wasRemoved);
    }

    /**
     * Keeps the world running, updating its internal clock and buildings
     * @param deltaTime time since the last frame in seconds
     */
    public void process(float deltaTime) {
        currentTime += deltaTime;
        updateMapObjects(currentTime);
        eventManager.processEvents(currentTime);
        // Maintain active events, removing them when necessary
        for (int i = 0; i < activeEventEndTime.length; i++) {
            if (activeEvents[i] != null && currentTime > activeEventEndTime[i]) {
                // Special effect for "Gym hype" to enable the possibility of winning the tournament if the user has placed enough gyms.
                if (i == GYM_HYPE.ordinal() && getCountOfSpecificBuilding(GYM) >= GYMS_FOR_TOURNAMENT_WIN) {
                    eventManager.enableEvent(TOURNAMENT_WON);
                }

                satisfaction.updateScore(activeEvents[i], false);
                activeEvents[i] = null;
                activeEventEndTime[i] = GAME_LENGTH_SECONDS + 1;
            }
        }
    }


    /**
     * @return Whether the game time has surpassed the limit.
     */
    public boolean getGameEnded() { return currentTime >= GAME_LENGTH_SECONDS; }

    /**
     * Gets the current game time.
     * @return The value of currentTime.
     */
    public float getCurrentTime() {
        return currentTime;
    }


    /**
     * Calls the respective processes that are needed for the given event.
     *
     * @param event The event that requires a process to be called
     */
    private void handleEvent(GameEvent event) {
        switch (event) {
            case FLOODING:
                for (BuildingObject building : getBuildingsNearTerrain(LAKE)) {
                    closeBuilding(building, 30f);
                }
                addActiveEvent(FLOODING, 30);
                break;
            case SEAGULL:
                if (mapObjects.getBuildings().size > 0) {
                    closeBuilding(getRandomBuilding(mapObjects.getBuildings()));
                }
                break;
            case TREE_HYPE:
                addActiveEvent(TREE_HYPE, 120);
                break;
            case TREE_DAMAGE:
                Array<BuildingObject> buildingsNearTrees = getBuildingsNearTerrain(TREE);
                if (buildingsNearTrees.size > 0) {
                    destroyMapObject(getRandomBuilding(buildingsNearTrees));
                }
                break;
            case GOOSE_ATTACK:
                // Has no effect
                break;
            case LECTURE_VIEW:
                addActiveEvent(LECTURE_VIEW);
                break;
            case ROCK_CLIMBING:
                addActiveEvent(ROCK_CLIMBING, 120);
                break;
            case LONG_BOI_SIGHTING:
                addActiveEvent(LONG_BOI_SIGHTING, 10);
                break;
            case GYM_HYPE:
                addActiveEvent(GYM_HYPE, 120);
                break;
            case TOURNAMENT_WON:
                addActiveEvent(TOURNAMENT_WON);
                break;
            case TOO_MANY_BUILDINGS:
                addActiveEvent(TOO_MANY_BUILDINGS);
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
        building.resetConstruction(currentTime, building.getRemainingConstructionTime(currentTime) + timeSeconds);

        updateWorldState(building, true);
    }

    /**
     * Returns a randomly selected building out of all that have placed in the world.
     *
     * @param buildings The array that contains all placed buildings
     * @return A randomly selected building from the placed buildings
     */
    private BuildingObject getRandomBuilding(Array<BuildingObject> buildings) {
        if (buildings.size == 0)
            return null;
        return buildings.get(random.nextInt(buildings.size));
    }

    /**
     * Adds an effect to the current game indefinitely
     * @param event The event associated with the effect
     */
    public void addActiveEvent(GameEvent event) {
        addActiveEvent(event, GAME_LENGTH_SECONDS + 1);
    }

    /**
     * Adds an effect to the current game for {@code timeSeconds} seconds
     * @param event The event associated with the effect
     */
    public void addActiveEvent(GameEvent event, float timeSeconds) {
        activeEvents[event.ordinal()] = event;
        activeEventEndTime[event.ordinal()] = currentTime + timeSeconds;

        // We do not update the world state here as the objects on the map have not changed
        satisfaction.updateScore(event, true);
    }

    /**
     * Returns whether an event is currently active or not.
     *
     * @param event The given event
     * @return Is the event active?
     */
    public boolean hasActiveEvent(GameEvent event) { return activeEvents[event.ordinal()] != null; }

    /**
     * Get the game time at which this active event will be removed. If the event is not active, returns {@value GAME_LENGTH_SECONDS} + 1
     * @param event The event to check
     * @return Time in seconds
     */
    public float getEndTimeOfActiveEvent(GameEvent event) { return activeEventEndTime[event.ordinal()]; }


    /**
     * Returns a count of a specific building type that have been placed.
     *
     * @param buildingType The type of building that is required for the count
     * @return The number of buildings of the given building type that have been placed
     */
    public int getCountOfSpecificBuilding(BuildingName buildingType) { return mapObjects.getByType(buildingType).size; }

    /**
     * Returns a list of buildings that are in any of the 8 tiles next to a given terrain feature
     * @param feature The type of feature, e.g. Lake
     */
    public Array<BuildingObject> getBuildingsNearTerrain(Feature feature) {
        Array<BuildingObject> foundBuildings = new Array<>();

        for (BuildingObject building : mapObjects.getBuildings()) {
            if (isBuildingNearTerrain(building, feature)) {
                foundBuildings.add(building);
            }
        }

        return foundBuildings;
    }

    /**
     * @return True if the building is near this type of terrain feature
     */
    public boolean isBuildingNearTerrain(BuildingObject building, Feature feature) {
        for (int x = Math.max(0, building.getGridCoords().x - 1); x <= Math.min(GRID_WIDTH - 1, building.getGridCoords().x + 1); x++) {
            for (int y = Math.max(0, building.getGridCoords().y - 1); y <= Math.min(GRID_HEIGHT - 1, building.getGridCoords().y + 1); y++) {
                if (mapObjects.getByGrid(x,y) instanceof TerrainObject && ((TerrainObject) mapObjects.getByGrid(x,y)).feature == feature) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return The objects on the map in any of the 8 spaces around the point specified. Returns an empty array if none
     * are found.
     */
    public Array<MapObject> getMapObjectsAroundPosition(GridCoordTuple coords) {
        Array<MapObject> mapObjects = new Array<>();
        for (int x = Math.max(0, coords.x - 1); x <= Math.min(GRID_WIDTH - 1, coords.x + 1); x++) {
            for (int y = Math.max(0, coords.y - 1); y <= Math.min(GRID_HEIGHT - 1, coords.y + 1); y++) {
                if (x != coords.x || y != coords.y) {
                    MapObject mapObject = getMapObjectAt(new GridCoordTuple(x,y));
                    if (mapObject != null) {
                        mapObjects.add(mapObject);
                    }
                }
            }
        }
        return mapObjects;
    }

    /**
     * @param feature e.g. Lake, Trees, etc.
     * @return The number of terrain tiles of type {@code feature} that is adjacent to any building
     */
    public int getCountOfTerrainNearBuildings(Feature feature) {
        int count = 0;
        for (TerrainObject terrainObject : mapObjects.getByFeature(feature)) {
            for (int x = Math.max(0, terrainObject.getGridCoords().x - 1); x <= Math.min(GRID_WIDTH - 1, terrainObject.getGridCoords().x + 1); x++) {
                for (int y = Math.max(0, terrainObject.getGridCoords().y - 1); y <= Math.min(GRID_HEIGHT - 1, terrainObject.getGridCoords().y + 1); y++) {
                    if (mapObjects.getByGrid(x,y) instanceof BuildingObject) {
                        count += 1;
                        break;
                    }
                }
            }
        }
        return count;
    }

    /**
     * @return The number of built buildings that provide a given use
     */
    public int getBuildingUseCount(Use use) {
        return mapObjects.getUseCount(use);
    }

    /**
     * @return All map objects
     */
    public Array<MapObject> getMapObjects() {
        return mapObjects.getAll();
    }

    /**
     * Get the map object at the grid square specified, or null if the space is empty
     */
    public MapObject getMapObjectAt(GridCoordTuple gridCoords) {
        return mapObjects.getByGrid(gridCoords.x, gridCoords.y);
    }

    /**
     * @return all buildings
     */
    public Array<BuildingObject> getBuildings() {
        return mapObjects.getBuildings();
    }

    /**
     * @return all {@code built} (or not built if false) buildings
     */
    public Array<BuildingObject> getBuildings(boolean built) {
        return mapObjects.getBuildings(built);
    }

    /**
     * @return all Terrain objects
     */
    public Array<TerrainObject> getTerrainObjects() {
        return mapObjects.getTerrainObjects();
    }

    /**
     * @return The Achievement Manager
     */
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    /**
     * Does not return the satisfaction score, but rather the satisfaction object.
     * To retrieve the score from a class such as GameScreen, do world.getSatisfaction().getSatisfactionScore().
     * @return The satisfaction object responsible for satisfaction storage and calculations.
     */
    public Satisfaction getSatisfaction() {
        return satisfaction;
    }

    /**
     * Checks if the map is full.
     * @return True if the map is full, false if not.
     */
    public boolean isMapFull() {
        return getBuildings().size + getTerrainObjects().size == GRID_WIDTH * GRID_HEIGHT;
    }
}
