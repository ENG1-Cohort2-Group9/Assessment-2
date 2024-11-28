package io.github.archessmn.ENG1.GameModel;

/**
 * Wrapper of {@link Building} that creates a building with the LECTURE_HALL type.
 */
public class LectureHallBuilding extends Building {
   public LectureHallBuilding(float x, float y, boolean built) {
        super(Type.LECTURE_HALL, x, y, 60, 60, 10f, built);
    }
}
