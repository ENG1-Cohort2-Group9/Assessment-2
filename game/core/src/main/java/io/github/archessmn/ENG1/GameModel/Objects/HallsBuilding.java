package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Wrapper of {@link BuildingObject} that creates a building with the HALLS type.
 */
public class HallsBuilding extends BuildingObject {
    public HallsBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.ACCOMMODATION},
            "halls.png", "Halls", 500);
    }
}
