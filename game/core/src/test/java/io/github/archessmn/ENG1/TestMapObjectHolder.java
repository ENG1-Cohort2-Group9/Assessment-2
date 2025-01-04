package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.MapObjectHolder;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingName;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestMapObjectHolder {

    private MapObjectHolder mapObjectHolder;

    @BeforeEach
    public void setUp() {
        mapObjectHolder = new MapObjectHolder(16,9);

        BuildingObject buildingObject1 = new BuildingObject(new GridCoordTuple(0,0), 0, BuildingName.PIAZZA);
        BuildingObject buildingObject2 = new BuildingObject(new GridCoordTuple(5,4), 0, BuildingName.GYM);
        BuildingObject buildingObject3 = new BuildingObject(new GridCoordTuple(15,8), 0, BuildingName.HALLS);

        TerrainObject terrainObject1 = new TerrainObject(new GridCoordTuple(1,1), TerrainObject.Feature.LAKE);
        TerrainObject terrainObject2 = new TerrainObject(new GridCoordTuple(5,2), TerrainObject.Feature.ROCK);
        TerrainObject terrainObject3 = new TerrainObject(new GridCoordTuple(3,8), TerrainObject.Feature.TREE);

        mapObjectHolder.add(buildingObject1);
        mapObjectHolder.add(buildingObject2);
        mapObjectHolder.add(buildingObject3);

        mapObjectHolder.add(terrainObject1);
        mapObjectHolder.add(terrainObject2);
        mapObjectHolder.add(terrainObject3);
    }

    @Test
    public void testAddOccupied() {
        TerrainObject overlap = new TerrainObject(new GridCoordTuple(5,4), TerrainObject.Feature.LAKE);

        assertThrows(IllegalArgumentException.class, () -> mapObjectHolder.add(overlap));
    }

    @Test
    public void testAddNewClass() {
        class NewMapObjectType extends TerrainObject {
            public NewMapObjectType(GridCoordTuple position) {
                super(position, Feature.TREE);
            }
        }
        NewMapObjectType newMapObject = new NewMapObjectType(new GridCoordTuple(10,1));

        int mapObjLenBefore = mapObjectHolder.getByType(MapObject.class).size;
        int terObjLenBefore = mapObjectHolder.getByType(TerrainObject.class).size;

        mapObjectHolder.add(newMapObject);

        assertEquals(1, mapObjectHolder.getByType(NewMapObjectType.class));
        assertEquals(mapObjLenBefore + 1, mapObjectHolder.getByType(NewMapObjectType.class));
        assertEquals(terObjLenBefore + 1, mapObjectHolder.getByType(NewMapObjectType.class));
    }

    @Test
    public void testAddBuildingObject() {

    }
}
