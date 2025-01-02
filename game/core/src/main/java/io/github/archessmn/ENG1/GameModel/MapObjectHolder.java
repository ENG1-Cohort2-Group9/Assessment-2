package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for managing MapObjects. This class is not concerned with the rules of the game (e.g. checking if an object
 * overlaps) and merely provides an efficient data structure for accessing MapObjects by different criteria.
 * This class preserves types and its functionality will be impaired if type erasure occurs from outside
 */
public class MapObjectHolder {
    private HashMap<Class<? extends MapObject>, Array<MapObject>> typeIndex = new HashMap<>(); // Get objects by their class
    private HashMap<Use, Array<BuildingObject>> useIndex = new HashMap<>(); // Get objects by their use. Note that only BuildingObjects have a Use
    private HashMap<BuildingName, Array<BuildingObject>> buildingIndex = new HashMap<>(); // Get buildings by their type.
    private HashMap<TerrainObject.Feature, Array<TerrainObject>> featureIndex = new HashMap<>(); // Get objects by their feature. Note that only TerrainObjects have a Feature
    private MapObject[][] gridLookup; // Get objects by their map co-ordinates

    /**
     * MapObjectHolder constructor
     * @param worldWidth The number of squares wide the grid is
     * @param worldHeight The number of squares high the grid is
     */
    public MapObjectHolder(int worldWidth, int worldHeight) {
        gridLookup = new MapObject[worldWidth][worldHeight];
    }

    /**
     * Adds a mapObject to the relevant maps.
     * @throws IllegalArgumentException If the grid space the object is on is already occupied
     */
    public void add(MapObject mapObject) {

        if (gridLookup[mapObject.getGridCoords().x][mapObject.getGridCoords().y] != null) {
            throw new IllegalArgumentException("Grid space is already occupied");
        }

        typeIndex.computeIfAbsent(mapObject.getClass(), c -> new Array<>()).add(mapObject);
        gridLookup[mapObject.getGridCoords().x][mapObject.getGridCoords().y] = mapObject;

        // The following two statements check if the object is a generic class. If it is not, the generic class array is
        // updated with the object. This allows objects to be retrieved by superclass and subclass (for example you can
        // find a 'Pub' in the list of Pubs, Buildings, and MapObjects
        if (mapObject.getClass() != MapObject.class) {
            typeIndex.computeIfAbsent(MapObject.class, c -> new Array<>()).add(mapObject);
        }
        if (mapObject instanceof BuildingObject && mapObject.getClass() != BuildingObject.class) {
            typeIndex.computeIfAbsent(BuildingObject.class, c -> new Array<>()).add(mapObject);
        }


    }

    /**
     * Adds a buildingObject to the relevant maps ({@code useIndex} specifically).
     * @throws IllegalArgumentException If the grid space the object is on is already occupied
     */
    public void add(BuildingObject buildingObject) {
        add((MapObject)buildingObject);

        for (Use use : buildingObject.getUses()) {
            useIndex.computeIfAbsent(use, c -> new Array<>()).add(buildingObject);
        }
        buildingIndex.computeIfAbsent(buildingObject.type, c -> new Array<>()).add(buildingObject);
    }

    /**
     * Adds a terrainObject to the relevant maps ({@code featureIndex} specifically).
     * @throws IllegalArgumentException If the grid space the object is on is already occupied
     */
    public void add(TerrainObject terrainObject) {
        add((MapObject)terrainObject);

        featureIndex.computeIfAbsent(terrainObject.feature, c -> new Array<>()).add(terrainObject);
    }

    private void removeTypeFromList(Class<? extends MapObject> type) {

    }

    /**
     * Removes all references in this object to a BapObject
     * @throws IllegalArgumentException If the MapObject is not found where expected
     */
    public void remove(MapObject mapObject) {
        // Remove from lists (For example a Pub will be in the list of MapObjects, BuildingObjects, and Pubs)
        for (Array<MapObject> list : typeIndex.values()) {
            if (list.contains(mapObject, true)) {
                list.removeValue(mapObject, true);
            }
        }

        // Remove from grid
        if (gridLookup[mapObject.getGridCoords().x][mapObject.getGridCoords().y] == mapObject) {
            gridLookup[mapObject.getGridCoords().x][mapObject.getGridCoords().y] = null;
        } else {throw new IllegalArgumentException("MapObject not found on grid");}
    }

