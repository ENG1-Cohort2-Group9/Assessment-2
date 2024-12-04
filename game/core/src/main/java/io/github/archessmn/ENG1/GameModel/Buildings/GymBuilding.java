package io.github.archessmn.ENG1.GameModel.Buildings;

import com.badlogic.gdx.utils.Array;

/**
 * Wrapper of {@link Building} that creates a building with the GYM type.
 */
public class GymBuilding extends Building {

    public GymBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.RECREATION}, "gym.png");
    }
}
