package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.Game;
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

    // The number of items in the Use enum, this value is used often, so it's stored to prevent repeated calculation.
    public final int useLength = Use.values().length;

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

    // Satisfaction score is the sum of the following 4 variables, allowing for easier access to each part of the score
    // This also means when one of these values needs to be reset, or altered, the satisfaction score will only update
    // on screen once when that calculation is complete.
    public float buildingDistancesScore;
    public float completionScore;
    public float eventsScore;
    public float buildingValuesScore;

    // Here the maximum value for each of these scores is set:

    public float buildingDistancesScoreCap = 40;
    public float completionScoreCap = 10;
    public float eventsScoreCap = 30;
    public float buildingValuesScoreCap = 20;

    // Some of these scores need/have more variables to help calculate them:

    // These help with buildingDistancesScore



    // The map currently is 11x9 tiles
    private static final int GRID_WIDTH = 11;
    private static final int GRID_HEIGHT = 9;

    // Stores events that have prolonged effects. Indices are preset for quicker lookup, even though instantaneous events are never stored here so the array can never be full.
    GameEvent[] activeEvents = new GameEvent[GameEvent.values().length];
    float[] activeEventEndTime = new float[GameEvent.values().length];
    Array<EfficiencyModifier> activeModifiers = new Array<>();
    // The maximum possible distance between two buildings, is the diagonal distance 1 less in both x and y,
    // than the number of tiles on the map
    float maxDistance = (float) (Math.sqrt(Math.pow(GRID_WIDTH-1, 2) + Math.pow(GRID_HEIGHT-1, 2)));

    // This is how much of the satisfaction score each building use pair accounts for.
    // The number of undirected use pairs is of the form n + n-1 + n-2... + n-n, as we want the first use connected to
    // all uses, then the second needs to connect to all uses except the first, as that's already been counted, the
    // third use ignores the first and second, and so on. So we use the sum of 1 to n formula for this, which is
    // (n *(n+1)) / 2 We divide the total percent allowed for average distances (40) by this number
    float percentPerUsePair = buildingDistancesScoreCap / (((float) useLength * ((float) useLength + 1)) / 2);
    // Allows the user to get the maximum satisfaction for a building use pair, if the pairs' average distance is
    // under 60% of the maximum possible distance. Anything over will give progressively less satisfaction.
    float maxScoreThreshold = maxDistance * 0.1f;


    // This 2D array stores the average distance between a pair of building types as an adjacency matrix
    // For example, you could set averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] to 0
    // However, averageDistances[Use.RECREATION.ordinal()][Use.TEACHING.ordinal()] and
    // averageDistances[Use.TEACHING.ordinal()][Use.RECREATION.ordinal()] should hold the same value.
    public float[][] averageDistances = new float[useLength][useLength];

    // When adding a new building, it will need to multiply the old average distance by how many building pairs there
    // were, For example, if the previous average distance between a teaching and accommodation building
    // (5 + 3 + 2 + 6 + 9)/ 5 = 6 (5 teaching buildings 1 accommodation), and we add a new accommodation building,
    // it will need to add 5 new values to the average distance, as the accommodation building will have a distance to
    // each of the 5 teaching buildings. For this reason, it's helpful to keep a count of how many building pairs each
    // average distance is made up of, so that we can know what to multiply the average by, and then increment it and
    // divide the new total distance by the new number of building pairs. This value is stored in this 2D array:
    public int[][] averageDistancesCount = new int[useLength][useLength];

    // Stores the weight for each use pair, allowing different building use pairs to give a bigger or smaller bonus
    // than others.
    public float[][] weightMatrix = new float[useLength][useLength];

    public float[][] averageDistanceScores = new float[useLength][useLength];

    // These help with completionScore:


    // Defines how much satisfaction score is gained for having > 0 of each building use type.
    public float completionScorePerUse = completionScoreCap / useLength;
    // Set to true when at least one of each use is placed, allows checks and calculation to be skipped
    public boolean isComplete = false;


    private float currentTime;
    private EventManager eventManager;
    private Random random = new Random();

    /**
     * Initialises an empty world and loads assets.
     * @param worldWidth Width to use for the usable world space
     * @param worldHeight Height to use for the usable world space
     */
    private void setUpWorld(int worldWidth, int worldHeight) {
        this.width = worldWidth;
        this.height = worldHeight;


        for (Use use : Use.values()) {
            buildingUseCounts.put(use, 0);
        }

        initialiseWeightMatrix();


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
     * @return true if the placement was successful
     */
    public boolean addBuilding(BuildingObject building) {
        if (!doesObjectOverlap(building)) {
            buildings.add(building);
            mapObjects.add(building);
            building.place();
            gridLookup[building.gridX][building.gridY] = building;

            if (!eventManager.isEventEnabled(GameEvent.Flooding) && isBuildingNearTerrain(building, TerrainObject.Feature.LAKE))
                eventManager.enableEvent(GameEvent.Flooding);
            if (!eventManager.isEventEnabled(GameEvent.TreeDamage) && isBuildingNearTerrain(building, TerrainObject.Feature.TREE))
                eventManager.enableEvent(GameEvent.TreeDamage);
            return true;
        }
        return false;
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
        // Some of the methods for satisfaction score use the building, these methods don't edit the building
        // but libGDX seems to get confused and break if a for (BuildingObject building : buildings) loop is used.
        for (int i = 0; i < buildings.size; i++) {
            BuildingObject building = buildings.get(i);
            if (building.placed && !building.built && currentTime > building.buildingCompletionTime) {
                // This will only trigger once (see '&& !building.built')
                building.built = true;

                // Update satisfaction score
                updateAverageDistances(building, true);
                updateBuildingDistancesScore(building);
                updateCompletionScore();
                updateSatisfactionScore();
                System.out.println("satisfaction score: " + satisfactionScore);

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
                updateEventScore();
            }
        }
        // Maintain active modifiers, removing them when necessary
        for (int i = 0; i < activeModifiers.size; i++) {
            if (currentTime > activeModifiers.get(i).endTime()) {
                BuildingObject building = activeModifiers.get(i).affectedBuilding();
                building.setEfficiency(building.getEfficiency() / activeModifiers.get(i).multiplier());
                activeModifiers.removeIndex(i);
                i--;
                updateEventScore();
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
        if (eventSatisfactionScore > 0.3f)
            eventSatisfactionScore = 0.3f;

        updateSatisfactionScore();
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
        if (building.built) {
            for (Use use : building.uses) {
                buildingUseCounts.put(use, buildingUseCounts.get(use) - 1);
            }
        }

        building.buildingCompletionTime = currentTime + timeSeconds;
        building.built = false;
        updateEventScore();
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
    }

    public void destroyTerrain(TerrainObject terrainObject) {
        // EVENT CHECKS TO BE COMPLETED HERE

        gridLookup[terrainObject.gridX][terrainObject.gridY] = null;
        terrain.removeValue(terrainObject, true);
        mapObjects.removeValue(terrainObject, true);

        updateEventScore();
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
                    destroyBuilding(getRandomBuilding(buildingsNearTrees));
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
        updateEventScore();
    }

    /**
     * Multiplies a building's efficiency by {@code multiplier} for {@code timeSeconds}, affecting satisfaction
     */
    public void modifyEfficiency(BuildingObject building, float multiplier, float timeSeconds) {
        activeModifiers.add(new EfficiencyModifier(currentTime + timeSeconds, multiplier, building));
        building.setEfficiency(building.getEfficiency() * multiplier);
        updateEventScore();
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
            return;
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
}
