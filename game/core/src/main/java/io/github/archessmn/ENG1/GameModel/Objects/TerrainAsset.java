package io.github.archessmn.ENG1.GameModel.Objects;


public class TerrainAsset extends Building {

    public TerrainAsset(float x, float y, float currentTime, boolean built, Feature type ) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.TERRAIN}, type.asset, type.name);
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
