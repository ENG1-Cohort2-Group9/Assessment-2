package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import static io.github.archessmn.ENG1.Interface.GameScreen.VIEWPORT_HEIGHT;
import static io.github.archessmn.ENG1.Interface.GameScreen.VIEWPORT_WIDTH;

/**
 * Utilities to assist with usage of the world grid.
 */
public class GridUtils {
    public final static int GRID_WIDTH = 16;
    public final static int GRID_HEIGHT = 9;

    /**
     * Get the screen coordinates of the bottom left of the grid square the given screen coordinates would snap to.
     * @param coords the grid square in question.
     * @return A {@link Vector2} with coordinates of the bottom left of a grid square on the screen.
     */
    public static Vector2 getGridSquareScreenCoords(GridCoordTuple coords) {
        float viewportX = (float)VIEWPORT_WIDTH * coords.x / GRID_WIDTH;
        float viewportY = (float)VIEWPORT_HEIGHT * coords.y / GRID_HEIGHT;
        return new Vector2(viewportX, viewportY);
    }

    /**
     * Get the grid square containing the given coordinates. The top left grid position is (0, 8).
     * @param x The centre X coordinate of the object
     * @param y The centre Y coordinate of the object
     * @return A {@link GridCoordTuple} with coordinates in the grid.
     */
    public static GridCoordTuple getGridCoords(float x, float y) {

        int gridX = (int)((x / VIEWPORT_WIDTH) * GRID_WIDTH);
        int gridY = (int)((y / VIEWPORT_HEIGHT) * GRID_HEIGHT);

        return new GridCoordTuple(gridX, gridY);
    }
}
