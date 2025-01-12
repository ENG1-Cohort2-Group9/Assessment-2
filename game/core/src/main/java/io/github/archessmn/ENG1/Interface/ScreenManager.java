package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ScreenManager extends Game {

    public MenuScreen menuScreen;
    public LeaderboardScreen leaderboardScreen;
    public TutorialScreen tutorialScreen;
    public GameScreen gameScreen;

    public Boolean fullScreen;

    /**
     * Create is responsible for setting all variables.
     * It is effectively the constructor.
     */
    public void create() {

        //Creates instance of the main menu screen
        menuScreen = new MenuScreen(this);

        fullScreen = false;
        // Initiate game to the game screen.
        setScreen(menuScreen);
    }

    public void render() {
        super.render();
    }

    // Disposes of all textures.
    public void dispose() {
    }
}
