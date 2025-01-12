package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * ScreenManager is responsible for holding all the screens, alongside changing the current screen displayed.
 */
public class ScreenManager extends Game {

    // All the screens that belong to the ScreenManager
    public MenuScreen menuScreen;
    public LeaderboardScreen leaderboardScreen;
    public TutorialScreen tutorialScreen;
    public GameScreen gameScreen;

    /**
     * Create is responsible for setting all variables.
     * It is effectively the constructor.
     */
    public void create() {

        //Creates instance of the main menu screen
        menuScreen = new MenuScreen(this);

        // Initiate game to the game screen.
        setScreen(menuScreen);
    }

    public void render() {
        super.render();
    }
}
