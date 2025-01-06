package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * The use of a building, adding a new use will require the updateBuildingDistancesScore in World to be updated if
 * certain use pairs with the added type should be weighted differently, as is already done in World.
 */
public enum Use {
    TEACHING("Teaching", "Teaching"),
    ACCOMMODATION("Accommodation", "Accomm."),
    CAFETERIA("Cafeteria", "Cafeteria"),
    RECREATION("Recreation", "Recreation");

    private final String stringName;
    private final String stringShortName;

    Use(String stringName, String stringShortName) {
        this.stringName = stringName;
        this.stringShortName = stringShortName;
    }

    public String getStringName() {
        return stringName;
    }
    public String getStringShortName() { return stringShortName; }
}
