package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Wrapper of {@link BuildingObject} that creates a building with the GYM type.
 */
public class GymBuilding extends BuildingObject {

    public GymBuilding(float x, float y, float currentTime, boolean built) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.RECREATION}, "gym.png", "Gym");
    }

    @Override
    public GymBuilding makeCopy(float currentTime) {
        return new GymBuilding(this.x, this.y + 60,currentTime, false);
    }
}
