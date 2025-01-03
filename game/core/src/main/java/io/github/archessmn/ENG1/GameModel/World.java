package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainObject;
import io.github.archessmn.ENG1.OpenSimplexNoise;
import io.github.archessmn.ENG1.GameModel.Objects.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.*;
import java.util.*;

/**
 * Class used to store information about the world and the buildings in it.
 */
public class World {

    public final int WIDTH, HEIGHT;
    public static final float GAME_LENGTH_SECONDS = 300;

    private MapObjectHolder mapObjects = new MapObjectHolder(GridUtils.GRID_WIDTH, GridUtils.GRID_HEIGHT);

    // An instance of the satisfaction class, this handles the satisfaction score, and all relevant calculations.
    public Satisfaction satisfaction;

    // Stores events that have prolonged effects. Indices are preset for quicker lookup, even though instantaneous events are never stored here so the array can never be full.
    private GameEvent[] activeEvents = new GameEvent[GameEvent.values().length];
    private float[] activeEventEndTime = new float[GameEvent.values().length];

    private float currentTime;
    private EventManager eventManager;
    private Random random = new Random();

    public final int TOO_MANY_LECTURE_BUILDINGS = 15; // How many lecture buildings are needed to allow the "too many buildings" event to occur
    private final int GYMS_FOR_TOURNAMENT_WIN = 10; // The number of gyms needed to allow the university to win a sports event.
    public final int DEMOLITION_TIME = 3; // How many in game days it will take for an object to be demolished

    /**
     * Initialises the game world with optional extra event listeners for event handling outside of this class
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     * @param additionalEventListeners Extra event listeners for event handling outside of this class. Can be used for rendering effects
     */
    public World(int worldWidth, int worldHeight, GameEventListener[] additionalEventListeners) {
        GameEventListener[] listeners = new GameEventListener[additionalEventListeners.length + 1];
        listeners[0] = new GameEventListener(this::handleEvent);
        System.arraycopy(additionalEventListeners, 0, listeners, 1, additionalEventListeners.length);
        eventManager = new EventManager(listeners, GAME_LENGTH_SECONDS);

        this.WIDTH = worldWidth;
        this.HEIGHT = worldHeight;



        // These can only happen when a building is near these terrain types
        eventManager.disableEvent(GameEvent.FLOODING);
        eventManager.disableEvent(GameEvent.TREE_DAMAGE);
        // These are conditional on the player's actions
        eventManager.disableEvent(GameEvent.TOURNAMENT_WON);
        eventManager.disableEvent(GameEvent.TOO_MANY_BUILDINGS);

        satisfaction = new Satisfaction(this);

        createWorldAssets();
    }


