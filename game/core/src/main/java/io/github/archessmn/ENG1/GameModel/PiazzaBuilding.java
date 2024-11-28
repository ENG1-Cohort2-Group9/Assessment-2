package io.github.archessmn.ENG1.GameModel;

/**
 * Wrapper of {@link Building} that creates a building with the PIAZZA type.
 */
public class PiazzaBuilding extends Building {
    public PiazzaBuilding(float x, float y, boolean built) {
        super(Type.PIAZZA, x, y, 60, 60, 10f, built);
    }
}
