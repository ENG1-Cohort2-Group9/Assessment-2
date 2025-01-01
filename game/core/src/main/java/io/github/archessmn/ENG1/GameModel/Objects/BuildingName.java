package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * The name of a building, as well as corresponding variables for that building.
 */
public enum BuildingName {
    GYM(10f, "gym.png", new Use[] {Use.RECREATION},
        150, "Gym", 60, 60),
    HALLS(10f, "halls.png", new Use[] {Use.ACCOMMODATION},
        500, "Halls", 60, 60),
    LECTURE_HALL(10f, "lecturehall.png", new Use[] {Use.TEACHING},
        250, "Lecture Theatre", 60, 60),
    PIAZZA(10f, "piazza.png", new Use[] {Use.CAFETERIA, Use.TEACHING},
        250, "Piazza", 60, 60),
    PUB(10f, "pub.png", new Use[] {Use.RECREATION, Use.CAFETERIA},
        100, "Pub", 60, 60);

    private final float buildingConstructionDuration;
    private final String spriteName;
    private final Use[] uses;
    private final int capacity;
    private final String objName;
    private final float width;
    private final float height;

    /**
     *
     * @param buildingConstructionDuration How long construction takes
     * @param spriteName The file name of the buildings' sprite.
     * @param uses The use the building has, used for updating building counters.
     * @param capacity The capacity of the building.
     * @param objName The name of the building.
     * @param width Width of the building.
     * @param height Height of the building.
     */
    BuildingName(float buildingConstructionDuration, String spriteName, Use[] uses, int capacity,
                  String objName, float width, float height) {

        this.buildingConstructionDuration = buildingConstructionDuration;
        this.spriteName = spriteName;
        this.uses = uses;
        this.capacity = capacity;
        this.objName = objName;
        this.width = width;
        this.height = height;
    }

    public String getSpriteName() {
        return spriteName;
    }

    public float getBuildingConstructionDuration() {
        return buildingConstructionDuration;
    }

    public Use[] getUses() {
        return uses;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getObjName() {
        return objName;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }
}