    /**
     * Responsible for creating all generated world assets, before it is showcased to the player
     */
    public void createWorldAssets() {
        generateTerrainFeatures(TerrainObject.Feature.LAKE, 0.6f, 100f);
        generateTerrainFeatures(TerrainObject.Feature.ROCK, 0.75f, 150f);
        generateTerrainFeatures(TerrainObject.Feature.TREE, 0.65f, 150f);

        // Places at least one lake tile down on the map - at a randomly generated location - if none were generated in the perlin noise
        if (mapObjects.getTerrainObjects().size == 0) {
            TerrainObject asset = new TerrainObject(new Random().nextInt(0, WIDTH), new Random().nextInt(0, HEIGHT), TerrainObject.Feature.LAKE);
            addMapObject(asset);
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
     * @param mapObject Building to add to the world
     * @return true if the placement was successful
     */
    public boolean addMapObject(MapObject mapObject) {
        mapObject.updateGridCoords();
        if (mapObject.getGridCoords().x < GridUtils.GRID_WIDTH && mapObject.getGridCoords().y < GridUtils.GRID_HEIGHT && !doesObjectOverlap(mapObject)) {
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
     * Update all MapObjects' states
     */
    public void updateMapObjects(float deltaTime) {
        // Some of the methods for satisfaction score use the building, these methods don't edit the building
        // but libGDX seems to get confused and break if a for (BuildingObject building : buildings) loop is used.
        for (int i = 0; i < mapObjects.getAll().size; i++) {
            MapObject mapObject = mapObjects.getAll().get(i);
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
    public void updateWorldState(BuildingObject building, boolean wasRemoved) {
        // Check if this changes which events can happen
        // Additional check (left hand side of &&) so we don't have to run the longer check every time
        if (wasRemoved) {
            if (isBuildingNearTerrain(building, TerrainObject.Feature.LAKE) && getCountOfTerrainNearBuildings(TerrainObject.Feature.LAKE) <= 1) {
                eventManager.disableEvent(GameEvent.FLOODING);
            }
            if (isBuildingNearTerrain(building, TerrainObject.Feature.TREE) && getCountOfTerrainNearBuildings(TerrainObject.Feature.TREE) <= 1) {
                eventManager.disableEvent(GameEvent.TREE_DAMAGE);
            }
            if (eventManager.isEventEnabled(GameEvent.TOO_MANY_BUILDINGS) && mapObjects.getUseCount(Use.TEACHING) <= TOO_MANY_LECTURE_BUILDINGS) {
                eventManager.disableEvent(GameEvent.TOO_MANY_BUILDINGS);
                // Disable the effect of the event as well if it has occurred
                activeEvents[GameEvent.TOO_MANY_BUILDINGS.ordinal()] = null;
            }
        } else {
            if (!eventManager.isEventEnabled(GameEvent.FLOODING) && isBuildingNearTerrain(building, TerrainObject.Feature.LAKE)) {
                eventManager.enableEvent(GameEvent.FLOODING);
            }
            if (!eventManager.isEventEnabled(GameEvent.TREE_DAMAGE) && isBuildingNearTerrain(building, TerrainObject.Feature.TREE)) {
                eventManager.enableEvent(GameEvent.TREE_DAMAGE);
            }
            if (!eventManager.isEventEnabled(GameEvent.TOO_MANY_BUILDINGS) && mapObjects.getUseCount(Use.TEACHING) >= TOO_MANY_LECTURE_BUILDINGS - 1) {
                eventManager.enableEvent(GameEvent.TOO_MANY_BUILDINGS);
            }
        }

        satisfaction.updateScore(building, !wasRemoved);
    }

    /**
     * Called when a world object has been added or removed to provide more information to the update the world's state.
     * Note that closing a building has the same effect on satisfaction as removing it.
     * @param terrain The object in question.
     * @param wasRemoved if true, the mapObject has just been removed. If false, the mapObject has just been added.
     */
    public void updateWorldState(TerrainObject terrain, boolean wasRemoved) {
        // Check if this changes which events can happen if this object is the first or last object next to a building
        if (wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 1) {
            if (terrain.feature == TerrainObject.Feature.LAKE) {
                eventManager.disableEvent(GameEvent.FLOODING);
            } else if (terrain.feature == TerrainObject.Feature.TREE) {
                eventManager.disableEvent(GameEvent.TREE_DAMAGE);
            }
        } else if (!wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 0) {
            if (terrain.feature == TerrainObject.Feature.LAKE && getCountOfTerrainNearBuildings(TerrainObject.Feature.LAKE) > 0) {
                eventManager.enableEvent(GameEvent.FLOODING);
            } else if (terrain.feature == TerrainObject.Feature.TREE && getCountOfTerrainNearBuildings(TerrainObject.Feature.TREE) > 0) {
                eventManager.enableEvent(GameEvent.TREE_DAMAGE);
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
        updateMapObjects(deltaTime);
        eventManager.processEvents(currentTime);
        // Maintain active events, removing them when necessary
        for (int i = 0; i < activeEventEndTime.length; i++) {
            if (activeEvents[i] != null && currentTime > activeEventEndTime[i]) {
                // Special effect for "Gym hype" to enable the possibility of winning the tournament if the user has placed enough gyms.
                if (i == GameEvent.GYM_HYPE.ordinal() && getCountOfSpecificBuilding(BuildingName.GYM) >= GYMS_FOR_TOURNAMENT_WIN) {
                    eventManager.enableEvent(GameEvent.TOURNAMENT_WON);
                }

                satisfaction.updateScore(activeEvents[i], false);
                activeEvents[i] = null;
                activeEventEndTime[i] = GAME_LENGTH_SECONDS + 1;
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

        return mapObjects.spaceIsOccupied(gridCoords.x, gridCoords.y);
    }

    public boolean getGameEnded() {
        return currentTime >= GAME_LENGTH_SECONDS;
    }


    /**
     * Gets the current game time.
     * @return The value of currentTime.
     */
    public float getCurrentTime() {
        return currentTime;
    }


    public void handleEvent(GameEvent event) {
        switch (event) {
            case FLOODING:
                for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.LAKE)) {
                    closeBuilding(building, 30f);
                }
                addActiveEvent(GameEvent.FLOODING, 30);
                break;
            case SEAGULL:
                if (mapObjects.getBuildings().size > 0) {
                    closeBuilding(getRandomBuilding(mapObjects.getBuildings()));
                }
                break;
            case TREE_HYPE:
                addActiveEvent(GameEvent.TREE_HYPE, 120);
                break;
            case TREE_DAMAGE:
                Array<BuildingObject> buildingsNearTrees = getBuildingsNearTerrain(TerrainObject.Feature.TREE);
                if (buildingsNearTrees.size > 0) {
                    destroyMapObject(getRandomBuilding(buildingsNearTrees));
                }
                break;
            case GOOSE_ATTACK:
                // Has no effect
                break;
            case LECTURE_VIEW:
                addActiveEvent(GameEvent.LECTURE_VIEW);
                break;
            case ROCK_CLIMBING:
                addActiveEvent(GameEvent.ROCK_CLIMBING, 120);
                break;
            case LONG_BOI_SIGHTING:
                addActiveEvent(GameEvent.LONG_BOI_SIGHTING, 10);
                break;
            case GYM_HYPE:
                addActiveEvent(GameEvent.GYM_HYPE, 120);
                break;
            case TOURNAMENT_WON:
                addActiveEvent(GameEvent.TOURNAMENT_WON);
                break;
            case TOO_MANY_BUILDINGS:
                addActiveEvent(GameEvent.TOO_MANY_BUILDINGS);
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
        building.resetConstruction(currentTime, timeSeconds);

        updateWorldState(building, true);
    }

    /**
     * Multiplies a building's efficiency by {@code multiplier} indefinitely, affecting satisfaction
     */
    public void modifyEfficiency(BuildingObject building, float multiplier) {
        modifyEfficiency(building, multiplier, GAME_LENGTH_SECONDS + 1);
    }

    /**
     * Multiplies a building's efficiency by {@code multiplier} for {@code timeSeconds}, affecting satisfaction
     */
    public void modifyEfficiency(BuildingObject building, float multiplier, float timeSeconds) {
        activeModifiers.add(new EfficiencyModifier(currentTime + timeSeconds, multiplier, building));
        building.setEfficiency(building.getEfficiency() * multiplier);

        // We do not update the world state here as the objects on the map have not changed
        satisfaction.updateScore();
    }

    public void destroyMapObject(MapObject mapObject) {
        mapObjects.remove(mapObject);

        if (mapObject instanceof BuildingObject buildingObject && buildingObject.built) {
            updateWorldState(buildingObject, true);
        }
        else if (mapObject instanceof TerrainObject terrainObject) {
            updateWorldState(terrainObject, true);
        }
    }

    public BuildingObject getRandomBuilding(Array<BuildingObject> buildings) {
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

    public boolean hasActiveEvent(GameEvent event) {
        return activeEvents[event.ordinal()] != null;
    }

    public int getCountOfSpecificBuilding(BuildingName name) {
        return mapObjects.getByType(name).size;
    }

    /**
     * Returns a list of buildings that are in any of the 8 tiles next to a given terrain feature
     * @param feature The type of feature, e.g. Lake
     */
    public Array<BuildingObject> getBuildingsNearTerrain(TerrainObject.Feature feature) {
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
    public boolean isBuildingNearTerrain(BuildingObject building, TerrainObject.Feature feature) {
        for (int x = Math.max(0, building.getGridCoords().x - 1); x <= Math.min(GridUtils.GRID_WIDTH - 1, building.getGridCoords().x + 1); x++) {
            for (int y = Math.max(0, building.getGridCoords().y - 1); y <= Math.min(GridUtils.GRID_HEIGHT - 1, building.getGridCoords().y + 1); y++) {
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
        for (int x = Math.max(0, coords.x - 1); x <= Math.min(GridUtils.GRID_WIDTH - 1, coords.x + 1); x++) {
            for (int y = Math.max(0, coords.y - 1); y <= Math.min(GridUtils.GRID_HEIGHT - 1, coords.y + 1); y++) {
                if (x != coords.x && y != coords.y) {
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
    public int getCountOfTerrainNearBuildings(TerrainObject.Feature feature) {
        int count = 0;
        for (TerrainObject terrainObject : mapObjects.getByFeature(feature)) {
            for (int x = Math.max(0, terrainObject.getGridCoords().x - 1); x <= Math.min(GridUtils.GRID_WIDTH - 1, terrainObject.getGridCoords().x + 1); x++) {
                for (int y = Math.max(0, terrainObject.getGridCoords().y - 1); y <= Math.min(GridUtils.GRID_HEIGHT - 1, terrainObject.getGridCoords().y + 1); y++) {
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
     * Save a user's score with the name into a file. If the name is already in the file, it is overwritten
     * @param name The university name
     * @param score The satisfaction score, between 0.0 and 1.0
     * @param filePath The path (relative to assets/..) of the file to create or save to.
     */
    public void saveScore(String name, float score, String filePath) {
        HashMap<String, Float> scores = loadScores(filePath);

        scores.put(name, score);

        // Output file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(scores);
            objectOutputStream.flush();
            objectOutputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns all the scores stored in the given file in descending order of score.
     * @param filePath The path (relative to assets/..) of the file to load from.
     * @return a list of Map.Entry<String, Float> where the String is the university name and the float is the
     * satisfaction score from 0.0 to 1.0
     */
    public ArrayList<Map.Entry<String, Float>> getTopScores(String filePath) {
        HashMap<String, Float> scores = loadScores(filePath);

        // Sort scores descending
        ArrayList<Map.Entry<String, Float>> list = new ArrayList<>(scores.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Collections.reverse(list);

        return list;
    }

    /**
     * Load the HashMap of scores from the file
     * @param filePath The path (relative to assets/..) of the file to load from.
     * @return A HashMap of names : scores
     */
    @SuppressWarnings("unchecked") // Try catch will prevent errors if the file is incorrect
    public HashMap<String, Float> loadScores(String filePath) {
        HashMap<String, Float> scores;

        // Read file
        try {
            FileInputStream fileInput = new FileInputStream(filePath);

            ObjectInputStream objectInput = new ObjectInputStream(fileInput);

            scores = (HashMap<String, Float>)objectInput.readObject();

            objectInput.close();
            fileInput.close();
        }
        catch (IOException ioException) {
            scores = new HashMap<>();
        }
        catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
            return null;
        }

        return scores;
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
     * @return The number of built buildings that provide a given use
     */
    public int getBuildingUseCount(Use use) {
        return mapObjects.getUseCount(use);
    }

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
     * Get the game time at which this active event will be removed. If the event is not active, returns {@value GAME_LENGTH_SECONDS} + 1
     * @param event The event to check
     * @return Time in seconds
     */
    public float getEndTimeOfActiveEvent(GameEvent event) {
        return activeEventEndTime[event.ordinal()];
    }
}