    /**
     * Removes all references in this object to a BuildingObject
     * @throws IllegalArgumentException If the BuildingObject is not found where expected
     */
    public void remove(BuildingObject buildingObject) {
        remove((MapObject)buildingObject);

        for (Use use : buildingObject.getUses()) {
            Array<BuildingObject> containingArray = useIndex.get(use);
            if (containingArray != null) {
                containingArray.removeValue(buildingObject, true);
            }
        }

        Array<BuildingObject> containingArray = buildingIndex.get(buildingObject.type);
        if (containingArray != null) {
            containingArray.removeValue(buildingObject, true);
        }
    }

    /**
     * Removes all references in this object to a TerrainObject
     * @throws IllegalArgumentException If the TerrainObject is not found where expected
     */
    public void remove(TerrainObject terrainObject) {
        remove((MapObject)terrainObject);

        Array<TerrainObject> containingArray = featureIndex.get(terrainObject.feature);
        if (containingArray != null) {
            containingArray.removeValue(terrainObject, true);
        }
    }

    /**
     * @return All buildings that have the Use, {@code use}, or an empty array if no placed buildings have this use.
     */
    public Array<BuildingObject> getByUse(Use use) {
        return useIndex.getOrDefault(use, new Array<>());
    }

    /**
     * @return All terrain objects with feature, {@code feature}, or an empty array if this feature has not been placed
     */
    public Array<TerrainObject> getByFeature(TerrainObject.Feature feature) {
        return featureIndex.getOrDefault(feature, new Array<>());
    }

    /**
     * Returns all MapObjects of type, {@code type}, or an empty array if no placed objects have this type.
     * @param type The class of an object, given by ClassName.class. Note that objects can be retrieved by superclass
     *             and subclass (e.g. you can find a {@link BuildingObject} in the list of {@link BuildingObject}s,
     *             and {@link MapObject}s
     * @return An array of the type specified by {@code type}
     */
    @SuppressWarnings("unchecked")
    public <T extends MapObject> Array<T> getByType(Class<? extends MapObject> type) {
        return (Array<T>)typeIndex.getOrDefault(type, new Array<>());
    }

    /**
     * Returns all Buildings of type, {@code type}, or an empty array if no placed buildings have this type.
     * @param type The type of building, e.g. building.type
     * @return An array of the BuildingObjects
     */
    public Array<BuildingObject> getByType(BuildingName type) {
        return buildingIndex.getOrDefault(type, new Array<>());
    }

    /**
     * @return The MapObject at the specified grid position, null if it is not occupied
     */
    public MapObject getByGrid(int gridX, int gridY) {
        return gridLookup[gridX][gridY];
    }

    /**
     * Wrapper of {@link #getByType(Class<? extends MapObject>) getByType} for more friendly access to all buildings
     */
    public Array<BuildingObject> getBuildings() {
        return getByType(BuildingObject.class);
    }

    /**
     * Wrapper of {@link #getByType(Class<? extends MapObject>) getByType} for more friendly access to all terrain objects
     */
    public Array<TerrainObject> getTerrainObjects() {
        return getByType(TerrainObject.class);
    }

    /**
     * Wrapper of {@link #getByType(Class<? extends MapObject>) getByType} for more friendly access to all MapObjects
     */
    public Array<MapObject> getAll() {
        return getByType(MapObject.class);
    }

    /**
     * @return the number of *built* buildings that provide a given {@code use}
     */
    public int getUseCount(Use use) {
        Array<BuildingObject> buildings = getByUse(use);
        int count = 0;
        for (BuildingObject buildingObject : buildings) {
            if (buildingObject.built) {
                count++;
            }
        }
        return count;
    }

    public boolean spaceIsOccupied(int x, int y) {
        return !(x < GridUtils.GRID_WIDTH && y < GridUtils.GRID_HEIGHT) || gridLookup[x][y] != null;
    }
}
