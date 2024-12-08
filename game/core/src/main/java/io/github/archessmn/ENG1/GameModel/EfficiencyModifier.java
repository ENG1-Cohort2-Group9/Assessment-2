package io.github.archessmn.ENG1.GameModel;

import io.github.archessmn.ENG1.GameModel.Objects.Building;

public record EfficiencyModifier(float endTime, float multiplier, Building affectedBuilding) {
    public EfficiencyModifier(float endTime, float multiplier, Building affectedBuilding) {
        this.endTime = endTime;
        this.multiplier = multiplier;
        this.affectedBuilding = affectedBuilding;
    }
}
