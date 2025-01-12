package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
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

    // All assets that would be used across all screens
    public AssetManager assetManager;

    /**
     * Create is responsible for setting all variables.
     * It is effectively the constructor.
     */
    public void create() {
        loadAssetManager();

        //Creates instance of the main menu screen
        menuScreen = new MenuScreen(this);

        // Initiate game to the game screen.
        setScreen(menuScreen);
    }

    public void render() {
        super.render();
    }

    /**
     * Loads all the used assets into the asset manager.
     */
    private void loadAssetManager() {
        assetManager = new AssetManager();

        assetManager.load("gym.png", Texture.class);
        assetManager.load("halls.png", Texture.class);
        assetManager.load("lecture_hall.png", Texture.class);
        assetManager.load("pub.png", Texture.class);
        assetManager.load("piazza.png", Texture.class);
        assetManager.load("lake.jpg", Texture.class);
        assetManager.load("rock.png", Texture.class);
        assetManager.load("tree.png", Texture.class);
        assetManager.load("construction.png", Texture.class);
        assetManager.load("rubble.png", Texture.class);
        assetManager.load("missing_texture.png", Texture.class);
        assetManager.load("achievement.png", Texture.class);

        // EVENTS
        assetManager.load("events/lecture_view.png", Texture.class);
        assetManager.load("events/flooding.png", Texture.class);
        assetManager.load("events/gym_hype.png", Texture.class);
        assetManager.load("events/tournament_won.png", Texture.class);
        assetManager.load("events/tree_hype.png", Texture.class);
        assetManager.load("events/too_many_buildings.png", Texture.class);
        assetManager.load("events/rock_climbing.png", Texture.class);
        assetManager.load("events/longboi.png", Texture.class);

        // UI
        assetManager.load("ui/active_event_background.png", Texture.class);
        assetManager.load("ui/notification_background.png", Texture.class);
        assetManager.load("ui/title_page.png", Texture.class);
        assetManager.load("ui/return_button.png", Texture.class);
        assetManager.load("ui/leaderboard_button.png", Texture.class);
        assetManager.load("ui/new_game_button.png", Texture.class);
        assetManager.load("ui/tutorial_button.png", Texture.class);
        assetManager.load("ui/logo.png", Texture.class);

        // TUTORIAL GUIDES
        assetManager.load("tutorials/achievements_tutorial.png", Texture.class);
        assetManager.load("tutorials/building_selection_tutorial.png", Texture.class);
        assetManager.load("tutorials/demolition_tutorial.png", Texture.class);
        assetManager.load("tutorials/event_tutorial.png", Texture.class);
        assetManager.load("tutorials/satisfaction_tutorial.png", Texture.class);

        assetManager.finishLoading();
    }
}
