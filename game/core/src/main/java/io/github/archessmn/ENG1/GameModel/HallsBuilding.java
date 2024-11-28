package io.github.archessmn.ENG1.GameModel;

/**
 * Wrapper of {@link Building} that creates a building with the HALLS type.
 */
public class HallsBuilding extends Building {
    public HallsBuilding(float x, float y, boolean built) {
        super(Type.HALLS, x, y, 60, 60, 10f, built);
    }
}
