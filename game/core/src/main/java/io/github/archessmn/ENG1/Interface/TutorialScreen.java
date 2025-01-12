package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.archessmn.ENG1.GameModel.ScoreManager;

import java.util.ArrayList;
import java.util.Map;


public class TutorialScreen implements Screen {
    public static final int VIEWPORT_WIDTH = 960;
    public static final int VIEWPORT_HEIGHT = 540;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private FitViewport viewport;

    private TextureAtlas atlas;
    private Skin skin;
    private Stage stage;
    private AssetManager assetManager;

    private BitmapFont headingFont;
    private BitmapFont bodyFont;

    private Texture background;

    private Table tutorialTable;
    private Table buttonTable;
    private Image currentTutorialScreen;
    private Label guideNameLabel;

    private boolean clicked = false;

    private Rectangle returnRectangle;
    private Texture returnButton;

    final ScreenManager game;

    Vector2 touchPos;


    public TutorialScreen(ScreenManager main) {
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

        assetManager = new AssetManager();

        assetManager.load("tutorials/achievements_tutorial.png", Texture.class);
        assetManager.load("tutorials/building_selection_tutorial.png", Texture.class);
        assetManager.load("tutorials/demolition_tutorial.png", Texture.class);
        assetManager.load("tutorials/event_tutorial.png", Texture.class);
        assetManager.load("tutorials/satisfaction_tutorial.png", Texture.class);
        assetManager.load("ui/title_page.png", Texture.class);
        assetManager.load("ui/return_button.png", Texture.class);

        assetManager.finishLoading();

        background = assetManager.get("ui/title_page.png", Texture.class);

        returnRectangle = new Rectangle();
        returnButton = assetManager.get("ui/return_button.png", Texture.class);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        tutorialTable = new Table();
        tutorialTable.pad(10);
        rootTable.add(tutorialTable).width(VIEWPORT_WIDTH * 0.75f).height(VIEWPORT_HEIGHT * 0.75f);
        rootTable.center();

        buttonTable = new Table();
        tutorialTable.pad(10);

        initButtonTable();
        initTutorialTable();

        touchPos = new Vector2();
    }

    private void initButtonTable() {
        TextButton.TextButtonStyle textButtonStyle = skin.get(TextButton.TextButtonStyle.class);

        TextButton achievementsButton = new TextButton("Achievements", textButtonStyle);
        achievementsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentTutorialScreen.setDrawable(new TextureRegionDrawable(new TextureRegion(assetManager.get("tutorials/achievements_tutorial.png", Texture.class))));
                guideNameLabel.setText("Achievements");
                clicked = true;
            }
        });

        TextButton buildingSelectionButton = new TextButton("Placing", textButtonStyle);
        buildingSelectionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentTutorialScreen.setDrawable(new TextureRegionDrawable(new TextureRegion(assetManager.get("tutorials/building_selection_tutorial.png", Texture.class))));
                guideNameLabel.setText("Placing");
                clicked = true;
            }
        });

        TextButton demolitionButton = new TextButton("Demolition", textButtonStyle);
        demolitionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentTutorialScreen.setDrawable(new TextureRegionDrawable(new TextureRegion(assetManager.get("tutorials/demolition_tutorial.png", Texture.class))));
                guideNameLabel.setText("Demolition");
                clicked = true;
            }
        });

        TextButton eventButton = new TextButton("Events", textButtonStyle);
        eventButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentTutorialScreen.setDrawable(new TextureRegionDrawable(new TextureRegion(assetManager.get("tutorials/event_tutorial.png", Texture.class))));
                guideNameLabel.setText("Events");
                clicked = true;
            }
        });

        TextButton satisfactionButton = new TextButton("Student Satisfaction", textButtonStyle);
        satisfactionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentTutorialScreen.setDrawable(new TextureRegionDrawable(new TextureRegion(assetManager.get("tutorials/satisfaction_tutorial.png", Texture.class))));
                guideNameLabel.setText("Student Satisfaction");
                clicked = true;
            }
        });

        buttonTable.add(achievementsButton).fillX().pad(10).row();
        buttonTable.add(buildingSelectionButton).fillX().pad(10).row();
        buttonTable.add(demolitionButton).fillX().pad(10).row();
        buttonTable.add(eventButton).fillX().pad(10).row();
        buttonTable.add(satisfactionButton).fillX().pad(10).row();
    }

    private void initTutorialTable() {
        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);

        Label guidesLabel = new Label("GUIDES", labelStyle);
        guideNameLabel = new Label("{GUIDE}", labelStyle);
        guidesLabel.setFontScale(1.3f);
        guideNameLabel.setFontScale(1.3f);
        guideNameLabel.setVisible(false);
        tutorialTable.add(guidesLabel).padBottom(15).expandX().top();
        tutorialTable.add(guideNameLabel).padBottom(15).expandX().top().row();

        tutorialTable.add(buttonTable);

        currentTutorialScreen = new Image(assetManager.get("tutorials/achievements_tutorial.png", Texture.class));
        currentTutorialScreen.setVisible(false);
        tutorialTable.add(currentTutorialScreen);
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
                clicked = false;
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
        headingFont.draw(batch, "A GUIDE TO BUILDING A CAMPUS", 140, 520);
        batch.draw(returnButton, returnRectangle.x, returnRectangle.y, returnRectangle.width, returnRectangle.height);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(tutorialTable.getX(), tutorialTable.getY(), tutorialTable.getWidth(), tutorialTable.getHeight());
        shapeRenderer.end();

        // Displays the tutorial screen and title when a button is clicked
        if (clicked) {
            guideNameLabel.setVisible(true);
            currentTutorialScreen.setVisible(true);
        }
        else {
            guideNameLabel.setVisible(false);
            currentTutorialScreen.setVisible(false);
        }

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
