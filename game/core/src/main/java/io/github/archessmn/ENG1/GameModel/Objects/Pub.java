package io.github.archessmn.ENG1.GameModel.Objects;


/**
 * Wrapper of {@link Building} that creates a building with the Pub type.
 */
public class Pub extends Building {
    public Pub(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.RECREATION, Use.CAFETERIA}, "offices.png");
    }
}
