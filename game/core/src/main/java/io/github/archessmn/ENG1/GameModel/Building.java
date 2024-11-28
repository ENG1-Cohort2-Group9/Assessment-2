package io.github.archessmn.ENG1.GameModel;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Base class for each building type ({@link Building.Type}),
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
    public float timeUntilBuilt;

    private boolean placed = false;


    /**
     * Defines the type and, by inference, the {@link Use}
     * and sprite of a building.
     */
    public enum Type {
        GYM, HALLS, LECTURE_HALL, OFFICES, PIAZZA
    }

    /**
     * The use of a building, inferred from its {@link Type}
     */
    public enum Use {
        SLEEP, LEARN, EAT, RECREATION
    }

    public Type buildingType;

    public Rectangle bounds;



    /**
     * Initialises a new building.
     * @param buildingType The {@link Type} of the building.
     * @param x The X coordinate to place the building at.
     * @param y The Y coordinate to place the building at.
     * @param width Width of the building.
     * @param height Height of the building.
     * @param timeUntilBuilt Time until the building is marked as built.
     * @param built Whether the building should be marked as built upon creation.
     */
    public Building(Type buildingType, float x, float y, float width, float height, float timeUntilBuilt, boolean built) {

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.initialBuildTime = timeUntilBuilt;
        this.timeUntilBuilt = timeUntilBuilt;

        this.built = built;

        if (timeUntilBuilt > 0) {
            this.unbuiltSpriteName = "construction.png";
        }

        spriteName = switch (buildingType) {
            case GYM -> "gym.png";
            case HALLS -> "halls.png";
            case LECTURE_HALL -> "lecturehall.png";
            case OFFICES -> "offices.png";
            case PIAZZA -> "piazza.png";
        };

        this.buildingType = buildingType;

        this.bounds = new Rectangle(this.x, this.y, this.width, this.height);
    }

    /**
     * Usually called once per frame to run any updates the building may need,
     * such as reducing time until built.
     * @param deltaTime The amount of time to advance by.
     */
    public void tick(float deltaTime) {
        if (placed && timeUntilBuilt >= 0)
            this.timeUntilBuilt -= deltaTime;
        if (timeUntilBuilt <= 0)
            built = true;
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
    public Building makeCopy() {
        return new Building(this.buildingType, this.x, this.y + 60, this.width, this.height, this.initialBuildTime, false);
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

    /**
     * Get the use of the building, inferred from its {@link Type}.
     * @return The {@link Use} of the building.
     */
    public Use getBuildingUse() {
        return switch (buildingType) {
            case GYM -> Use.RECREATION;
            case HALLS -> Use.SLEEP;
            case LECTURE_HALL, OFFICES -> Use.LEARN;
            case PIAZZA -> Use.EAT;
        };
    }
}
