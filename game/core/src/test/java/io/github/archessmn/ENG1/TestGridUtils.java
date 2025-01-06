package io.github.archessmn.ENG1;

import com.badlogic.gdx.math.Vector2;
import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.GridUtils;
import io.github.archessmn.ENG1.Interface.GameScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGridUtils {

    float gridSquareSize = (float)GameScreen.MAP_WIDTH / GridUtils.GRID_WIDTH;

    @Test
    public void testScreenCoords() {
        assertAll(
            "Ordinary grid co-ordinates incorrect",
            () -> assertEquals(new Vector2(0,0), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(0,0))),
            () -> assertEquals(new Vector2(0,gridSquareSize), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(0,1))),
            () -> assertEquals(new Vector2(gridSquareSize,0), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(1,0))),
            () -> assertEquals(new Vector2(gridSquareSize,gridSquareSize), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(1,1))),
            () -> assertEquals(new Vector2(gridSquareSize * 15f, gridSquareSize * 8f), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(15,8)))
        );

        assertAll(
            "Out of bounds grid co-ordinates incorrect",
            () -> assertEquals(new Vector2(-gridSquareSize,-gridSquareSize), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(-1,-1))),
            () -> assertEquals(new Vector2(gridSquareSize * 20f,gridSquareSize * 20f), GridUtils.getGridSquareScreenCoords(new GridCoordTuple(20,20)))
        );
    }

    @Test
    public void testGridCoords() {
        assertAll(
            "Ordinary screen co-ordinates incorrect",
            () -> assertEquals(new GridCoordTuple(0,0), GridUtils.getGridCoords(gridSquareSize * 0.5f,gridSquareSize * 0.5f)),
            () -> assertEquals(new GridCoordTuple(0,1), GridUtils.getGridCoords(0, gridSquareSize * 1.5f)),
            () -> assertEquals(new GridCoordTuple(1,0), GridUtils.getGridCoords(gridSquareSize  * 1.5f,0)),

            () -> assertEquals(new GridCoordTuple(0,0), GridUtils.getGridCoords(0,0)),
            () -> assertEquals(new GridCoordTuple(1,1), GridUtils.getGridCoords(gridSquareSize, gridSquareSize))
        );

        assertAll(
            "Out of bounds screen co-ordinates incorrect",
            () -> assertEquals(new GridCoordTuple(-1,-1), GridUtils.getGridCoords(-gridSquareSize * 1.5f, -gridSquareSize  * 1.5f)),
            () -> assertEquals(new GridCoordTuple(20, 20), GridUtils.getGridCoords( gridSquareSize * 20.5f, gridSquareSize * 20.5f))
        );
    }
}
