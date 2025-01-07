package io.github.archessmn.ENG1;

import io.github.archessmn.ENG1.GameModel.EventManager;
import io.github.archessmn.ENG1.GameModel.GameEventHandler;
import io.github.archessmn.ENG1.GameModel.GameEventListener;
import org.junit.jupiter.api.BeforeEach;

public class TestEventManager {

    EventManager eventManager;

    @BeforeEach
    public void setUp() {
        eventManager = new EventManager(new GameEventHandler[] {this::mockListener})
    }

    private void mockListener() {

    }
}
