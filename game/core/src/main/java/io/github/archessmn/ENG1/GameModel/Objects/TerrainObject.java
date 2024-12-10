package io.github.archessmn.ENG1.GameModel.Objects;


public class TerrainObject extends MapObject {

    public TerrainObject(float x, float y, Feature type) {
        super(x, y, 60, 60, type.asset, type.name, false);
        feature = type;
    }

    public enum Feature {
        LAKE("lake.jpg", "Water"),
        ROCK("rock.png", "Rock"),
        TREE("tree.png", "Trees");

        public String asset;
        public String name;

        private Feature(String asset, String name) {
            this.asset = asset;
            this.name = name;
        }
    }

    public Feature feature;
}
