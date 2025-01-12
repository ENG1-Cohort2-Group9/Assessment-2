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

    /**
     * Doesn't actually render anything, but instead is used to check for the user pressing F11 at any time.
     */
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)){
            fullScreen = Gdx.graphics.isFullscreen();
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            if (fullScreen)
                Gdx.graphics.setWindowedMode(960, 540);
            else
                Gdx.graphics.setFullscreenMode(currentMode);
        }

        super.render();
    }

    // Disposes of all textures.
    public void dispose() {
    }
}
