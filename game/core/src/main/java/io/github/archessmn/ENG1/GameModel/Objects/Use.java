package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * The use of a building, adding a new use will require the updateBuildingDistancesScore in World to be updated if
 * certain use pairs with the added type should be weighted differently, as is already done in World.
 */
public enum Use {
    TEACHING("Teaching"),
    ACCOMMODATION("Accommodation"),
    CAFETERIA("Cafeteria"),
    RECREATION("Recreation");

    private final String stringName;

    Use(String stringName) {
        this.stringName = stringName;
    }

    public String getStringName() {
        return stringName;
    }
}
