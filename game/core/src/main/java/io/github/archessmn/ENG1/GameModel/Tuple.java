package io.github.archessmn.ENG1.GameModel;

public class Tuple<T, S> {
    public final T x;
    public final S y;
    public Tuple(T x, S y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Tuple [x=" + x.toString() + ", y=" + y.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tuple tuple) {
            return x.equals(tuple.x) && y.equals(tuple.y);
        } else {
            return false;
        }
    }
}
