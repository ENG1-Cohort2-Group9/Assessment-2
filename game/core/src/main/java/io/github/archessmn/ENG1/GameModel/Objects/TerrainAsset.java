package io.github.archessmn.ENG1.GameModel.Objects;


public class TerrainAsset extends Building {

    public TerrainAsset(float x, float y, float currentTime, boolean built, Feature type ) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.TERRAIN}, type.asset);
        feature = type;
    }

    public enum Feature {
        LAKE("lake.jpg"),
        ROCK("rock.png"),
        TREE("tree.jpg");

        public String asset;

        private Feature(String asset) {
            this.asset = asset;
        }
    }

    public Feature feature;
}
