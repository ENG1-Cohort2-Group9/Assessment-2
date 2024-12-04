package io.github.archessmn.ENG1.GameModel.Buildings;

import com.badlogic.gdx.utils.Array;

/**
 * Wrapper of {@link Building} that creates a building with the LECTURE_HALL type.
 */
public class LectureHallBuilding extends Building {
   public LectureHallBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built,  new Use[] {Use.TEACHING}, "lecturehall.png");
    }
}
