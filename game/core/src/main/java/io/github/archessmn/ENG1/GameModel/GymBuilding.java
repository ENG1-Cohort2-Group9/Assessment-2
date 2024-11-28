package io.github.archessmn.ENG1.GameModel;

/**
 * Wrapper of {@link Building} that creates a building with the GYM type.
 */
public class GymBuilding extends Building {
    public GymBuilding(float x, float y, boolean built) {
        super(Type.GYM, x, y, 60, 60, 10f, built);
    }
}
