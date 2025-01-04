package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.MapObjectHolder;
import io.github.archessmn.ENG1.GameModel.Objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMapObjectHolder {

    private MapObjectHolder mapObjectHolder;

    BuildingObject buildingObject1;
    BuildingObject buildingObject2;
    BuildingObject buildingObject3;

    TerrainObject terrainObject1;
    TerrainObject terrainObject2;
    TerrainObject terrainObject3;

    @BeforeEach
    public void setUp() {
        mapObjectHolder = new MapObjectHolder(16,9);

        buildingObject1 = new BuildingObject(new GridCoordTuple(0,0), 0, BuildingName.PIAZZA);
        buildingObject2 = new BuildingObject(new GridCoordTuple(5,4), 0, BuildingName.GYM);
        buildingObject3 = new BuildingObject(new GridCoordTuple(15,8), 0, BuildingName.HALLS);

        terrainObject1 = new TerrainObject(new GridCoordTuple(1,1), TerrainObject.Feature.LAKE);
        terrainObject2 = new TerrainObject(new GridCoordTuple(5,2), TerrainObject.Feature.ROCK);
        terrainObject3 = new TerrainObject(new GridCoordTuple(3,8), TerrainObject.Feature.TREE);
    }

    @Test
    public void testAddOccupied() {
        mapObjectHolder.add(buildingObject2);
        TerrainObject overlap = new TerrainObject(new GridCoordTuple(5,4), TerrainObject.Feature.LAKE);

        assertThrows(IllegalArgumentException.class, () -> mapObjectHolder.add(overlap), "Incorrect exception or no exception thrown for overlapping objects");
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

        mapObjectHolder.add(newMapObject);

        assertEquals(1, mapObjectHolder.getByType(NewMapObjectType.class).size, "NewMapObjectType list not updated correctly");
        assertEquals(mapObjLenBefore + 1, mapObjectHolder.getByType(NewMapObjectType.class).size, "MapObject list not updated correctly");
    }

    @Test
    public void testAddBuildingObject() {
        BuildingObject buildingObject = new BuildingObject(new GridCoordTuple(6,6), 0, BuildingName.PUB);

        mapObjectHolder.add(buildingObject);

        assertAll(
            "Building missing from list",
            () -> assertTrue(mapObjectHolder.getBuildings().contains(buildingObject, true)),
            () -> assertTrue(mapObjectHolder.getAll().contains(buildingObject, true)),
            () -> assertTrue(mapObjectHolder.getByType(BuildingName.PUB).contains(buildingObject, true)),
            () -> assertTrue(mapObjectHolder.getByType(BuildingObject.class).contains(buildingObject, true)),
            () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(buildingObject, true)),
            () -> assertTrue(mapObjectHolder.getByUse(Use.RECREATION).contains(buildingObject, true)),
            () -> assertTrue(mapObjectHolder.getByUse(Use.CAFETERIA).contains(buildingObject, true))
        );

        assertAll(
            "Duplicate entry(s) added",
            () -> assertEquals(1, mapObjectHolder.getBuildings().size),
            () -> assertEquals(1, mapObjectHolder.getAll().size),
            () -> assertEquals(1, mapObjectHolder.getByType(BuildingName.PUB).size),
            () -> assertEquals(1, mapObjectHolder.getByType(BuildingObject.class).size),
            () -> assertEquals(1, mapObjectHolder.getByType(MapObject.class).size),
            () -> assertEquals(1, mapObjectHolder.getByUse(Use.RECREATION).size),
            () -> assertEquals(1, mapObjectHolder.getByUse(Use.CAFETERIA).size)
        );

        buildingObject.built = true;

        assertEquals(1, mapObjectHolder.getUseCount(Use.RECREATION), "Incorrect use count (RECREATION)");
        assertEquals(1, mapObjectHolder.getUseCount(Use.CAFETERIA), "Incorrect use count (CAFETERIA)");

        assertEquals(buildingObject, mapObjectHolder.getByGrid(6,6), "Building not found when searching by grid");
    }

    @Test
    public void testAddTerrainObject() {
        mapObjectHolder.add(terrainObject1);

        assertAll(
            "Terrain missing from list",
            () -> assertTrue(mapObjectHolder.getTerrainObjects().contains(terrainObject1, true)),
            () -> assertTrue(mapObjectHolder.getAll().contains(terrainObject1, true)),
            () -> assertTrue(mapObjectHolder.getByType(TerrainObject.class).contains(terrainObject1, true)),
            () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(terrainObject1, true)),
            () -> assertTrue(mapObjectHolder.getByFeature(TerrainObject.Feature.LAKE).contains(terrainObject1, true))
        );

        assertAll(
            "Duplicate entry(s) added",
            () -> assertEquals(1, mapObjectHolder.getBuildings().size),
            () -> assertEquals(1, mapObjectHolder.getAll().size),
            () -> assertEquals(1, mapObjectHolder.getByType(BuildingName.PUB).size),
            () -> assertEquals(1, mapObjectHolder.getByType(BuildingObject.class).size),
            () -> assertEquals(1, mapObjectHolder.getByType(MapObject.class).size),
            () -> assertEquals(1, mapObjectHolder.getByUse(Use.RECREATION).size),
            () -> assertEquals(1, mapObjectHolder.getByUse(Use.CAFETERIA).size)
        );

        assertEquals(terrainObject1, mapObjectHolder.getByGrid(1,1), "Building not found when searching by grid");
    }
}
