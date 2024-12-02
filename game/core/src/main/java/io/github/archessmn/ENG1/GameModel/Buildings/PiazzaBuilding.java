package io.github.archessmn.ENG1.GameModel.Buildings;

/**
 * Wrapper of {@link Building} that creates a building with the PIAZZA type.
 */
public class PiazzaBuilding extends Building {
    public PiazzaBuilding(float x, float y, boolean built) {
        super(x, y, 60, 60, 10f, built,  new Use[] {Use.CAFETERIA, Use.TEACHING}, "piazza.png");
    }
}
