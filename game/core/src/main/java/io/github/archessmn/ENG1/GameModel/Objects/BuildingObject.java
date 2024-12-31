package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Base class for each building type,
 * stores information about the building and provides utility classes for interacting with it.
 */
public class BuildingObject extends MapObject {
    public boolean built;

    public final String unbuiltSpriteName;

    private float initialBuildTime;
    private float constructionDuration;
    private float buildingCompletionTime;

    public final Use[] uses;

    // The number of students a building has capacity for, this is used to incentivise the player to place different
    // amounts of each building.
    public int capacity;


    /**
     * Initialises a new building.
     * @param x The X coordinate to place the building at.
     * @param y The Y coordinate to place the building at.
     * @param width Width of the building.
     * @param height Height of the building.
     * @param buildingConstructionDuration How long construction takes
     * @param initialBuildTime The time at which the building will start construction
     * @param built Whether the building should be marked as built upon creation.
     * @param uses The use the building has, used for updating building counters.
     * @param spriteName The file name of the buildings' sprite.
     * @param objName The name of the object in the game space
     */
    public BuildingObject(float x, float y, float width, float height, float buildingConstructionDuration,
                          float initialBuildTime, boolean built, Use[] uses, String spriteName, String objName,
                          int capacity) {
        super(x, y, width, height, spriteName, objName);

        this.initialBuildTime = initialBuildTime;
        this.constructionDuration = buildingConstructionDuration;
        this.buildingCompletionTime = initialBuildTime + buildingConstructionDuration;

        this.built = built;
        this.uses = uses;
        this.capacity = capacity;

        this.unbuiltSpriteName = "construction.png";
    }

    /**
     * Sets the building back to un-built and begins construction again
     * @param newInitialConstructionTime The time at which the building will begin construction
     */
    public void resetConstruction(float newInitialConstructionTime) {
        this.built = false;
        this.initialBuildTime = newInitialConstructionTime;
        this.buildingCompletionTime = initialBuildTime + constructionDuration;
    }

    /**
     * Sets the building back to un-built and begins construction again
     * @param newInitialConstructionTime The time at which the building will begin construction
     * @param newConstructionTime The time the building will take to finish construction
     */
    public void resetConstruction(float newInitialConstructionTime, float newConstructionTime) {
        this.built = false;
        this.initialBuildTime = newInitialConstructionTime;
        this.buildingCompletionTime = initialBuildTime + newConstructionTime;
    }

    /**
     * @return True if the building has finished construction
     * @param currentTime The current game time in seconds
     */
    public boolean isComplete(float currentTime) {
        return currentTime >= buildingCompletionTime;
    }

    /**
     * Get the uses for the building
     * @return the array of uses the building has.
     */
    public Use[] getUses() {
        return uses;
    }

    /**
     * Get the capacity of a building
     * @return the integer value capacity.
     */
    public int getCapacity() {
        return capacity;
    }
}
