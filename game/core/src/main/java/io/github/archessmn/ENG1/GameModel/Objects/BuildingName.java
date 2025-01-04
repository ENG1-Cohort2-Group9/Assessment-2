package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * The name of a building, as well as corresponding variables for that building.
 */
public enum BuildingName {
    GYM(1f, "gym.png", new Use[] {Use.RECREATION},
        new int[] {150}, "Gym", 60, 60),
    HALLS(1f, "halls.png", new Use[] {Use.ACCOMMODATION},
        new int[] {500}, "Halls", 60, 60),
    LECTURE_HALL(1f, "lecturehall.png", new Use[] {Use.TEACHING},
        new int[] {250}, "Lecture Theatre", 60, 60),
    PIAZZA(1f, "piazza.png", new Use[] {Use.TEACHING, Use.CAFETERIA},
        new int[] {125, 50}, "Piazza", 60, 60),
    PUB(1f, "pub.png", new Use[] {Use.CAFETERIA, Use.RECREATION},
        new int[] {100, 150}, "Pub", 60, 60);

    private final float buildingConstructionDuration;
    private final String spriteName;
    private final Use[] uses;
    private final int[] capacity = new int[Use.values().length];
    private final String objName;
    private final float width;
    private final float height;

    /**
     *
     * @param buildingConstructionDuration How long construction takes
     * @param spriteName The file name of the buildings' sprite.
     * @param uses The use the building has, used for updating building counters.
     * @param capacity The capacity of each of the buildings uses.
     * @param objName The name of the building.
     * @param width Width of the building.
     * @param height Height of the building.
     */
    BuildingName(float buildingConstructionDuration, String spriteName, Use[] uses, int[] capacity,
                  String objName, float width, float height) {

        this.buildingConstructionDuration = buildingConstructionDuration;
        this.spriteName = spriteName;
        this.uses = uses;
        this.objName = objName;
        this.width = width;
        this.height = height;

        // This works by letting the uses and capacity arrays be passed with any order of uses, where the first values
        // with the same index in those 2 arrays are corresponding. They are then added to this.capacity using the Use
        // ordinal values for indexing.
        for (int i=0; i < capacity.length; i++) {
            this.capacity[uses[i].ordinal()] = capacity[i];
        }
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

    public int[] getCapacity() {
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
