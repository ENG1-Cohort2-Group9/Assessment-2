package io.github.archessmn.ENG1;

import com.badlogic.gdx.maps.Map;
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
        buildingObject3 = new BuildingObject(new GridCoordTuple(9,8), 0, BuildingName.HALLS);
        buildingObject1.built = true;
        buildingObject2.built = true;
        buildingObject3.built = false;

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
        class NewMapObjectType extends MapObject {
            public NewMapObjectType(GridCoordTuple position) {
                super(position, "none", "none");
            }
        }
        NewMapObjectType newMapObject = new NewMapObjectType(new GridCoordTuple(10,1));

        int mapObjLenBefore = mapObjectHolder.getByType(MapObject.class).size;

        mapObjectHolder.add(newMapObject);

        assertEquals(1, mapObjectHolder.getByType(NewMapObjectType.class).size, "NewMapObjectType list not updated correctly");
        assertEquals(mapObjLenBefore + 1, mapObjectHolder.getByType(MapObject.class).size, "MapObject list not updated correctly");
    }

    @Test
    public void testAddBuildingObject() {
        BuildingObject buildingObject = new BuildingObject(new GridCoordTuple(6,6), 0, BuildingName.PUB);

        mapObjectHolder.add(buildingObject);

        assertAll(
                "Building missing from list(s)",
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
                "Terrain missing from list(s)",
                () -> assertTrue(mapObjectHolder.getTerrainObjects().contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(TerrainObject.class).contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getByFeature(TerrainObject.Feature.LAKE).contains(terrainObject1, true))
        );

        assertAll(
                "Duplicate entry(s) added",
                () -> assertEquals(1, mapObjectHolder.getTerrainObjects().size),
                () -> assertEquals(1, mapObjectHolder.getAll().size),
                () -> assertEquals(1, mapObjectHolder.getByFeature(TerrainObject.Feature.LAKE).size),
                () -> assertEquals(1, mapObjectHolder.getByType(TerrainObject.class).size),
                () -> assertEquals(1, mapObjectHolder.getByType(MapObject.class).size)
        );

        assertEquals(terrainObject1, mapObjectHolder.getByGrid(1,1), "Terrain not found when searching by grid");
    }

    @Test
    public void testRemoveNotFound() {
        assertThrows(IllegalArgumentException.class, () -> mapObjectHolder.remove(buildingObject1), "Incorrect exception or no exception thrown when removed item is not found");
    }

    @Test
    public  void testRemoveBuildingObject() {

        mapObjectHolder.add(buildingObject1);
        mapObjectHolder.remove(buildingObject1);

        assertAll(
                "Building not removed from list(s)",
                () -> assertFalse(mapObjectHolder.getBuildings().contains(buildingObject1, true)),
                () -> assertFalse(mapObjectHolder.getAll().contains(buildingObject1, true)),
                () -> assertFalse(mapObjectHolder.getByType(BuildingName.PUB).contains(buildingObject1, true)),
                () -> assertFalse(mapObjectHolder.getByType(BuildingObject.class).contains(buildingObject1, true)),
                () -> assertFalse(mapObjectHolder.getByType(MapObject.class).contains(buildingObject1, true)),
                () -> assertFalse(mapObjectHolder.getByUse(Use.TEACHING).contains(buildingObject1, true)),
                () -> assertFalse(mapObjectHolder.getByUse(Use.CAFETERIA).contains(buildingObject1, true))
        );

        assertEquals(0, mapObjectHolder.getUseCount(Use.TEACHING), "Incorrect use count (TEACHING)");
        assertEquals(0, mapObjectHolder.getUseCount(Use.CAFETERIA), "Incorrect use count (CAFETERIA)");

        assertNotEquals(buildingObject1, mapObjectHolder.getByGrid(6,6), "Building not removed from grid");
    }

    @Test
    public void testRemoveTerrainObject() {

        mapObjectHolder.add(terrainObject1);
        mapObjectHolder.remove(terrainObject1);

        assertAll(
                "Terrain not removed from list",
                () -> assertFalse(mapObjectHolder.getTerrainObjects().contains(terrainObject1, true)),
                () -> assertFalse(mapObjectHolder.getAll().contains(terrainObject1, true)),
                () -> assertFalse(mapObjectHolder.getByType(TerrainObject.class).contains(terrainObject1, true)),
                () -> assertFalse(mapObjectHolder.getByType(MapObject.class).contains(terrainObject1, true)),
                () -> assertFalse(mapObjectHolder.getByFeature(TerrainObject.Feature.LAKE).contains(terrainObject1, true))
        );

        assertNotEquals(buildingObject1, mapObjectHolder.getByGrid(6,6), "Terrain not removed from grid");
    }

    @Test
    public void testGetByUseEmpty() {
        assertEquals(0, mapObjectHolder.getByUse(Use.TEACHING).size);
    }

    @Test
    public void testGetBuildingByAll() {
        mapObjectHolder.add(buildingObject1);
        mapObjectHolder.add(buildingObject2);
        mapObjectHolder.add(buildingObject3);

        assertAll(
                "1st building (Piazza (0,0)) missing from list(s)",
                () -> assertTrue(mapObjectHolder.getBuildings().contains(buildingObject1, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(buildingObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(BuildingName.PIAZZA).contains(buildingObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(BuildingObject.class).contains(buildingObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(buildingObject1, true)),
                () -> assertTrue(mapObjectHolder.getByUse(Use.TEACHING).contains(buildingObject1, true)),
                () -> assertTrue(mapObjectHolder.getByUse(Use.CAFETERIA).contains(buildingObject1, true)),
                () -> assertEquals(buildingObject1, mapObjectHolder.getByGrid(0,0))
        );

        assertAll(
                "2nd building (Gym (5,4)) missing from list(s)",
                () -> assertTrue(mapObjectHolder.getBuildings().contains(buildingObject2, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(buildingObject2, true)),
                () -> assertTrue(mapObjectHolder.getByType(BuildingName.GYM).contains(buildingObject2, true)),
                () -> assertTrue(mapObjectHolder.getByType(BuildingObject.class).contains(buildingObject2, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(buildingObject2, true)),
                () -> assertTrue(mapObjectHolder.getByUse(Use.RECREATION).contains(buildingObject2, true)),
                () -> assertEquals(buildingObject2, mapObjectHolder.getByGrid(5,4))
        );

        assertAll(
                "3rd building (unbuilt Halls (9,8)) missing from list(s)",
                () -> assertTrue(mapObjectHolder.getBuildings().contains(buildingObject3, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(buildingObject3, true)),
                () -> assertTrue(mapObjectHolder.getByType(BuildingName.HALLS).contains(buildingObject3, true)),
                () -> assertTrue(mapObjectHolder.getByType(BuildingObject.class).contains(buildingObject3, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(buildingObject3, true)),
                () -> assertTrue(mapObjectHolder.getByUse(Use.ACCOMMODATION).contains(buildingObject3, true)),
                () -> assertEquals(buildingObject3, mapObjectHolder.getByGrid(9,8))
        );
    }

    @Test
    public void testGetTerrainByAll() {
        mapObjectHolder.add(terrainObject1);
        mapObjectHolder.add(terrainObject2);
        mapObjectHolder.add(terrainObject3);

        assertAll(
                "1st terrain (Lake, (1,1)) missing from list(s)",
                () -> assertTrue(mapObjectHolder.getTerrainObjects().contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(TerrainObject.class).contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(terrainObject1, true)),
                () -> assertTrue(mapObjectHolder.getByFeature(TerrainObject.Feature.LAKE).contains(terrainObject1, true)),
                () -> assertEquals(terrainObject1, mapObjectHolder.getByGrid(1,1))
        );

        assertAll(
                "2nd terrain (Rock, (5,2)) missing from list(s)",
                () -> assertTrue(mapObjectHolder.getTerrainObjects().contains(terrainObject2, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(terrainObject2, true)),
                () -> assertTrue(mapObjectHolder.getByType(TerrainObject.class).contains(terrainObject2, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(terrainObject2, true)),
                () -> assertTrue(mapObjectHolder.getByFeature(TerrainObject.Feature.ROCK).contains(terrainObject2, true)),
                () -> assertEquals(terrainObject2, mapObjectHolder.getByGrid(5,2))
        );

        assertAll(
                "3rd terrain (Tree, (3,8)) missing from list(s)",
                () -> assertTrue(mapObjectHolder.getTerrainObjects().contains(terrainObject3, true)),
                () -> assertTrue(mapObjectHolder.getAll().contains(terrainObject3, true)),
                () -> assertTrue(mapObjectHolder.getByType(TerrainObject.class).contains(terrainObject3, true)),
                () -> assertTrue(mapObjectHolder.getByType(MapObject.class).contains(terrainObject3, true)),
                () -> assertTrue(mapObjectHolder.getByFeature(TerrainObject.Feature.TREE).contains(terrainObject3, true)),
                () -> assertEquals(terrainObject3, mapObjectHolder.getByGrid(3,8))
        );
    }

    @Test
    public void testSpaceIsOccupied() {
        mapObjectHolder.add(buildingObject1);
        mapObjectHolder.add(terrainObject2);
        mapObjectHolder.add(buildingObject3);

        assertTrue(mapObjectHolder.spaceIsOccupied(0,0), "Space (0,0) should be occupied");
        assertTrue(mapObjectHolder.spaceIsOccupied(5,2), "Space (5,2) should be occupied");
        assertTrue(mapObjectHolder.spaceIsOccupied(9,8), "Space (9,8) should be occupied");
    }
}
