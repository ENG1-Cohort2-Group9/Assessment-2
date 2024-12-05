package io.github.archessmn.ENG1.GameModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EventManager {
    ArrayList<GameEvent> occurredEventList = new ArrayList<GameEvent>();
    ArrayList<GameEvent> possibleEvents;
    GameEventListener listener;

    private Random random = new Random();
    private float maxRandomVal = 0f;
    private float anyEventChancePerMin = 1f; // Probability of any event occurring this minute
    private final float numberOfEventsThisGame = random.nextFloat(3, GameEvent.values().length);

    /**
     * Assigns the GameEventListener
     * @param listener The listener that can process the event
     */
    public EventManager(GameEventListener listener) {
        this.listener = listener;
        // Possible events will be pruned when events occur to prevent duplicate events
        possibleEvents = new ArrayList<GameEvent>(Arrays.asList(GameEvent.values()));
        for (GameEvent event : possibleEvents) {
            maxRandomVal += event.chancePerMin;
            anyEventChancePerMin *= (1 - event.chancePerMin);
        }
        anyEventChancePerMin = 1 - anyEventChancePerMin;
    }

    /**
     * Run this inside the main process loop. Raises events at jittered random intervals
     * @param currentTime world time in seconds since the last frame
     */
    public void processEvents(float currentTime) {


        if (random.nextFloat() < anyEventChancePerMin * framePercentOfMinute) { // Calculate whether to raise an event
            float randomValue = random.nextFloat(0, maxRandomVal);
            int eventIndex = (int)((randomValue / maxRandomVal) * possibleEvents.size());
            GameEvent event = possibleEvents.get(eventIndex);
            possibleEvents.remove(eventIndex);
            occurredEventList.add(event);
            maxRandomVal -= event.chancePerMin;
            anyEventChancePerMin = 1 - ((1 - anyEventChancePerMin) / (1 - event.chancePerMin));
            listener.raiseEvent(event);
        }
    }
}
