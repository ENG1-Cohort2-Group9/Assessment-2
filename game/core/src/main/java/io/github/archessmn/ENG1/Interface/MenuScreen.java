package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.Objects;

// I WILL COMMENT THIS EVENTUALLY

public class MenuScreen implements Screen {
    public static final int VIEWPORT_WIDTH = 960;
    public static final int VIEWPORT_HEIGHT = 540;

    private TextureAtlas atlas;
    private Skin skin;
    private Stage stage;

    private Table inputTable;
    private TextField inputField;

    private SpriteBatch batch;
    private FitViewport viewport;

    private Texture background;
    private Texture logo;

    private Array<Rectangle> buttons;
    private Texture newGameButton;
    private Rectangle newGameRectangle;
    private Texture leaderboardButton;
    private Rectangle leaderboardRectangle;
    private Texture tutorialButton;
    private Rectangle tutorialRectangle;

    private Vector2 touchPos;

    private final float LOGO_SIZE = 250f;

    private final ScreenManager game;

    public MenuScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.addRegions(atlas);

        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        inputTable = new Table();
        inputTable.pad(10);
        rootTable.add(inputTable).width((float) VIEWPORT_WIDTH * 0.25f).height((float) VIEWPORT_HEIGHT * 0.2f);
        rootTable.bottom();

        initInputTable();

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

    private void initInputTable() {
        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);

        Label inputLabel = new Label("Enter Campus Name:", labelStyle);
        inputLabel.setAlignment(2);
        inputLabel.setFontScale(1.25f);
        inputTable.add(inputLabel).expandX().fillX().row();

        inputField = new TextField("University of York", skin);
        inputField.setAlignment(1);
        inputTable.add(inputField).expandX().fillX();
    }

    @Override
    public void render(float v) {
        input();
        logic();
        draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, true);
        stage.getViewport().update(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, true);
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
                    if (button == newGameRectangle && !Objects.equals(inputField.getText(), "")) {
                        game.gameScreen = new GameScreen(game, inputField.getText());
                        game.setScreen(game.gameScreen);
                    }
                    if (button == leaderboardRectangle) {
                        game.leaderboardScreen = new LeaderboardScreen(game);
                        game.setScreen(game.leaderboardScreen);
                    }
                    if (button == tutorialRectangle) {
                        game.tutorialScreen = new TutorialScreen(game);
                        game.setScreen(game.tutorialScreen);
                    }
                    break;
                }
            }
        }

        float delta = Gdx.graphics.getDeltaTime();
        stage.act(delta);
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
        batch.draw(logo, viewport.getWorldWidth() * 0.3f - (LOGO_SIZE/2), viewport.getWorldHeight() / 2 - (LOGO_SIZE/2), LOGO_SIZE, LOGO_SIZE);

        // Renders the buttons
        batch.draw(newGameButton, newGameRectangle.x, newGameRectangle.y, newGameRectangle.width, newGameRectangle.height);
        batch.draw(leaderboardButton, leaderboardRectangle.x, leaderboardRectangle.y, leaderboardRectangle.width, leaderboardRectangle.height);
        batch.draw(tutorialButton, tutorialRectangle.x, tutorialRectangle.y, tutorialRectangle.width, tutorialRectangle.height);
        batch.end();

        stage.draw();
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
