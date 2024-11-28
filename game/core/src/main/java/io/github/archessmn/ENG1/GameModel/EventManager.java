package io.github.archessmn.ENG1.GameModel;

import java.util.ArrayList;

public class EventManager {
    ArrayList<GameEvent> eventList = new ArrayList<GameEvent>();
    GameEventListener listener;

    /**
     * Assigns the GameEventListener
     * @param listener The listener that can process the event
     */
    public EventManager(GameEventListener listener) {
        this.listener = listener;
    }

    /**
     * Run this inside the main process loop. Raises events at random intervals determined by
     * the rarity of said event.
     * @param delta time in seconds since the last frame
     */
    public void processEvents(float delta) {
        if (false) { // Calculate whether to raise an event
            listener.raiseEvent(eventList.get(0));
        }
    }
}
