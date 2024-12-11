package io.github.archessmn.ENG1.GameModel;

import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;

public record EfficiencyModifier(float endTime, float multiplier, BuildingObject affectedBuilding) {
    public EfficiencyModifier(float endTime, float multiplier, BuildingObject affectedBuilding) {
        this.endTime = endTime;
        this.multiplier = multiplier;
        this.affectedBuilding = affectedBuilding;
    }
}
