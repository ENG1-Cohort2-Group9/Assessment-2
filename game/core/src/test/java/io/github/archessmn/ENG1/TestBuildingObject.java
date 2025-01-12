package io.github.archessmn.ENG1;

import com.badlogic.gdx.math.Vector2;
import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.GridUtils;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingName;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.Use;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestBuildingObject {
    BuildingObject buildingObject;

    @BeforeEach
    public void setUp() {
        buildingObject = new BuildingObject(180, 100, 0, BuildingName.PIAZZA);
    }

    @Test
    public void testResetConstruction() {
        buildingObject.built = true;

        buildingObject.resetConstruction(10);

        assertFalse(buildingObject.built, "Building should not be built");
        assertFalse(buildingObject.isComplete(10), "Building should not be complete now");
        assertTrue(buildingObject.isComplete(10 + BuildingName.PIAZZA.getBuildingConstructionDuration() + 0.1f), "Building should be complete now");
    }

    @Test
    public void testResetConstructionTime() {
        buildingObject.built = true;

        buildingObject.resetConstruction(10, 5);

        assertFalse(buildingObject.built, "Building should not be built");
        assertFalse(buildingObject.isComplete(12), "Building should not be complete now");
        assertTrue(buildingObject.isComplete(16), "Building should be complete now");
    }

    @Test
    public void testConstructionPercent() {
        buildingObject.resetConstruction(0, 64);
        assertEquals(25, buildingObject.getConstructionPercent(16), "Incorrect construction percent");
    }

    @Test
    public void testConstructionTime() {
        buildingObject.resetConstruction(0, 64);
        assertEquals(48, buildingObject.getRemainingConstructionTime(16), "Incorrect remaining construction time");
    }

    @Test
    public void testHasUse() {
        assertTrue(buildingObject.hasUse(Use.TEACHING), "Teaching use not found");
        assertTrue(buildingObject.hasUse(Use.CAFETERIA), "Cafeteria use not found");
        assertFalse(buildingObject.hasUse(Use.RECREATION), "Recreation use incorrectly found");
    }

    @Test
    public void testPlace() {
        assertFalse(buildingObject.placed, "Building should not be placed yet");
        GridCoordTuple priorPos = GridUtils.getGridCoords(buildingObject.getUnsnappedScreenPos().x + buildingObject.width / 2f, buildingObject.getUnsnappedScreenPos().y + buildingObject.height / 2f);
        buildingObject.place();
        assertTrue(buildingObject.placed, "Building should be placed");
        assertEquals(priorPos, buildingObject.getGridCoords(), "Building position should not change");
    }

    @Test
    public void testDemolition() {
        buildingObject.beginDemolition(0, 10);
        assertFalse(buildingObject.isDemolished(0), "Building should not be demolished yet");
        assertTrue(buildingObject.isDemolished(11), "Building should be demolished now");
    }

    @Test
    public void testContains() {
        assertTrue(buildingObject.contains(new Vector2(181, 101)), "Building should contain the co-ordinates (181, 101)");
        assertFalse(buildingObject.contains(new Vector2(0, 0)), "Building should not contain the co-ordinates (0, 0)");
        assertFalse(buildingObject.contains(new Vector2(-181, -101)), "Building should not contain the co-ordinates (-181, -101)");
    }

    @Test
    public void testSnappedPos() {
        assertEquals(GridUtils.getGridSquareScreenCoords(buildingObject.getGridCoords()), buildingObject.getSnappedScreenPosition());
    }

    @Test
    public void testClone() {
        try {
            Object clone = buildingObject.clone();
            assertInstanceOf(BuildingObject.class, clone);
            if (clone instanceof BuildingObject buildingClone) {
                assertNotSame(buildingClone, buildingObject);

                assertAll(
                    "Deep copy check failed",
                    () -> assertNotSame(buildingClone.getUnsnappedScreenPos(), buildingObject.getUnsnappedScreenPos()),
                    () -> assertEquals(buildingClone.getGridCoords(), buildingObject.getGridCoords()),
                    () -> assertNotSame(buildingClone.getGridCoords(), buildingObject.getGridCoords()),
                    () -> assertEquals(buildingClone.getGridCoords(), buildingObject.getGridCoords())
                );
            }
        } catch (CloneNotSupportedException e) {
            fail("Clone not supported");
        }
    }

    static class AchievementTest {

        @Test
        void checkCondition() {
        }
    }
}
