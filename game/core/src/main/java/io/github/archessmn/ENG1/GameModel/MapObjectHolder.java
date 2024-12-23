package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.utils.Array;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainObject;
import io.github.archessmn.ENG1.GameModel.Objects.Use;

import java.util.HashMap;

public class MapObjectHolder {
    private HashMap<Class<? extends MapObject>, Array<MapObject>> typeIndex = new HashMap<>(); // Get objects by their class
    private HashMap<Use, Array<BuildingObject>> useIndex = new HashMap<>(); // Get objects by their use. Note that only BuildingObjects have a Use
    private HashMap<TerrainObject.Feature, Array<TerrainObject>> featureIndex = new HashMap<>(); // Get objects by their feature. Note that only TerrainObjects have a Feature
    private MapObject[][] gridLookup; // Get objects by their map co-ordinates

    public MapObjectHolder(int worldWidth, int worldHeight) {
        gridLookup = new MapObject[worldWidth][worldHeight];
    }

    public void add(MapObject mapObject) {
        typeIndex.computeIfAbsent(mapObject.getClass(), c -> new Array<>()).add(mapObject);
        gridLookup[mapObject.gridX][mapObject.gridY] = mapObject;
    }

    public void add(BuildingObject buildingObject) {
        add((MapObject)buildingObject);

        for (Use use : buildingObject.getUses()) {
            useIndex.computeIfAbsent(use, c -> new Array<>()).add(buildingObject);
        }
    }

    public void add(TerrainObject terrainObject) {
        add((MapObject)terrainObject);

        featureIndex.computeIfAbsent(terrainObject.feature, c -> new Array<>()).add(terrainObject);
    }

    public void remove(MapObject mapObject) {
        Array<MapObject> containingArray = typeIndex.get(mapObject.getClass());
        if (containingArray != null) {
            containingArray.removeValue(mapObject, true);
        }
        gridLookup[mapObject.gridX][mapObject.gridY] = null;
    }

    public void remove(BuildingObject buildingObject) {
        remove((MapObject)buildingObject);

        for (Use use : buildingObject.getUses()) {
            Array<BuildingObject> containingArray = useIndex.get(use);
            if (containingArray != null) {
                containingArray.removeValue(buildingObject, true);
            }
        }
    }

    public void remove(TerrainObject terrainObject) {
        remove((MapObject)terrainObject);

        Array<TerrainObject> containingArray = featureIndex.get(terrainObject.feature);
        if (containingArray != null) {
            containingArray.removeValue(terrainObject, true);
        }
    }

    public Array<BuildingObject> getByUse(Use use) {
        return useIndex.getOrDefault(use, new Array<>());
    }

    public Array<MapObject> getByType(Class<? extends MapObject> type) {
        return typeIndex.get(type);
    }

    public MapObject getByGrid(int gridX, int gridY) {
        return gridLookup[gridX][gridY];
    }

}
