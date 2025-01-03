package io.github.archessmn.ENG1.GameModel.Objects;

import com.badlogic.gdx.math.Vector2;
import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.GridUtils;

/**
 * A super-class representing anything that can be placed on a map.
 * It stored information about the object and provides utility classes for interacting with it.
 */
public abstract class MapObject implements Cloneable {
    private Vector2 screenPosition; // The position on screen of the bottom left corner of the sprite

    private GridCoordTuple gridCoords;

    public final float width;
    public final float height;

    public final String spriteName;
    public final String objName;

    public boolean placed = false;

    private float efficiency = 0.5f;


    /**
     * Initialises a new map object.
     * @param x The X coordinate to place the object at.
     * @param y The Y coordinate to place the object at.
     * @param width Width of the object.
     * @param height Height of the object.
     * @param spriteName The file name of the object's sprite.
     * @param objName The name of the object in the game space
     */
    public MapObject(float x, float y, float width, float height, String spriteName, String objName) {
        this.screenPosition = new Vector2(x, y);

        this.width = width;
        this.height = height;

        updateGridCoords();

        this.spriteName = spriteName;
        this.objName = objName;
    }

    /**
     * Called when placing the object into the world, will snap the object to
     * the grid and update its grid coordinates to match its position.
     */
    public void place() {
        this.updateGridCoords();

        this.placed = true;
    }

    /**
     * Sets the centre of the object to the position given by the co-ordinates. For example, if the object is 1 wide and high,
     * and it's top right is given as (1,1), this method will set its position to (0.5, 0.5)
     * @param x The rightmost (positive) x position
     * @param y The topmost (positive) y position
     */
    public void setCentre(float x, float y) {
        this.screenPosition.x = x - this.width / 2;
        this.screenPosition.y = y - this.height / 2;
    }


    /**
     * @return True if the given position is within the bounds of this mapObject
     */
    public boolean contains(Vector2 screenPosition) {
        return screenPosition.x >= this.screenPosition.x && screenPosition.x <= this.screenPosition.x + width
            && screenPosition.y >= this.screenPosition.y && screenPosition.y <= this.screenPosition.y + height;
    }

    /**
     * Sets the X position of the object
     * @param x The X position to use
     */
    public void setX(float x) {
        this.screenPosition.x = x;
    }

    /**
     * Sets the Y position of the object
     * @param y The Y position to use
     */
    public void setY(float y) {
        this.screenPosition.y = y;
    }

    /**
     * Refreshes this object's gridCoords with the correct grid square it should be in
     */
    public void updateGridCoords() {
        gridCoords = GridUtils.getGridCoords(this.screenPosition.x + width / 2, this.screenPosition.y + height / 2);
        System.out.println();
    }

    /**
     * Get the raw coordinates of the grid square the object would
     * snap to, relative to the grid.
     * For example, the top left grid position would be (0, 8).
     * @return A {@link GridCoordTuple} of the grid position
     */
    public GridCoordTuple getGridCoords() {
        return this.gridCoords;
    }

    /**
     * Get this object's position on screen, unrelated to its grid position
     */
    public Vector2 getUnsnappedScreenPos() {
        return screenPosition;
    }

    /**
     * Get this objects position on screen, snapped to the grid. Updates the grid coordinates of this object
     */
    public Vector2 getSnappedScreenPosition() {
        updateGridCoords();
        return GridUtils.getGridSquareScreenCoords(gridCoords);
    }

    public float getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(float efficiency) {
        this.efficiency = efficiency;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Object clone = super.clone();
        // Make a deep copy
        ((MapObject)clone).screenPosition = new Vector2(screenPosition.x, screenPosition.y);
        ((MapObject)clone).gridCoords = new GridCoordTuple(gridCoords.x, gridCoords.y);

        return clone;
    }
}
