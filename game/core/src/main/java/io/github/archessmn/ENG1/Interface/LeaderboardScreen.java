package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.archessmn.ENG1.GameModel.ScoreManager;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Math.floorDiv;

public class LeaderboardScreen implements Screen {
    public static final int VIEWPORT_WIDTH = 960;
    public static final int VIEWPORT_HEIGHT = 540;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private FitViewport viewport;

    private TextureAtlas atlas;
    private Skin skin;
    private Stage stage;

    private BitmapFont headingFont;
    private BitmapFont bodyFont;

    private Table leaderboardTable;
    private ArrayList<Map.Entry<Label, Label>> labelTable;

    private Rectangle returnRectangle;
    private Texture returnButton;

    final ScreenManager game;

    Texture background;

    Vector2 touchPos;

    public LeaderboardScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.addRegions(atlas);

        background = new Texture(Gdx.files.internal("ui/title_page.png"));

        returnRectangle = new Rectangle();
        returnButton = new Texture(Gdx.files.internal("ui/return_button.png"));

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        leaderboardTable = new Table();
        labelTable = new ArrayList<>();

        leaderboardTable.pad(10);
        rootTable.add(leaderboardTable).width(450).height(370);
        rootTable.center();

        initLeaderboard();
        displayScores();

        touchPos = new Vector2();
    }

    private void initLeaderboard() {
        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);
        TextButton.TextButtonStyle textButtonStyle = skin.get(TextButton.TextButtonStyle.class);

        Label uniNameTitle = new Label("UNIVERSITY NAME", labelStyle);
        Label scoreTitle = new Label("SCORE", labelStyle);
        uniNameTitle.setFontScale(1.3f);
        scoreTitle.setFontScale(1.3f);
        leaderboardTable.add(uniNameTitle).padBottom(15).expandX().top();
        leaderboardTable.add(scoreTitle).padBottom(15).expandX().top().row();

        for (int i = 0; i < 10; i++) {
            Label nameLabel = new Label("", labelStyle);
            Label scoreLabel = new Label("", labelStyle);

            labelTable.add(Map.entry(nameLabel, scoreLabel));
            leaderboardTable.add(nameLabel).padBottom(4);
            leaderboardTable.add(scoreLabel).padBottom(4).row();
        }
    }

    private void displayScores() {
        ArrayList<Map.Entry<String, Float>> topScores = ScoreManager.getTopScores("scores.txt");

        int size = topScores.size();
        if (size > 10) {
            topScores.subList(10, size).clear();
        }

        int labelTableIndex = 0;
        for (Map.Entry<String, Float> entry : topScores) {
            Map.Entry<Label, Label> labelEntry = labelTable.get(labelTableIndex);
            labelEntry.getKey().setText(entry.getKey());
            labelEntry.getValue().setText((entry.getValue()) + "% satisfaction");

            labelTableIndex++;
        }
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

        // Generates the heading and body font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/Product_Sans_Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = (int) (0.075f * Gdx.graphics.getHeight());
        parameter.borderWidth = 2;
        parameter.borderColor = Color.DARK_GRAY;
        headingFont = generator.generateFont(parameter);
        headingFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        parameter.size = (int) (0.030f * Gdx.graphics.getHeight());
        parameter.shadowColor = Color.BLACK;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;
        bodyFont = generator.generateFont(parameter);
        bodyFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        generator.dispose();
    }

    private void input() {
        touchPos.set(Gdx.input.getX(), Gdx.input.getY());
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();

        Vector3 touch = new Vector3(touchPos.x, touchPos.y, 0);
        viewport.getCamera().unproject(touch);

        if (Gdx.input.justTouched()) {
            // Checks to see if the Return button is clicked
            if (returnRectangle.contains(touch.x, touch.y)) {
                game.setScreen(game.menuScreen);
            }
        }

        stage.act(delta);
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // All positions have been tuned to look good on the screen; they are not based on anything mathematical
        returnRectangle.setSize(175, 43.75f);
        returnRectangle.setPosition(760, 10);

        // Renders the background, title and return button
        batch.begin();
        batch.draw(background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        headingFont.draw(batch, "UNISIM LOCAL LEADERBOARD", 170, 500);
        batch.draw(returnButton, returnRectangle.x, returnRectangle.y, returnRectangle.width, returnRectangle.height);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(leaderboardTable.getX(), leaderboardTable.getY(), leaderboardTable.getWidth(), leaderboardTable.getHeight());
        shapeRenderer.end();

        stage.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        headingFont.dispose();
        bodyFont.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
