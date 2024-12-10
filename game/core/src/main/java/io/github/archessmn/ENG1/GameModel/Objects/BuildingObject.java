package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Base class for each building type,
 * stores information about the building and provides utility classes for interacting with it.
 */
public class BuildingObject extends MapObject {
    public boolean built;

    public String unbuiltSpriteName;

    public float initialBuildTime;
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
     * @param built Whether the building should be marked as built upon creation.
     * @param uses The use the building has, used for updating building counters.
     * @param spriteName The file name of the buildings' sprite.
     * @param objName The name of the object in the game space
     */
    public BuildingObject(float x, float y, float width, float height, float buildingConstructionDuration, float initialBuildTime, boolean built, Use[] uses, String spriteName, String objName) {
        super(x, y, width, height, spriteName, objName, true);

        this.initialBuildTime = initialBuildTime;
        this.buildingCompletionTime = initialBuildTime + buildingConstructionDuration;

        this.built = built;
        this.uses = uses;
        this.UseSize = Use.values().length;

        if (!built) {
            this.unbuiltSpriteName = "construction.png";
        }
    }

    /**
     * Makes an un-built copy of the current building type
     * @return A copy of the building.
     */
    public BuildingObject makeCopy(float currentTime) {
        return new BuildingObject(this.x, this.y + 60, this.width, this.height, currentTime, this.buildingCompletionTime - this.initialBuildTime, false, this.uses, this.spriteName, this.objName);
    }

    /**
     * Get the uses for the building
     * @return the array of uses the building has.
     */
    public Use[] getUses() {
        return uses;
    }
}
