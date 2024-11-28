package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import static io.github.archessmn.ENG1.Interface.Main.VIEWPORT_HEIGHT;
import static io.github.archessmn.ENG1.Interface.Main.VIEWPORT_WIDTH;

/**
 * Utilities to assist with usage of the world grid.
 */
public class GridUtils {
    /**
     * Get the raw coordinates of the grid square the given coordinates would snap to.
     * @param x The center X coordinate of the object
     * @param y The center Y coordinate of the object
     * @return A {@link Vector2} with coordinates of a grid square.
     */
    public static Vector2 getRawGridCoords(float x, float y) {
        float rawGridX = calcRawGridXCoord(x);
        float rawGridY = calcRawGridYCoord(y);
        return new Vector2(rawGridX, rawGridY);
    }

    /**
     * Get the normalised coordinates of the grid square the given coordinates
     * would snap to, relative to the grid.
     * For example, the top left grid position would be (0, 8).
     * @param x The center X coordinate of the object
     * @param y The center Y coordinate of the object
     * @return A {@link GridCoordTuple} with coordinates in the grid.
     */
    public static GridCoordTuple getGridCoords(float x, float y) {
        float gridWidth = (VIEWPORT_WIDTH / 16f);
        float gridHeight = (VIEWPORT_HEIGHT / 9f);

        int gridX = Math.round((x / gridWidth) + 0.5f);
        int gridY = Math.round((y / gridHeight) + 0.5f);

        return new GridCoordTuple(gridX, gridY);
    }

    /**
     * Used to calculate the closes grid square in the X direction
     * @param coord The X coordinate of the object
     * @return X coordinate of the closest grid square
     */
    public static float calcRawGridXCoord(float coord) {
        float gridWidth = (VIEWPORT_WIDTH / 16f);
        return (MathUtils.round(coord / gridWidth + 0.5f) * gridWidth) - (gridWidth / 2f);
    }

    /**
     * Used to calculate the closes grid square in the Y direction
     * @param coord The Y coordinate of the object
     * @return Y coordinate of the closest grid square
     */
    public static float calcRawGridYCoord(float coord) {
        float gridHeight = (VIEWPORT_HEIGHT / 9f);
        return (MathUtils.round(coord / (gridHeight) + 0.5f) * gridHeight) - (gridHeight / 2f);
    }
}
