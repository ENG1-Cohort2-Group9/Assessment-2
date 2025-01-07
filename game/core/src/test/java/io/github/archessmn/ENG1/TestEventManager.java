package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.EventManager;
import io.github.archessmn.ENG1.GameModel.GameEvent;
import io.github.archessmn.ENG1.GameModel.GameEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestEventManager {

    EventManager eventManager;
    int eventsRaised;

    @BeforeEach
    public void setUp() {
        eventManager = new EventManager(new GameEventHandler[] { this::mockListener }, 300f);
        eventsRaised = 0;
    }

    private void mockListener(GameEvent event) {
        eventsRaised++;
    }

    @Test
    public void testRaiseEvent() {
        eventManager.processEvents(150f);

        assertEquals(1, eventsRaised);
    }

    @Test
    public void testDoNotRaiseEvent() {
        eventManager.processEvents(1f);

        assertEquals(0, eventsRaised);
    }

    @Test
    public void testDisableEvent() {
        eventManager.disableEvent(GameEvent.FLOODING);

        assertFalse(eventManager.isEventEnabled(GameEvent.FLOODING));
    }

    @Test
    public void testEnableEvent() {
        testDisableEvent();

        eventManager.enableEvent(GameEvent.FLOODING);

        assertTrue(eventManager.isEventEnabled(GameEvent.FLOODING));
    }

    @Test
    public void testEventAlreadyEnabled() {
        eventManager.enableEvent(GameEvent.FLOODING);

        assertTrue(eventManager.isEventEnabled(GameEvent.FLOODING));
    }

    @Test
    public void testRepeatEvent() {
        for (GameEvent event : GameEvent.values()) {
            eventManager.disableEvent(event);
        }

        eventManager.enableEvent(GameEvent.ROCK_CLIMBING);

        eventManager.processEvents(100f);
        eventManager.processEvents(100f);

        assertTrue(eventsRaised <= 1);
    }

    @Test
    public void testNoEvents() {
        for (GameEvent event : GameEvent.values()) {
            eventManager.disableEvent(event);
        }

        assertDoesNotThrow(() -> eventManager.processEvents(150f));
    }
}
