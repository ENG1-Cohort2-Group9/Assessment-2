package io.github.archessmn.ENG1.GameModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class EventManager {
    ArrayList<GameEvent> occurredEventList = new ArrayList<GameEvent>();
    ArrayList<GameEvent> possibleEvents;
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
        // Possible events will be pruned when events occur to prevent duplicate events
        possibleEvents = new ArrayList<GameEvent>(Arrays.asList(GameEvent.values()));
        for (GameEvent event : possibleEvents) {
            maxRandomVal += event.chance;
        }

        averageEventInterval = gameLengthSeconds / (float)numberOfEventsThisGame;
        nextEventTime = getNextEventTime(averageEventInterval, eventTimeVariation, eventsRaised);
        System.out.println(numberOfEventsThisGame + " events will occur this game");
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
        if (currentTime > nextEventTime) { // Calculate whether to raise an event
            eventsRaised++;
            nextEventTime = getNextEventTime(averageEventInterval, eventTimeVariation, eventsRaised);

            // Select a random event based on its chance
            float randomValue = random.nextFloat(0, maxRandomVal);
            int eventIndex = (int)((randomValue / maxRandomVal) * possibleEvents.size());
            GameEvent event = possibleEvents.get(eventIndex);
            possibleEvents.remove(eventIndex);
            occurredEventList.add(event);
            maxRandomVal -= event.chance;

            for (GameEventListener listener : listeners) {
                listener.raiseEvent(event);
            }
        }
    }
}
