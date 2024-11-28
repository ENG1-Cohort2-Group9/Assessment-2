package io.github.archessmn.ENG1.GameModel;


/**
 * Wrapper of {@link Building} that creates a building with the OFFICES type.
 */
public class OfficeBuilding extends Building {
    public OfficeBuilding(float x, float y, boolean built) {
        super(Type.OFFICES, x, y, 60, 60, 10f, built);
    }
}
