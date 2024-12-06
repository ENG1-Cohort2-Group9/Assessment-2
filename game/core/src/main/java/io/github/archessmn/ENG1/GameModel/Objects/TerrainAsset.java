package io.github.archessmn.ENG1.GameModel.Objects;

/**
 * Wrapper of {@link Building} that creates a building with the GYM type.
 */
public class TerrainAsset extends Building {

    public TerrainAsset(float x, float y, float currentTime, boolean built, Feature type ) {
        super(x, y, 60, 60, 10f, currentTime, built, new Use[] {Use.TERRAIN}, featureToAsset(type));
    }

    public enum Feature {
        LAKE, ROCK, TREE
    }

    private static String featureToAsset(Feature type) {
        switch (type) {
            case LAKE:
                return "lake.jpg";
            case ROCK:
                return "rock.png";
            case TREE:
                return "tree.jpg";
            default:
                return null;
        }
    }
}
