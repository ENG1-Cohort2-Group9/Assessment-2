package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.GameEvent;
import io.github.archessmn.ENG1.GameModel.GameEventHandler;
import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingName;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import io.github.archessmn.ENG1.GameModel.Objects.TerrainObject;
import io.github.archessmn.ENG1.GameModel.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.github.archessmn.ENG1.GameModel.GridUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestWorld {
    World world;

    BuildingObject buildingObject = new BuildingObject(new GridCoordTuple(2,2), 0, BuildingName.PIAZZA);

    @BeforeEach
    public void setUp() {
        world = new World(100,100, new GameEventHandler[] {}, null);
    }

    @Test
    public void testTerrainFeatures() {
        assertTrue(world.getTerrainObjects().size > 0, "Map must contain at least one terrain object.");
    }

    @Test
    public void testAddBuildingAllowed() {
        GridCoordTuple emptySpace = getEmptySpace();

        BuildingObject building = new BuildingObject(emptySpace, 0, BuildingName.PIAZZA);
        building.built = true;
        assertTrue(world.addMapObject(building), "Building placed on empty space failed");
        assertFalse(building.built, "Building should be set to not built once placed");

        assertEquals(building, world.getMapObjectAt(emptySpace), "Building was not found at the correct position");
    }

    @Test
    public void testAddBuildingOccupied() {
        boolean tested = false;
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (world.getMapObjectAt(new GridCoordTuple(x, y)) != null) {
                    BuildingObject building = new BuildingObject(new GridCoordTuple(x, y), 0, BuildingName.PIAZZA);
                    assertFalse(world.addMapObject(building), "Building placed on occupied space did not fail");

                    assertNotEquals(building, world.getMapObjectAt(new GridCoordTuple(x, y)), "Building was placed on the map");

                    tested = true;
                    break;
                }
            }
        }
        assertTrue(tested, "Map must contain at least one terrain object");
    }

    @Test
    public void testAddBuildingOutsideBounds() {
        assertFalse(world.addMapObject(new BuildingObject(new GridCoordTuple(1, 10), 0, BuildingName.PIAZZA)), "Building placed outside bounds did not fail");
    }

    @Test
    public void testAddTerrainObject() {
        GridCoordTuple emptySpace = getEmptySpace();

        TerrainObject terrain = new TerrainObject(emptySpace, TerrainObject.Feature.TREE);
        assertTrue(world.addMapObject(terrain), "Terrain placed on empty space failed");

        assertEquals(terrain, world.getMapObjectAt(emptySpace), "Terrain was not found at the correct position");
    }

    @Test
    public void testUnknownMapObject() {
        class NewMapObject extends MapObject {
            public NewMapObject(GridCoordTuple gridCoordTuple) {
                super(gridCoordTuple, "none", "none");
            }
        }

        assertThrows(IllegalArgumentException.class, () -> world.addMapObject(new NewMapObject(getEmptySpace())), "Unknown map object should raise an exception");
    }

    @Test
    public void testProcessRemovesActiveEvents() {
        world.addActiveEvent(GameEvent.FLOODING, 30);
        assertTrue(world.hasActiveEvent(GameEvent.FLOODING), "Active event not added");
        world.process(31f);
        assertFalse(world.hasActiveEvent(GameEvent.FLOODING), "Active event not removed");
    }

    @Test
    public void testDoesOverlap() {
        world.addMapObject(buildingObject);

        assertTrue(world.doesObjectOverlap(new BuildingObject(new GridCoordTuple(2,2), 0, BuildingName.PIAZZA)), "Building did not overlap");
    }

    @Test
    public void testDoesNotOverlap() {
        BuildingObject building = new BuildingObject(getEmptySpace(), 0, BuildingName.PIAZZA);

        assertFalse(world.doesObjectOverlap(building), "Building was overlapped on empty space");
    }

    @Test
    public void testOverlapOutOfBounds() {
        BuildingObject building = new BuildingObject(new GridCoordTuple(-2,2), 0, BuildingName.PIAZZA);

        assertThrows(IndexOutOfBoundsException.class, () -> world.doesObjectOverlap(building), "Out of bounds check did not raise an error");
    }

    @Test
    public void testGameEnded() {
        world.process(World.GAME_LENGTH_SECONDS + 1);

        assertTrue(world.getGameEnded(), "Game should have ended");
    }

    @Test
    public void testGameNotEnded() {
        world.process(10);

        assertFalse(world.getGameEnded(), "Game should have not ended");
    }

    @Test
    public void testCloseBuildingForever() {
        GridCoordTuple space = getEmptySpace();
        BuildingObject building = new BuildingObject(space, 0, BuildingName.PIAZZA);
        world.addMapObject(building);

        world.process(BuildingName.PIAZZA.getBuildingConstructionDuration() + 1);
        assertTrue(building.built, "Building was not opened");

        world.closeBuilding(building);
        assertFalse(building.built, "Building was not closed");

        world.process(World.GAME_LENGTH_SECONDS - world.getCurrentTime());

        assertFalse(building.built, "Building reopened near the end of the game");
    }

    @Test
    public void testCloseBuildingForOneSec() {
        GridCoordTuple space = getEmptySpace();
        BuildingObject building = new BuildingObject(space, 0, BuildingName.PIAZZA);
        world.addMapObject(building);

        world.process(BuildingName.PIAZZA.getBuildingConstructionDuration() + 1);
        assertTrue(building.built, "Building was not opened");

        world.closeBuilding(building, 1);
        assertFalse(building.built, "Building was not closed");

        world.process(1.1f);

        assertTrue(building.built, "Building did not reopen");
    }

    @Test
    public void testCloseBuildingAlreadyClosed() {
        GridCoordTuple space = getEmptySpace();
        BuildingObject building = new BuildingObject(space, 0, BuildingName.PIAZZA);
        world.addMapObject(building);
        world.closeBuilding(building, BuildingName.PIAZZA.getBuildingConstructionDuration() / 2f);

        world.process((BuildingName.PIAZZA.getBuildingConstructionDuration() / 2f) + 0.1f);

        assertFalse(building.built, "Building was opened too early");

        world.process(BuildingName.PIAZZA.getBuildingConstructionDuration());

        assertTrue(building.built, "Building was not opened in time");
    }

    @Test
    public void getBuildingsNearTerrain() {
        TerrainObject terrain = world.getTerrainObjects().get(0);

        GridCoordTuple space = getSpaceInBoundsNextTo(terrain.getGridCoords());
        if (world.getMapObjectAt(space) != null)
            world.destroyMapObject(world.getMapObjectAt(space));
        BuildingObject building = new BuildingObject(space, 0, BuildingName.PIAZZA);

        world.addMapObject(building);
        building.built = true;

        assertEquals(building, world.getBuildingsNearTerrain(terrain.feature).get(0), "Building not found near the terrain");
    }

    @Test
    public void testMapObjectsAroundCentre() {
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                MapObject mapObject = world.getMapObjectAt(new GridCoordTuple(x, y));
                if (mapObject != null) {
                    world.destroyMapObject(mapObject);
                }
                if (x == 2 && y == 2) {
                    world.addMapObject(buildingObject);
                }
            }
        }
        assertEquals(0, world.getMapObjectsAroundPosition(new GridCoordTuple(2,2)).size, "No map objects should be found");

        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                if (x != 2 || y != 2) {
                    TerrainObject terrain = new TerrainObject(new GridCoordTuple(x, y), TerrainObject.Feature.TREE);
                    world.addMapObject(terrain);
                }
            }
        }

        assertEquals(8, world.getMapObjectsAroundPosition(new GridCoordTuple(2,2)).size, "8 map objects should be found");
    }

    @Test
    public void testMapObjectsAroundBounds() {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                MapObject mapObject = world.getMapObjectAt(new GridCoordTuple(x, y));
                if (mapObject != null) {
                    world.destroyMapObject(mapObject);
                }
            }
        }
        world.addMapObject(new BuildingObject(new GridCoordTuple(0,0), 0, BuildingName.PIAZZA));

        assertEquals(0, world.getMapObjectsAroundPosition(new GridCoordTuple(0,0)).size, "No map objects should be found");

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    TerrainObject terrain = new TerrainObject(new GridCoordTuple(x, y), TerrainObject.Feature.TREE);
                    world.addMapObject(terrain);
                }
            }
        }

        assertEquals(3, world.getMapObjectsAroundPosition(new GridCoordTuple(0,0)).size, "3 map objects should be found");


        for (int x = 3; x <= 4; x++) {
            for (int y = 3; y <= 4; y++) {
                MapObject mapObject = world.getMapObjectAt(new GridCoordTuple(x, y));
                if (mapObject != null) {
                    world.destroyMapObject(mapObject);
                }
            }
        }
        world.addMapObject(new BuildingObject(new GridCoordTuple(4,4), 0, BuildingName.PIAZZA));
        assertEquals(0, world.getMapObjectsAroundPosition(new GridCoordTuple(4,4)).size, "No map objects should be found");

        for (int x = 3; x <= 4; x++) {
            for (int y = 3; y <= 4; y++) {
                if (x != 4 || y != 4) {
                    TerrainObject terrain = new TerrainObject(new GridCoordTuple(x, y), TerrainObject.Feature.TREE);
                    world.addMapObject(terrain);
                }
            }
        }

        assertEquals(3, world.getMapObjectsAroundPosition(new GridCoordTuple(4,4)).size, "3 map objects should be found");
    }

    @Test
    public void testTerrainNearBuildingsZero() {
        assertEquals(0, world.getCountOfTerrainNearBuildings(TerrainObject.Feature.LAKE));
    }

    @Test
    public void testTerrainNearBuildings() {
        GridCoordTuple space = getEmptySpace();
        world.addMapObject(new BuildingObject(space, 0, BuildingName.PIAZZA));

        int totalCount = 0;
        for (TerrainObject.Feature feature : TerrainObject.Feature.values()) {
            totalCount += world.getCountOfTerrainNearBuildings(feature);
        }

        assertEquals(world.getMapObjectsAroundPosition(space).size,  totalCount, "Terrain objects near buildings did not match terrain objects around placed building");
    }

    private GridCoordTuple getEmptySpace() {
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (world.getMapObjectAt(new GridCoordTuple(x, y)) == null) {
                    return new GridCoordTuple(x, y);
                }
            }
        }
        fail("Random error, map was generated fully occupied, please run tests again");
        return null;
    }

    private GridCoordTuple getSpaceInBoundsNextTo(GridCoordTuple centre) {
        for (int x = Math.max(centre.x - 1, 0); x <= Math.min(centre.x + 1, 4); x++) {
            for (int y = Math.max(centre.y - 1, 0); y <= Math.min(centre.y + 1, 4); y++) {
                if (x != centre.x && y != centre.y) {
                    return new GridCoordTuple(x, y);
                }
            }
        }
        return null;
    }
}
