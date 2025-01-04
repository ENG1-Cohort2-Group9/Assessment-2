package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.MapObjectHolder;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingName;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;
import org.junit.jupiter.api.BeforeEach;

public class TestMapObjectHolder {

    private MapObjectHolder mapObjectHolder;

    @BeforeEach
    public void setUp() {
        mapObjectHolder = new MapObjectHolder(16,9);

        MapObject mapObject1 = new BuildingObject(0,0,0, BuildingName.PIAZZA);
        MapObject mapObject2 = new BuildingObject(1,1,0, BuildingName.HALLS);

    }
}
