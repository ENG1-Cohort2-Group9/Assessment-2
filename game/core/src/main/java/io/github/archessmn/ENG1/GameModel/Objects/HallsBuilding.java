package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Wrapper of {@link Building} that creates a building with the HALLS type.
 */
public class HallsBuilding extends Building {
    public HallsBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.ACCOMMODATION}, "halls.png", "Halls");
    }
}
