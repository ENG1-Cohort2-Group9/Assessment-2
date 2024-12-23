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

    public int width, height;
    public final float GAME_LENGTH_SECONDS = 300;

    private MapObjectHolder mapObjects = new MapObjectHolder(GridUtils.GRID_WIDTH, GridUtils.GRID_HEIGHT);

    // An instance of the satisfaction class, this handles the satisfaction score, and all relevant calculations.
    public Satisfaction satisfaction;

    // Stores events that have prolonged effects. Indices are preset for quicker lookup, even though instantaneous events are never stored here so the array can never be full.
    GameEvent[] activeEvents = new GameEvent[GameEvent.values().length];
    float[] activeEventEndTime = new float[GameEvent.values().length];
    Array<EfficiencyModifier> activeModifiers = new Array<>();

    private float currentTime;
    private EventManager eventManager;
    private Random random = new Random();

    public final int TOO_MANY_LECTURE_BUILDINGS = 15; // How many lecture buildings are needed to allow the "too many buildings" event to occur
    private final int GYMS_FOR_TOURNAMENT_WIN = 10; // The number of gyms needed to allow the university to win a sports event.

    /**
     * Initialises an empty world and loads assets.
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    private void setUpWorld(int worldWidth, int worldHeight, EventManager eventManager) {
        this.width = worldWidth;
        this.height = worldHeight;



        // These can only happen when a building is near these terrain types
        eventManager.disableEvent(GameEvent.Flooding);
        eventManager.disableEvent(GameEvent.TreeDamage);
        // These are conditional on the player's actions
        eventManager.disableEvent(GameEvent.TournamentWon);
        eventManager.disableEvent(GameEvent.TooManyBuildings);

        satisfaction = new Satisfaction(this);

        createWorldAssets();
    }


    /**
     * Initialises the game world
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    public World(int worldWidth, int worldHeight) {
        eventManager = new EventManager(new GameEventListener[] { new GameEventListener(this::handleEvent) }, GAME_LENGTH_SECONDS);
        setUpWorld(worldWidth, worldHeight, eventManager);
    }


    /**
     * Initialises the game world with an extra event listener for event handling outside of this class
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     * @param additionalEventListener an extra event listener for event handling outside of this class. Can be used for rendering effects
     */
    public World(int worldWidth, int worldHeight, GameEventListener additionalEventListener) {
        eventManager = new EventManager(new GameEventListener[] { new GameEventListener(this::handleEvent), additionalEventListener }, GAME_LENGTH_SECONDS);
        setUpWorld(worldWidth, worldHeight, eventManager);
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
            TerrainObject asset = new TerrainObject(new Random().nextInt(0, width), new Random().nextInt(0, height), TerrainObject.Feature.LAKE);
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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width - 60; x++) {
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
        if (mapObject.getGridCoords().x < GridUtils.GRID_WIDTH && mapObject.getGridCoords().y < GridUtils.GRID_HEIGHT && !doesObjectOverlap(mapObject)) {
            mapObject.place();

            if (mapObject instanceof BuildingObject buildingObject) {
                buildingObject.resetBuildingConstruction(currentTime);
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
     * Update all buildings' states
     */
    public void updateBuildings(float deltaTime) {
        // Some of the methods for satisfaction score use the building, these methods don't edit the building
        // but libGDX seems to get confused and break if a for (BuildingObject building : buildings) loop is used.
        for (int i = 0; i < mapObjects.getBuildings().size; i++) {
            BuildingObject building = mapObjects.getBuildings().get(i);
            if (!building.built && currentTime > building.buildingCompletionTime) {
                // This will only trigger once (see '&& !building.built')
                building.built = true;

                updateWorldState(building, false);
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
                eventManager.disableEvent(GameEvent.Flooding);
            }
            if (isBuildingNearTerrain(building, TerrainObject.Feature.TREE) && getCountOfTerrainNearBuildings(TerrainObject.Feature.TREE) <= 1) {
                eventManager.disableEvent(GameEvent.TreeDamage);
            }
            if (eventManager.isEventEnabled(GameEvent.TooManyBuildings) && mapObjects.getUseCount(Use.TEACHING) <= TOO_MANY_LECTURE_BUILDINGS) {
                eventManager.disableEvent(GameEvent.TooManyBuildings);
                // Disable the effect of the event as well if it has occurred
                activeEvents[GameEvent.TooManyBuildings.ordinal()] = null;
            }
        } else {
            if (!eventManager.isEventEnabled(GameEvent.Flooding) && isBuildingNearTerrain(building, TerrainObject.Feature.LAKE)) {
                eventManager.enableEvent(GameEvent.Flooding);
            }
            if (!eventManager.isEventEnabled(GameEvent.TreeDamage) && isBuildingNearTerrain(building, TerrainObject.Feature.TREE)) {
                eventManager.enableEvent(GameEvent.TreeDamage);
            }
            if (!eventManager.isEventEnabled(GameEvent.TooManyBuildings) && mapObjects.getUseCount(Use.TEACHING) >= TOO_MANY_LECTURE_BUILDINGS - 1) {
                eventManager.enableEvent(GameEvent.TooManyBuildings);
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
                eventManager.disableEvent(GameEvent.Flooding);
            } else if (terrain.feature == TerrainObject.Feature.TREE) {
                eventManager.disableEvent(GameEvent.TreeDamage);
            }
        } else if (!wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 0) {
            if (terrain.feature == TerrainObject.Feature.LAKE && getCountOfTerrainNearBuildings(TerrainObject.Feature.LAKE) > 0) {
                eventManager.enableEvent(GameEvent.Flooding);
            } else if (terrain.feature == TerrainObject.Feature.TREE && getCountOfTerrainNearBuildings(TerrainObject.Feature.TREE) > 0) {
                eventManager.enableEvent(GameEvent.TreeDamage);
            }
        }

        satisfaction.updateScore();
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
                // Special effect for "Gym hype" to enable the possibility of winning the tournament if the user has placed enough gyms.
                if (i == GameEvent.GymHype.ordinal() && getCountOfSpecificBuilding(GymBuilding.class) >= GYMS_FOR_TOURNAMENT_WIN) {
                    eventManager.enableEvent(GameEvent.TournamentWon);
                }

                activeEvents[i] = null;
                activeEventEndTime[i] = GAME_LENGTH_SECONDS + 1;
                satisfaction.updateScore();
            }
        }
        // Maintain active modifiers, removing them when necessary
        for (int i = 0; i < activeModifiers.size; i++) {
            if (currentTime > activeModifiers.get(i).endTime()) {
                BuildingObject building = activeModifiers.get(i).affectedBuilding();
                building.setEfficiency(building.getEfficiency() / activeModifiers.get(i).multiplier());
                activeModifiers.removeIndex(i);
                i--;
                satisfaction.updateScore();
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

        for (MapObject mapObject : mapObjects.getAll()) {
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


    /**
     * Gets the current game time.
     * @return The value of currentTime.
     */
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
                if (mapObjects.getBuildings().size > 0) {
                    modifyEfficiency(getRandomBuilding(mapObjects.getBuildings()), 0.5f);
                }
                break;
            case Seagull:
                if (mapObjects.getBuildings().size > 0) {
                    closeBuilding(getRandomBuilding(mapObjects.getBuildings()));
                }
                break;
            case TreeHype:
                addActiveEvent(GameEvent.TreeHype, 120);
                break;
            case TreeDamage:
                Array<BuildingObject> buildingsNearTrees = getBuildingsNearTerrain(TerrainObject.Feature.TREE);
                if (buildingsNearTrees.size > 0) {
                    demolishBuilding(getRandomBuilding(buildingsNearTrees));
                }
                break;
            case GooseAttack:
                // Has no effect
                break;
            case LectureView:
                addActiveEvent(GameEvent.LectureView);
                break;
            case RockClimbing:
                addActiveEvent(GameEvent.RockClimbing, 120);
                break;
            case LongBoiSighting:
                addActiveEvent(GameEvent.LongBoiSighting, 10);
                break;
            case GymHype:
                addActiveEvent(GameEvent.GymHype, 120);
                break;
            case TournamentWon:
                addActiveEvent(GameEvent.TournamentWon);
                break;
            case TooManyBuildings:
                addActiveEvent(GameEvent.TooManyBuildings);
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
        building.buildingCompletionTime = currentTime + timeSeconds;
        building.built = false;

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

    public void demolishBuilding(BuildingObject building) {
        mapObjects.remove(building);

        if (building.built) { // We only update the world state if this building has been completed. Otherwise, it will not have changed the world space in the first place
            updateWorldState(building, true);
        }
    }

    public void destroyTerrain(TerrainObject terrainObject) {
        mapObjects.remove(terrainObject);

        updateWorldState(terrainObject, true);
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
        satisfaction.updateScore();
    }

    public <T extends BuildingObject> int getCountOfSpecificBuilding(Class<T> buildingClass) {
        return mapObjects.getByType(buildingClass).size;
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
        for (int x = Math.max(0, building.gridX - 1); x <= Math.min(GridUtils.GRID_WIDTH - 1, building.gridX + 1); x++) {
            for (int y = Math.max(0, building.gridY - 1); y <= Math.min(GridUtils.GRID_HEIGHT - 1, building.gridY + 1); y++) {
                if (mapObjects.getByGrid(x,y) instanceof TerrainObject && ((TerrainObject) mapObjects.getByGrid(x,y)).feature == feature) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param feature e.g. Lake, Trees, etc.
     * @return The number of terrain tiles of type {@code feature} that is adjacent to any building
     */
    public int getCountOfTerrainNearBuildings(TerrainObject.Feature feature) {
        int count = 0;
        for (TerrainObject terrainObject : mapObjects.getByFeature(feature)) {
            for (int x = Math.max(0, terrainObject.gridX - 1); x <= Math.min(GridUtils.GRID_WIDTH - 1, terrainObject.gridX + 1); x++) {
                for (int y = Math.max(0, terrainObject.gridY - 1); y <= Math.min(GridUtils.GRID_HEIGHT - 1, terrainObject.gridY + 1); y++) {
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
     * Gets a HashMap between a building use, and the number of built instances of that use currently on the map.
     * @return The buildingUseCounts HashMap.
     */
    public HashMap<Use, Integer> getBuildingUseCounts() {
        return mapObjects.getBuildingUseCounts();
    }

    /**
     * Gets the List of currently active game events.
     * @return The activeEvents list.
     */
    public GameEvent[] getActiveEvents() {
        return activeEvents;
    }

    public Array<MapObject> getMapObjects() {
        return mapObjects.getAll();
    }

    public Array<BuildingObject> getBuildings() {
        return mapObjects.getBuildings();
    }
}
