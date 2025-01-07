package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.archessmn.ENG1.GameModel.Objects.BuildingObject;
import io.github.archessmn.ENG1.GameModel.Objects.MapObject;

import static java.lang.Math.floorDiv;

// I WILL COMMENT THIS EVENTUALLY

public class MenuScreen implements Screen {
    public static final int VIEWPORT_WIDTH = 960;
    public static final int VIEWPORT_HEIGHT = 540;

    SpriteBatch batch;
    FitViewport viewport;
    Stage stage;
    final ScreenManager game;

    Texture background;
    Texture logo;

    Array<Rectangle> buttons;
    Texture newGameButton;
    Rectangle newGameRectangle;
    Texture leaderboardButton;
    Rectangle leaderboardRectangle;
    Texture tutorialButton;
    Rectangle tutorialRectangle;

    Vector2 touchPos;

    float logoSize = 250f;

    public MenuScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        stage = new Stage(viewport);

        background = new Texture(Gdx.files.internal("ui/title_page.png"));
        logo = new Texture(Gdx.files.internal("ui/logo.png"));

        newGameButton = new Texture(Gdx.files.internal("ui/new_game_button.png"));
        newGameRectangle = new Rectangle();
        leaderboardButton = new Texture(Gdx.files.internal("ui/leaderboard_button.png"));
        leaderboardRectangle = new Rectangle();
        tutorialButton = new Texture(Gdx.files.internal("ui/tutorial_button.png"));
        tutorialRectangle = new Rectangle();

        buttons = new Array<>();
        buttons.add(newGameRectangle, leaderboardRectangle, tutorialRectangle);

        touchPos = new Vector2();
    }

    @Override
    public void render(float v) {
        input();
        logic();
        draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    private void input() {
        touchPos.set(Gdx.input.getX(), Gdx.input.getY());
    }

    private void logic() {
        Vector3 touch = new Vector3(touchPos.x, touchPos.y, 0);
        viewport.getCamera().unproject(touch);

        if (Gdx.input.justTouched()) {
            // Checks to see if any of the buttons were clicked
            for (Rectangle button : buttons) {
                if (button.contains(touch.x, touch.y)) {
                    if (button == newGameRectangle) {
                        game.setScreen(game.gameScreen);
                    }
                    else if (button == leaderboardRectangle) {
                        game.setScreen(game.leaderboardScreen);
                    }
                    else {
                        game.setScreen(game.tutorialScreen);
                    }
                    break;
                }
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        // All positions have been tuned to look good on the screen; they are not based on anything mathematical
        newGameRectangle.setSize(250, 62.5f);
        newGameRectangle.setCenter(viewport.getWorldWidth() * 0.7f, viewport.getWorldHeight() * 0.6f);
        leaderboardRectangle.setSize(200, 50);
        leaderboardRectangle.setCenter(viewport.getWorldWidth() * 0.7f, viewport.getWorldHeight() * 0.475f);
        tutorialRectangle.setSize(200, 50);
        tutorialRectangle.setCenter(viewport.getWorldWidth() * 0.7f, viewport.getWorldHeight() * 0.36f);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Renders the background and logo
        batch.begin();
        batch.draw(background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.draw(logo, viewport.getWorldWidth() * 0.3f - (logoSize/2), viewport.getWorldHeight() / 2 - (logoSize/2), logoSize, logoSize);

        // Renders the buttons
        batch.draw(newGameButton, newGameRectangle.x, newGameRectangle.y, newGameRectangle.width, newGameRectangle.height);
        batch.draw(leaderboardButton, leaderboardRectangle.x, leaderboardRectangle.y, leaderboardRectangle.width, leaderboardRectangle.height);
        batch.draw(tutorialButton, tutorialRectangle.x, tutorialRectangle.y, tutorialRectangle.width, tutorialRectangle.height);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();

    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
