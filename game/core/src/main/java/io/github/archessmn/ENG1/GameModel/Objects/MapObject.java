package io.github.archessmn.ENG1.GameModel.Objects;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.GridUtils;

/**
 * A super-class representing anything that can be placed on a map.
 * It stored information about the object and provides utility classes for interacting with it.
 */
public class MapObject {
    public float x;
    public float y;

    public int gridX;
    public int gridY;

    public float width;
    public float height;

    public final String spriteName;
    public final String objName;

    public boolean placed = false;
    public boolean isBuilding = false;

    public Rectangle bounds;

    private float efficiency = 0.5f;


    /**
     * Initialises a new map object.
     * @param x The X coordinate to place the object at.
     * @param y The Y coordinate to place the object at.
     * @param width Width of the object.
     * @param height Height of the object.
     * @param spriteName The file name of the object's sprite.
     * @param objName The name of the object in the game space
     * @param isBuilding Indicates whether the map object is a building or not
     */
    public MapObject(float x, float y, float width, float height, String spriteName, String objName, boolean isBuilding) {
        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.spriteName = spriteName;
        this.objName = objName;

        this.isBuilding = isBuilding;

        this.bounds = new Rectangle(this.x, this.y, this.width, this.height);
    }

    /**
     * Called when placing the object into the world, will snap the object to
     * the grid and update its grid coordinates to match its position.
     */
    public void place() {
        this.snapToGrid();
        GridCoordTuple gridCoord = GridUtils.getGridCoords(this.x, this.y);
        this.gridX = gridCoord.x;
        this.gridY = gridCoord.y;

        this.placed = true;
    }

    /**
     * Used to update the position of the object in the world.
     * @param x The X position to use
     * @param y The Y position to use
     */
    public void setCenter(float x, float y) {
        this.x = x - this.width / 2;
        this.y = y - this.height / 2;
    }

    /**
     * Sets the X position of the object
     * @param x The X position to use
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Sets the Y position of the object
     * @param y The Y position to use
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Calculates and returns the bounding box of the object.
     * @return The bounding box {@link Rectangle} of the object.
     */
    public Rectangle getBounds() {
        return this.bounds.set(this.x, this.y, this.width, this.height);
    }

    /**
     * Get the raw coordinates of the grid square the object would
     * snap to, relative to the entire viewport.
     * For example the bottom left grid position would be (0.0, 480).
     * @return A {@link Vector2} of the position on the grid
     */
    public Vector2 getRawGridCoords() {
        return GridUtils.getRawGridCoords(this.x + this.width / 2, this.y + this.height / 2);
    }

    /**
     * Get the raw coordinates of the grid square the object would
     * snap to, relative to the grid.
     * For example, the top left grid position would be (0, 8).
     * @return A {@link GridCoordTuple} of the grid position
     */
    public GridCoordTuple getGridCoords() {
        return GridUtils.getGridCoords(this.x + this.width / 2, this.y + this.height / 2);
    }

    /**
     * Snaps the object to the grid.
     */
    public void snapToGrid() {
        Vector2 gridCoords = getRawGridCoords();
        this.setCenter(gridCoords.x, gridCoords.y);
    }

    /**
     * Determines if an object is in any of the nine squares (including the centre) adjacent to this square
     * @param other the other object to compare
     * @return true if the object is adjacent to this one
     */
    public boolean isAdjacentTo(MapObject other) {
        return Math.abs(other.gridX - gridX) <= 1 && Math.abs(other.gridY - gridY) <= 1;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(float efficiency) {
        this.efficiency = efficiency;
    }
}
