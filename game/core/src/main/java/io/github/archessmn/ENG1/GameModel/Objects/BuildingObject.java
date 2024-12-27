package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Base class for each building type,
 * stores information about the building and provides utility classes for interacting with it.
 */
public class BuildingObject extends MapObject {
    public boolean built;

    public String unbuiltSpriteName;

    public float initialBuildTime;
    public float constructionDuration;
    public float buildingCompletionTime;

    public final Use[] uses;
    int UseSize;


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
    public BuildingObject(float x, float y, float width, float height, float buildingConstructionDuration, float initialBuildTime, boolean built, Use[] uses, String spriteName, String objName) {
        super(x, y, width, height, spriteName, objName, true);

        this.initialBuildTime = initialBuildTime;
        this.constructionDuration = buildingConstructionDuration;
        this.buildingCompletionTime = initialBuildTime + buildingConstructionDuration;

        this.built = built;
        this.uses = uses;
        this.UseSize = Use.values().length;

        this.unbuiltSpriteName = "construction.png";
    }

    /**
     * Creates a copy of the BuildingObject
     * @return The BuildingObject clone
     */
    public BuildingObject makeCopy() {
        return new BuildingObject(this.screenPosition.x, this.screenPosition.y + 60, this.width, this.height, this.constructionDuration, this.initialBuildTime, true, this.uses, this.spriteName, this.objName);
    }

    /**
     * Sets the building back to un-built and begins construction again
     * @param newInitialConstructionTime The time at which the building will begin construction
     */
    public void resetBuildingConstruction(float newInitialConstructionTime) {
        this.built = false;
        this.initialBuildTime = newInitialConstructionTime;
        this.buildingCompletionTime = initialBuildTime + constructionDuration;
    }

    /**
     * Get the uses for the building
     * @return the array of uses the building has.
     */
    public Use[] getUses() {
        return uses;
    }
}
