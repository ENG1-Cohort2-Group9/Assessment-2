package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.math.MathUtils;
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

    public Array<BuildingObject> buildings;
    public Array<TerrainObject> terrain;
    public Array<MapObject> mapObjects;
    public MapObject[][] gridLookup; // Stores pointers to the buildings and terrain on the grid so that squares can be queried

    public HashMap<Use, Integer> buildingUseCounts = new HashMap<>();

    // An instance of the satisfaction class, this handles the satisfaction score, and all relevant calculations.
    public Satisfaction satisfaction;

    // How many *more* of building A than building B before issues arise. If this value is 3 and there are 2 lecture halls and 6 pubs, a "too many pubs" event is likely
    private final int buildingTypeTolerance = 3;

    // Stores events that have prolonged effects. Indices are preset for quicker lookup, even though instantaneous events are never stored here so the array can never be full.
    GameEvent[] activeEvents = new GameEvent[GameEvent.values().length];
    float[] activeEventEndTime = new float[GameEvent.values().length];
    Array<EfficiencyModifier> activeModifiers = new Array<>();

    private float currentTime;
    private EventManager eventManager;
    private Random random = new Random();


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


        for (Use use : Use.values()) {
            buildingUseCounts.put(use, 0);
        }

        satisfaction = new Satisfaction(this);

        // Pretty sure these lines aren't needed, can be done above
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
        if (terrain.size == 0) {
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
     * Adds a map object to the world if allowed, updating the relevant store(s)
     *
     * @param mapObject Map Object to add to the world
     * @return true if the placement was successful
     */
    public boolean addMapObject(MapObject mapObject) {
        if (!doesObjectOverlap(mapObject)) {
            if (mapObject instanceof BuildingObject building) {
                buildings.add(building);
            } else if (mapObject instanceof TerrainObject terrainObject) {
                terrain.add(terrainObject);
            }
            mapObjects.add(mapObject);
            mapObject.place();
            gridLookup[mapObject.gridX][mapObject.gridY] = mapObject;

            updateWorldState(mapObject, false);
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
        for (int i = 0; i < buildings.size; i++) {
            BuildingObject building = buildings.get(i);
            if (building.placed && !building.built && currentTime > building.buildingCompletionTime) {
                // This will only trigger once (see '&& !building.built')
                building.built = true;

                // Update satisfaction score when the building has finished being built.
                satisfaction.updateScore(building, true);

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
     * Gets the current game time.
     * @return The value of currentTime.
     */
    public float getCurrentTime() {
        return currentTime;
    }

    public boolean getGameEnded() {
        return currentTime >= GAME_LENGTH_SECONDS;
    }

    /**
     * Called when a world object has been added or removed to provide more information to the update the world's state.
     * Note that closing a building has the same effect on satisfaction as removing it.
     * @param mapObject The object in question.
     * @param wasRemoved if true, the mapObject has just been removed. If false, the mapObject has just been added.
     */
    public void updateWorldState(MapObject mapObject, boolean wasRemoved) {
        int addOrRemove = wasRemoved ? -1 : 1;
        // Check if this changes which events can happen
        if (mapObject instanceof BuildingObject building) {
            // Additional check (left hand side of &&) so we don't have to run the longer check every time
            if (wasRemoved) {
                if (isBuildingNearTerrain(building, TerrainObject.Feature.LAKE) && getBuildingsNearTerrain(TerrainObject.Feature.LAKE).size == 1) {
                    eventManager.disableEvent(GameEvent.Flooding);
                }
                if (isBuildingNearTerrain(building, TerrainObject.Feature.TREE) && getBuildingsNearTerrain(TerrainObject.Feature.TREE).size == 1) {
                    eventManager.disableEvent(GameEvent.TreeDamage);
                }
            } else {
                if (!eventManager.isEventEnabled(GameEvent.Flooding) && isBuildingNearTerrain(building, TerrainObject.Feature.LAKE)) {
                    eventManager.disableEvent(GameEvent.Flooding);
                }
                if (!eventManager.isEventEnabled(GameEvent.TreeDamage) && isBuildingNearTerrain(building, TerrainObject.Feature.TREE)) {
                    eventManager.disableEvent(GameEvent.TreeDamage);
                }
            }

            // Update building counters
            if (building.built) {
                for (Use use : building.uses) {
                    buildingUseCounts.put(use, buildingUseCounts.get(use) + addOrRemove);
                }
            }

        } else if (mapObject instanceof TerrainObject terrain) {
            // Check if this object is the first or last object next to a building
            if (wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 1) {
                if (terrain.feature == TerrainObject.Feature.LAKE) {
                    eventManager.disableEvent(GameEvent.Flooding);
                } else if (terrain.feature == TerrainObject.Feature.TREE) {
                    eventManager.disableEvent(GameEvent.TreeDamage);
                }
            } else if (!wasRemoved && getCountOfTerrainNearBuildings(terrain.feature) == 0) {
                if (terrain.feature == TerrainObject.Feature.LAKE) {
                    eventManager.enableEvent(GameEvent.Flooding);
                } else if (terrain.feature == TerrainObject.Feature.TREE) {
                    eventManager.enableEvent(GameEvent.TreeDamage);
                }
            }
        }


    }

    public void updateEventScore() {
        // Events
        float eventSatisfactionScore = 0f;
        if (activeEvents[GameEvent.TreeHype.ordinal()] != null) {
            // Accommodation near trees is more effective
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.TREE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.ACCOMMODATION) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
        }
        if (activeEvents[GameEvent.LectureView.ordinal()] != null) {
            // Teaching near lakes and trees is more effective
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.LAKE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.TEACHING) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.TREE)) {
                for (Use use : building.getUses()) {
                    if (use == Use.TEACHING) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
        }
        if (activeEvents[GameEvent.RockClimbing.ordinal()] != null) {
            // Accommodation near rocks is more effective
            for (BuildingObject building : getBuildingsNearTerrain(TerrainObject.Feature.ROCK)) {
                for (Use use : building.getUses()) {
                    if (use == Use.ACCOMMODATION) {
                        eventSatisfactionScore += 0.05f;
                    }
                }
            }
        }
        if (activeEvents[GameEvent.GymHype.ordinal()] != null) {
            // Gyms are more effective
            for (BuildingObject building : buildings) {
                if (building instanceof GymBuilding) {
                    eventSatisfactionScore += 0.05f;
                }
            }
        }
        if (activeEvents[GameEvent.SportsWon.ordinal()] != null) {
            // Permanent boost to satisfaction
            eventSatisfactionScore += 0.1f;
        }
        if (activeEvents[GameEvent.TooMuchTeaching.ordinal()] != null) {
            // Reduces overall event satisfaction
            eventSatisfactionScore -= 0.1f;
        }
        if (activeEvents[GameEvent.TooManyPubs.ordinal()] != null) {
            // Reduces overall event satisfaction
            eventSatisfactionScore -= 0.1f;
        }
        if (activeEvents[GameEvent.TooMuchHousing.ordinal()] != null) {
            // Reduces overall event satisfaction
            eventSatisfactionScore -= 0.1f;
        }
        if (activeEvents[GameEvent.LongBoiSighting.ordinal()] != null) {
            eventSatisfactionScore = 0.3f; // This event is very powerful, but only lasts a short time
        }
        eventSatisfactionScore = MathUtils.clamp(eventSatisfactionScore, 0f, 0.3f);

        updateSatisfactionScore();
    }

    /**
     * Removes active events that have been resolved. This is typically when an active event is caused by a circumstance
     * (e.g. too many pubs) and that condition is no longer met. This does not manage active event timers and so should
     * not be called every frame, only when the world state is updated
     * @param building The building that has been added or removed
     */
    private void pruneActiveEvents(BuildingObject building) {
        // The outer conditions are used to evaluate whether we should even check to change the event. For instance, if
        // the affected building is neither a teaching building nor a pub, there will be no effect on the number of each
        // building compared to the last time this was called.
        if (activeEvents[GameEvent.TooManyPubs.ordinal()] != null) {
            for (Use use : building.getUses()) {
                if (use == Use.TEACHING || building instanceof Pub) {
                    int pubCount = 0;
                    for (BuildingObject worldBuilding : buildings) {
                        if (building instanceof Pub) {
                            pubCount++;
                        }
                    }
                    if (pubCount <= buildingUseCounts.get(Use.TEACHING) + buildingTypeTolerance)
                        activeEvents[GameEvent.TooManyPubs.ordinal()] = null;
                    break;
                }
            }
        }
        if (activeEvents[GameEvent.TooMuchTeaching.ordinal()] != null) {
            if (buildingUseCounts.get(Use.TEACHING) <= buildingUseCounts.get(Use.ACCOMMODATION) + buildingTypeTolerance)
                activeEvents[GameEvent.TooMuchTeaching.ordinal()] = null;
        }
        if (activeEvents[GameEvent.TooMuchHousing.ordinal()] != null) {
            if (buildingUseCounts.get(Use.ACCOMMODATION) <= buildingUseCounts.get(Use.TEACHING) + buildingTypeTolerance)
                activeEvents[GameEvent.TooMuchTeaching.ordinal()] = null;
        }
    }


    /**
     * Updates satisfactionScore by assigning it to the sum of its components.
     */
    public void updateSatisfactionScore() {

        buildingDistancesScore = applyCap(buildingDistancesScore, buildingDistancesScoreCap);
        completionScore = applyCap(completionScore, completionScoreCap);
        eventsScore = applyCap(eventsScore, eventsScoreCap);
        buildingValuesScore = applyCap(buildingValuesScore, buildingValuesScoreCap);

        // If no accommodation buildings are placed yet, the buildingDistancesScore is ignored.
        // (Since no one lives on campus to care about it)
        if (buildingUseCounts.get(Use.ACCOMMODATION) == 0) {
            satisfactionScore = completionScore + eventsScore + buildingValuesScore;
        }
        else {
            satisfactionScore = buildingDistancesScore + completionScore + eventsScore + buildingValuesScore;
        }
    }


    /**
     * Helper method to apply cap each component of satisfactionScore.
     * @param score the current score
     * @param cap the maximum allowed value for the score
     * @return the capped score
     */
    private float applyCap(double score, float cap) {
        return (float) Math.min(score, cap);
    }


    /**
     * Updates the average distance between each building use pair, this is further explained in the methods' comments,
     * and in the description of averageDistances.
     * @param building The building that was added to/removed from the map, its uses are iterated through to know which
     *                 building use pairs need to have their average distances updated.
     */
    public void updateAverageDistances(BuildingObject building, boolean placed) {
        // updateFactor is 1 if the building passed to this method was just placed, -1 if it was removed.

        // When it is 1, the distance found for each use pair is added to the corresponding averageDistance
        // index, the new total distance is divided by (numberOfPairs + 1) to find the new averageDistance, and the
        // corresponding averageDistancesCount is incremented by 1.

        // When it is -1, the distance found for each use pair is subtracted from the corresponding averageDistance
        // index, the new total distance is divided by (numberOfPairs - 1) to find the new averageDistance, and the
        // corresponding averageDistancesCount is decremented by 1.
        int updateFactor;
        if (placed) {
            updateFactor = 1;
        }
        else {
            updateFactor = -1;
        }

        // Iterates through each use the building passed to this method has
        for (Use use1 : building.getUses()) {
            // Iterates through all buildings currently placed on the map
            for (BuildingObject comparisonBuilding : buildings) {
                // Distance is the diagonal distance between the building passed to this method, and the current
                // comparisonBuilding.
                float distance = (float) Math.sqrt(Math.pow(building.gridX - comparisonBuilding.gridX, 2) +
                    Math.pow(building.gridY - comparisonBuilding.gridY, 2));

                // Iterates through each use the comparison building has, except when the building passed to this method
                // and the comparison building are the same building.
                if (distance != 0) {
                    for (Use use2 : comparisonBuilding.getUses()) {
                        // The averageDistancesCount array stores average distances, this is calculated as each use
                        // count in the use pair multiplied together. (e.g., 5 teaching * 3 accommodation = 15 pairs).
                        // If we label each pair in the form T1A1 = distance between teaching building 1 and
                        // accommodation building 1, the average distance between all teaching and accommodation
                        // buildings would be: (T1A1 + T1A2 + T1A3 + T2A1 +...+ T5A3)/(5*3)

                        // numberOfPairs is the number of pairs the average distance between the two uses is made up of
                        // For example, if there were 4 teaching and 3 accommodation buildings before the building
                        // passed to this method was added/removed, then numberOfPairs would be 12.
                        int numberOfPairs;

                        // Update the average distance between the current pair of uses.
                        // If statement ensures that only the upper triangle of the adjacency matrix is actually
                        // updated, meaning use pairs are only ever updated and later checked, where the first use
                        // is <= to the second use. (based of the ordinal of the use)
                        if (use1.ordinal() < use2.ordinal()) {
                            numberOfPairs = averageDistancesCount[use1.ordinal()][use2.ordinal()];
                            averageDistances[use1.ordinal()][use2.ordinal()] = (averageDistances[use1.ordinal()][use2.ordinal()]
                                * numberOfPairs + distance * updateFactor) / (numberOfPairs + updateFactor);

                            // Update the number of building pairs used for the average distance between use1 and use2
                            averageDistancesCount[use1.ordinal()][use2.ordinal()] += updateFactor;
                        }
                        else {
                            numberOfPairs = averageDistancesCount[use2.ordinal()][use1.ordinal()];
                            averageDistances[use2.ordinal()][use1.ordinal()] = (averageDistances[use2.ordinal()][use1.ordinal()]
                                * numberOfPairs + distance * updateFactor) / (numberOfPairs + updateFactor);

                            // Update the number of building pairs used for the average distance between use2 and use1
                            averageDistancesCount[use2.ordinal()][use1.ordinal()] += updateFactor;
                        }
                    }
                }
            }
        }
    }


    /**
     * Updates the buildingDistancesScore when a building is added or deleted, assumes the building passed to this
     * method was successfully placed, or successfully deleted.
     * Also assumes that averageDistances has already been updated accordingly.
     *
     * @param building The building that was just placed or deleted.
     */
    public void updateBuildingDistancesScore(BuildingObject building) {
        for (Use use1 : building.getUses()) {
            for (Use use2 : Use.values()) {
                // Only iterates over the upper triangle of the adjacency matrix
                if (use2.ordinal() >= use1.ordinal()) {
                    // score is used to prevent duplicate calculation
                    float score = calculateScoreBonus(getWeight(use1, use2), use1, use2);
                    // Add the new score for this use pair, and subtract the previous score for this use pair
                    // from buildingDistancesScore
                    buildingDistancesScore += score - averageDistanceScores[use1.ordinal()][use2.ordinal()];
                    // Save the new score for this use pair so the above line will work when next the use pair is
                    // next updated.
                    averageDistanceScores[use1.ordinal()][use2.ordinal()] = score;
                }
                // Same logic is used as the if statement, but since only the upper triangle of the adjacency
                // matrices are used, use1 and use2 need to be swapped if use1 is larger than use2.
                else {
                    float score = calculateScoreBonus(getWeight(use2, use1), use2, use1);
                    buildingDistancesScore += score - averageDistanceScores[use2.ordinal()][use1.ordinal()];
                    averageDistanceScores[use2.ordinal()][use1.ordinal()] = score;
                }
            }
        }
    }


    /**
     * Calculates what portion of the maximum score bonus possible for the use pair passed is achieved, based on the
     * difference between the average distance for that pair, the maximum possible distance, and the threshold set.
     *
     * @param multiplier Allows the different pairs to be weighted differently, but requires careful tracking of all
     *                   multipliers, such that the score doesn't go over what is allowed.
     * @param use1 The first use of the use pair.
     * @param use2 The second use of the use pair.
     * @return The score bonus from the average distance between the use pair passed to this method.
     */
    public float calculateScoreBonus(float multiplier, Use use1, Use use2) {
        // Prevents adding to satisfaction score for buildings uses that aren't yet placed.
        if (buildingUseCounts.get(use1) == 0 || buildingUseCounts.get(use2) == 0) {
            return 0;
        }
        // If the distance between the use pair is under the maxScoreThreshold, the max satisfaction is given, as long
        // as the distance is greater than 0.
        else if (averageDistances[use1.ordinal()][use2.ordinal()] <= maxScoreThreshold) {
            return percentPerUsePair * multiplier;
        }
        // If the distance between the use pair is not under the maxScoreThreshold, then the score given is calculated
        // as follows. If the max score was 10, and the threshold 60%, anything between 6 and 10 gives progressively
        // less score. In this example, that would mean 7 gives 75% of the score, 8 gives 50%, 8.5 gives 62.5% etc.
        // To calculate this, we would do maxDistance - distance, divided by maxDistance - threshold, which would be
        // (10 - 7) / (10 - 6) = 3/4 = 0.75 (using distance as 7 here).
        else {
            return (percentPerUsePair * multiplier) *
                ((maxDistance - averageDistances[use1.ordinal()][use2.ordinal()]) / (maxDistance - maxScoreThreshold));
        }
    }


    /**
     * Initialises the weight matrix according to values selected by the developer(s).
     */
    private void initialiseWeightMatrix() {
        // This needs to be set manually, but doing so here avoids needing this if else statement to be used to find the
        // weight for a use pair, instead the weightMatrix can be referenced.
        for (Use use1 : Use.values()) {
            for (Use use2 : Use.values()) {
                // Only iterates over the upper triangle of the adjacency matrix
                if (use2.ordinal() >= use1.ordinal()) {
                    if (use1 == Use.TEACHING && use2 == Use.ACCOMMODATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    } else if (use1 == Use.TEACHING && use2 == Use.TEACHING) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    } else if (use1 == Use.ACCOMMODATION && use2 == Use.ACCOMMODATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.5f;
                    } else if (use1 == Use.ACCOMMODATION && use2 == Use.CAFETERIA) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 2.25f;
                    } else if (use1 == Use.ACCOMMODATION && use2 == Use.RECREATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.75f;
                    } else if (use1 == Use.CAFETERIA && use2 == Use.RECREATION) {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 0.5f;
                    } else {
                        weightMatrix[use1.ordinal()][use2.ordinal()] = 1.0f; // Default weight
                    }
                }
            }
        }
    }


    /**
     * Allows for neater access to the weighting of a use pair, assumes order of the two uses was checked before being
     * passed to this method.
     * @param use1 The first use in the pair, should be smaller (listed earlier in the Use enum) than use2.
     * @param use2 The second use in the pair, should be larger (listed later in the Use enum) than use1.
     * @return The weighting associated with the use pair use1 --> use2.
     */
    private float getWeight(Use use1, Use use2) {
        return weightMatrix[use1.ordinal()][use2.ordinal()];
    }


    /**
     * For each building placed while the building counters are not all > 0,
     * the completionScore is incremented by completionScorePerUse
     */
    public float updateCompletionScore() {
        // reset completionScore to avoid falsely maxing it.
        if (isComplete) {
            return completionScoreCap;
        }
        else {
            completionScore = 0;
            for (Use use : Use.values()) {
                if (buildingUseCounts.get(use) != 0) {
                    completionScore += completionScorePerUse;
                }
            }
            // Used >= in case floats don't quite equal each other.
            if (completionScore >= completionScoreCap) {
                isComplete = true;
            }
        }
        return completionScore;
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
        satisfaction.updateScore();
    }

    public void demolishBuilding(BuildingObject building) {
        gridLookup[building.gridX][building.gridY] = null;
        buildings.removeValue(building, true);
        mapObjects.removeValue(building, true);

        updateWorldState(building, true);
        satisfaction.updateScore();
    }

    public void destroyTerrain(TerrainObject terrainObject) {

        gridLookup[terrainObject.gridX][terrainObject.gridY] = null;
        terrain.removeValue(terrainObject, true);
        mapObjects.removeValue(terrainObject, true);

        updateWorldState(terrainObject, true);
        satisfaction.updateScore();
    }

    public BuildingObject getRandomBuilding(Array<BuildingObject> buildings) {
        if (buildings.size == 0)
            return null;
        return buildings.get(random.nextInt(buildings.size));
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
                    demolishBuilding(getRandomBuilding(buildingsNearTrees));
                }
                break;
            case GooseAttack:
                // Has no effect
                break;
            case LectureView:
                addActiveEvent(GameEvent.LectureView, GAME_LENGTH_SECONDS + 1);
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
            case SportsWon:
                addActiveEvent(GameEvent.SportsWon, GAME_LENGTH_SECONDS + 1);
                break;
            case TooMuchHousing:
                addActiveEvent(GameEvent.TooMuchHousing, GAME_LENGTH_SECONDS + 1);
                break;
            case TooManyPubs:
                addActiveEvent(GameEvent.TooManyPubs, GAME_LENGTH_SECONDS + 1);
                break;
            case TooMuchTeaching:
                addActiveEvent(GameEvent.TooMuchTeaching, GAME_LENGTH_SECONDS + 1);
                break;
            default:
                throw new RuntimeException("Unknown event type: " + event);
        }
    }

    /**
     * Adds an effect to the current game for {@code timeSeconds} seconds
     * @param event The event associated with the effect
     */
    public void addActiveEvent(GameEvent event, float timeSeconds) {
        activeEvents[event.ordinal()] = event;
        activeEventEndTime[event.ordinal()] = currentTime + timeSeconds;
        satisfaction.updateScore();
    }

    /**
     * Multiplies a building's efficiency by {@code multiplier} for {@code timeSeconds}, affecting satisfaction
     */
    public void modifyEfficiency(BuildingObject building, float multiplier, float timeSeconds) {
        activeModifiers.add(new EfficiencyModifier(currentTime + timeSeconds, multiplier, building));
        building.setEfficiency(building.getEfficiency() * multiplier);
        satisfaction.updateScore();
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
     * @param feature e.g. Lake, Trees, etc.
     * @return The number of terrain tiles of type {@code feature} that is adjacent to any building
     */
    public int getCountOfTerrainNearBuildings(TerrainObject.Feature feature) {
        int count = 0;
        for (TerrainObject terrainObject : terrain) {
            if (terrainObject.feature == feature) {
                for (int x = Math.max(0, terrainObject.gridX - 1); x <= Math.min(width - 1, terrainObject.gridX + 1); x++) {
                    for (int y = Math.max(0, terrainObject.gridY - 1); y <= Math.min(height - 1, terrainObject.gridY + 1); y++) {
                        if (gridLookup[x][y] instanceof BuildingObject) {
                            count += 1;
                        }
                    }
                }
            }
        }
        return count;
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
     * Gets the HashMap between a building use, and the number of instances of that use currently on the map.
     * @return The buildingUseCounts HashMap.
     */
    public HashMap<Use, Integer> getBuildingUseCounts() {
        return buildingUseCounts;
    }


    /**
     * Get the Array containing the buildings placed on the map.
     * @return The Array<BuildingObject> buildings variable.
     */
    public Array<BuildingObject> getBuildings() {
        return buildings;
    }


    /**
     * Gets the List of currently active game events.
     * @return The activeEvents list.
     */
    public GameEvent[] getActiveEvents() {
        return activeEvents;
    }
}
