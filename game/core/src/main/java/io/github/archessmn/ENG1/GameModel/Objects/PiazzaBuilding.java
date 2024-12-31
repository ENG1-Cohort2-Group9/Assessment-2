package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Wrapper of {@link BuildingObject} that creates a building with the PIAZZA type.
 */
public class PiazzaBuilding extends BuildingObject {
    public PiazzaBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built,
            new Use[] {Use.CAFETERIA, Use.TEACHING}, "piazza.png", "Piazza Building", 250);
    }
}
