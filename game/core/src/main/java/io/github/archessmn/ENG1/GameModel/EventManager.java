package io.github.archessmn.ENG1.GameModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class EventManager {
    boolean[] eventsEnabled = new boolean[GameEvent.values().length]; // Whether each event can happen (assuming it has not already)
    boolean[] eventsOccurred = new boolean[GameEvent.values().length]; // True if an event has happened
    GameEventListener[] listeners;

    private Random random = new Random();
    private float maxRandomVal = 0f;

    private final int numberOfEventsThisGame = random.nextInt(3, GameEvent.values().length);
    private final float eventTimeVariation = 0.5f; // A value of 1 means that one event can happen immediately after another, 0 means events happen at regular intervals

    private float averageEventInterval;
    private float nextEventTime;
    private int eventsRaised = 0;

    /**
     * Assigns the GameEventListener
     * @param listeners The listeners that will react to events
     */
    public EventManager(GameEventListener[] listeners, float gameLengthSeconds) {
        this.listeners = listeners;

        // All events are initially enabled
        for (int i = 0; i < GameEvent.values().length; i++) {
            eventsEnabled[i] = true;
        }
        for (GameEvent event : GameEvent.values()) {
            maxRandomVal += event.chance;
        }

        averageEventInterval = gameLengthSeconds / (float)numberOfEventsThisGame;
        nextEventTime = getNextEventTime(averageEventInterval, eventTimeVariation, eventsRaised);
        System.out.println("a maximum of " + numberOfEventsThisGame + " events will occur this game");
    }

    /**
     * Gets the next instant at which an event will occur. Events are evenly distributed throughout the game
     * with a configurable amount of jitter.
     * @param averageEventInterval The average time between events
     * @param eventTimeVariation A value of 1 means that one event can happen immediately after another, 0 means events
     *                           happen at regular intervals
     * @param eventsRaised How many events have occurred this game
     * @return the time in seconds at which an event will occur
     */
    private float getNextEventTime(float averageEventInterval, float eventTimeVariation, int eventsRaised) {
        return (averageEventInterval * (eventsRaised + 0.5f)) + (random.nextFloat(-eventTimeVariation / 2f, eventTimeVariation / 2f) * averageEventInterval);
    }

    /**
     * Run this inside the main process loop. Raises events at jittered random intervals
     * @param currentTime world time in seconds
     */
    public void processEvents(float currentTime) {
        if (currentTime > nextEventTime && getPossibleEventsCount() > 0) { // Calculate whether to raise an event
            eventsRaised++;
            nextEventTime = getNextEventTime(averageEventInterval, eventTimeVariation, eventsRaised);

            // Select a random event based on its chance
            float randomValue = random.nextFloat(0, maxRandomVal);;
            GameEvent event = getValidEventFromFloat(randomValue);
            eventsOccurred[event.ordinal()] = true;
            maxRandomVal -= event.chance;

            for (GameEventListener listener : listeners) {
                listener.raiseEvent(event);
            }
        }
    }

    /**
     * @return The number of events that can occur when called
     */
    public int getPossibleEventsCount() {
        int count = 0;
        for (int i = 0; i < GameEvent.values().length; i++) {
            if (eventsEnabled[i] && !eventsOccurred[i]) {
                count++;
            }
        }

        return count;
    }

    /**
     * Given a float where 0 represents the first valid event, return a possible event at that index. The last valid event
     * is given by the sum of all possible event chances when the function is called.
     */
    public GameEvent getValidEventFromFloat(float value) {
        float distanceToIndex = value;
        int position = 0;
        while (position < GameEvent.values().length && distanceToIndex >= 0) {
            if (eventsEnabled[position] && !eventsOccurred[position]) {
                distanceToIndex -= GameEvent.values()[position].chance;
            }
            position++;
        }
        // Ensure index is in bounds
        if (position - 1 == GameEvent.values().length)
            throw new RuntimeException("No available events to raise");
        return GameEvent.values()[position - 1];
    }

    public void disableEvent(GameEvent event) {
        if (eventsEnabled[event.ordinal()]) {
            eventsEnabled[event.ordinal()] = false;
            if (!eventsOccurred[event.ordinal()]) {
                maxRandomVal -= event.chance;
            }
        }
    }

    public void enableEvent(GameEvent event) {
        if (!eventsEnabled[event.ordinal()]) {
            eventsEnabled[event.ordinal()] = true;
            if (!eventsOccurred[event.ordinal()]) {
                maxRandomVal += event.chance;
            }
        }
    }

    public boolean isEventEnabled(GameEvent event) {
        return eventsEnabled[event.ordinal()];
    }
}
