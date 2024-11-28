package io.github.archessmn.ENG1.GameModel;

public class GameEventListener {
    GameEventHandler handler;

    public GameEventListener(GameEventHandler handler) {
        this.handler = handler;
    }

    public void raiseEvent(GameEvent event) {
        handler.handle(event);
    }
}
