package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Wrapper of {@link BuildingObject} that creates a building with the LECTURE_HALL type.
 */
public class LectureHallBuilding extends BuildingObject {
   public LectureHallBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built,  new Use[] {Use.TEACHING}, "lecturehall.png", "Lecture Theatre");
    }
}
