package io.github.archessmn.ENG1.GameModel.Objects;

import io.github.archessmn.ENG1.GameModel.World;

/**
 * Base class for each building type,
 * stores information about the building and provides utility classes for interacting with it.
 */
public class BuildingObject extends MapObject {
    public boolean built;

    public final String unbuiltSpriteName;

    private float initialBuildTime;
    private final float constructionDuration;
    private float buildingCompletionTime;

    public final Use[] uses;

    // The number of students a building has capacity for, this is used to incentivise the player to place different
    // amounts of each building.
    public int[] capacity;

    public BuildingName type;


    /**
     * Initialises a new building.
     * @param x The X coordinate to place the building at.
     * @param y The Y coordinate to place the building at.
     * @param initialBuildTime The time at which the building will start construction
     * @param type The enum type of the building, storing other associated values for the building.
     */
    public BuildingObject(float x, float y, float initialBuildTime, BuildingName type) {
        super(x, y, type.getWidth(), type.getHeight(), type.getSpriteName(), type.getObjName());

        this.initialBuildTime = initialBuildTime;
        this.constructionDuration = type.getBuildingConstructionDuration();
        this.buildingCompletionTime = initialBuildTime + type.getBuildingConstructionDuration();

        this.built = false;
        this.uses = type.getUses();
        this.capacity = type.getCapacity();
        this.type = type;

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
     * Get the capacity of a building for a specific use.
     * @return the integer value capacity.
     */
    public int getUseCapacity(Use use) {
        return capacity[use.ordinal()];
    }

    public BuildingName getType() {
        return type;
    }

    public boolean isBuilt() {
        return built;
    }

    public float getConstructionPercent(World world) {

        return 100 - 100 * (buildingCompletionTime - world.getCurrentTime())/ constructionDuration;
    }


}
