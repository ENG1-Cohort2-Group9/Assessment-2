package io.github.archessmn.ENG1.GameModel;

/**
 * Utility class for storing the grid coordinates of a building.
 */
public class GridCoordTuple {
    public int x;
    public int y;

    public GridCoordTuple(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GridCoordTuple other) {
            return x == other.x && y == other.y;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "GridCoordTuple [x=" + x + ", y=" + y + "]";
    }
}
