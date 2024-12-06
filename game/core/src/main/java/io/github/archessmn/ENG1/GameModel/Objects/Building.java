package io.github.archessmn.ENG1.GameModel.Objects;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.archessmn.ENG1.GameModel.GridCoordTuple;
import io.github.archessmn.ENG1.GameModel.GridUtils;

/**
 * Base class for each building type,
 * stores information about the building and provides utility classes for interacting with it.
 */
public class Building {
    public float x;
    public float y;

    public int gridX;
    public int gridY;

    public float width;
    public float height;

    public boolean built;

    public final String spriteName;
    public String unbuiltSpriteName;

    public float initialBuildTime;
    public float buildingCompletionTime;

    public boolean placed = false;

    public Rectangle bounds;

    public final Use[] uses;

    /**
     * The use of a building
     */
    public enum Use {
        TEACHING, ACCOMMODATION, CAFETERIA, RECREATION, TERRAIN
    }


    /**
     * Initialises a new building.
     * @param x The X coordinate to place the building at.
     * @param y The Y coordinate to place the building at.
     * @param width Width of the building.
     * @param height Height of the building.
     * @param buildingConstructionDuration How long construction takes
     * @param built Whether the building should be marked as built upon creation.
     * @param uses The use the building has, used for updating building counters.
     * @param spriteName The file name of the buildings' sprite.
     */
    public Building(float x, float y, float width, float height, float buildingConstructionDuration, float initialBuildTime, boolean built, Use[] uses, String spriteName) {

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.initialBuildTime = initialBuildTime;
        this.buildingCompletionTime = initialBuildTime + buildingConstructionDuration;

        this.built = built;
        this.uses = uses;
        this.spriteName = spriteName;

        if (!built) {
            this.unbuiltSpriteName = "construction.png";
        }

        this.bounds = new Rectangle(this.x, this.y, this.width, this.height);
    }

    /**
     * Called when placing the building into the world, will snap the building to
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
     * Used to update the position of the building in the world.
     * @param x The X position to use
     * @param y The Y position to use
     */
    public void setCenter(float x, float y) {
        this.x = x - this.width / 2;
        this.y = y - this.height / 2;
    }

    /**
     * Sets the X position of the building
     * @param x The X position to use
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Sets the Y position of the building
     * @param y The Y position to use
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Calculates and returns the bounding box of the building.
     * @return The bounding box {@link Rectangle} of the building.
     */
    public Rectangle getBounds() {
        return this.bounds.set(this.x, this.y, this.width, this.height);
    }

    /**
     * Makes an un-built copy of the current building type
     * @return A copy of the building.
     */
    public Building makeCopy(float currentTime) {
        return new Building(this.x, this.y + 60, this.width, this.height, currentTime, this.buildingCompletionTime - this.initialBuildTime, false, this.uses, this.spriteName);
    }

    /**
     * Get the uses for the building
     * @return the array of uses the building has.
     */
    public Use[] getUses() {
        return uses;
    }

    /**
     * Get the raw coordinates of the grid square the building would
     * snap to, relative to the entire viewport.
     * For example the bottom left grid position would be (0.0, 480).
     * @return A {@link Vector2} of the position on the grid
     */
    public Vector2 getRawGridCoords() {
        return GridUtils.getRawGridCoords(this.x + this.width / 2, this.y + this.height / 2);
    }

    /**
     * Get the raw coordinates of the grid square the building would
     * snap to, relative to the grid.
     * For example, the top left grid position would be (0, 8).
     * @return A {@link GridCoordTuple} of the grid position
     */
    public GridCoordTuple getGridCoords() {
        return GridUtils.getGridCoords(this.x + this.width / 2, this.y + this.height / 2);
    }

    /**
     * Snaps the building to the grid.
     */
    public void snapToGrid() {
        Vector2 gridCoords = getRawGridCoords();
        this.setCenter(gridCoords.x, gridCoords.y);
    }
}
