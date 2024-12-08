package io.github.archessmn.ENG1.GameModel.Buildings;

/**
 * The use of a building, adding a new use will require the updateBuildingDistancesScore in World to be updated if
 * certain use pairs with the added type should be weighted differently, as is already done in World.
 */
public enum Use {
    TEACHING, ACCOMMODATION, CAFETERIA, RECREATION
}
