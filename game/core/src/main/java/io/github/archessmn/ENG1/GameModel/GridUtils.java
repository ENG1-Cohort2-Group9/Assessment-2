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
     * Get the screen coordinates of the centre of the grid square the given screen coordinates would snap to.
     * @param coords the grid square in question.
     * @return A {@link Vector2} with coordinates of the centre of a grid square.
     */
    public static Vector2 getGridSquareScreenCoords(GridCoordTuple coords) {
        float gridWidth = ((float)VIEWPORT_WIDTH / GRID_WIDTH);
        float gridHeight = ((float)VIEWPORT_HEIGHT / GRID_HEIGHT);

        float rawGridX = (MathUtils.round(coords.x / gridWidth + 0.5f) * gridWidth) - (gridWidth / 2f);
        float rawGridY = (MathUtils.round(coords.y / (gridHeight) + 0.5f) * gridHeight) - (gridHeight / 2f);

        return new Vector2(rawGridX, rawGridY);
    }

    /**
     * Get the grid square the given coordinates would snap to.
     * For example, the top left grid position would be (0, 8).
     * @param x The centre X coordinate of the object
     * @param y The centre Y coordinate of the object
     * @return A {@link GridCoordTuple} with coordinates in the grid.
     */
    public static GridCoordTuple getGridCoords(float x, float y) {
        float gridWidth = ((float)VIEWPORT_WIDTH / GRID_WIDTH);
        float gridHeight = ((float)VIEWPORT_HEIGHT / GRID_HEIGHT);

        int gridX = Math.round((x / gridWidth) + 0.5f);
        int gridY = Math.round((y / gridHeight) + 0.5f);

        return new GridCoordTuple(gridX, gridY);
    }
}
