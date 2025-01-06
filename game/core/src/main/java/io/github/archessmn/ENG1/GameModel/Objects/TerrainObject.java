package io.github.archessmn.ENG1.GameModel.Objects;


import io.github.archessmn.ENG1.GameModel.GridCoordTuple;

public class TerrainObject extends MapObject {

    public TerrainObject(float x, float y, Feature type) {
        super(x, y, type.asset, type.name);
        feature = type;
    }

    public TerrainObject(GridCoordTuple gridCoords, Feature type) {
        super(gridCoords, type.asset, type.name);
        feature = type;
    }

    public enum Feature {
        LAKE("lake.jpg", "Water"),
        ROCK("rock.png", "Rock"),
        TREE("tree.png", "Trees");

        public final String asset;
        public final String name;

        private Feature(String asset, String name) {
            this.asset = asset;
            this.name = name;
        }
    }

    public Feature feature;
}
