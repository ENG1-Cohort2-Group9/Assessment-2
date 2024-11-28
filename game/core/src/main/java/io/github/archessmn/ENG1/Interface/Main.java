package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.archessmn.ENG1.GameModel.*;
import io.github.archessmn.ENG1.GameModel.World;


import java.util.HashMap;

import static java.lang.Math.floorDiv;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Main extends ApplicationAdapter {
    public static final Integer VIEWPORT_WIDTH = 960;
    public static final Integer VIEWPORT_HEIGHT = 540;

    World world;

    AssetManager assetManager;

    TextureAtlas atlas;
    Skin skin;

    ShapeRenderer gridRenderer;
    ShapeRenderer shapeRenderer;
    ShapeRenderer blockRenderer;

    SpriteBatch batch;

    FitViewport viewport;

    Vector2 touchPos;
    Vector2 unprojectedTouchPos;

    Array<Building> draggableBuildings;

    float gameTimer;

    Rectangle buildingRectangle;
    BitmapFont font;
    Boolean isClicked = false;
    Building buildingClicked = null;

    Boolean paused = true;
    Boolean gameEnded = false;
    Boolean fullScreen = false;

    private Stage stage;
    private Table rightTable;
    private Label countDownLabel;
    private Label timerLabel;
    private final HashMap<Building.Use, Label> buildingUseCountLabels = new HashMap<>();
    private final HashMap<Building.Use, Label> buildingUseNameLabels = new HashMap<>();


    @Override
    public void create() {
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        // 300 here represents the pixel width of the UI on the right hand side
        world = new World(VIEWPORT_WIDTH - 300, VIEWPORT_HEIGHT);

        atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.addRegions(atlas);

        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);
        TextButtonStyle textButtonStyle = skin.get(TextButtonStyle.class);

        countDownLabel = new Label("Timer", labelStyle);
        timerLabel = new Label("Timer", labelStyle);

        for (Building.Use buildingUse : Building.Use.values()) {
            String useName = buildingUse.toString().charAt(0) + buildingUse.toString().substring(1).toLowerCase();
            buildingUseNameLabels.put(buildingUse, new Label(useName + " buildings:", labelStyle));
        }
        for (Building.Use buildingUse : Building.Use.values()) {
            buildingUseCountLabels.put(buildingUse, new Label("0", labelStyle));
        }

        TextButton button = new TextButton("Clear Buildings", textButtonStyle);


        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        rightTable = new Table();

        rightTable.pad(10);

        rootTable.right().add(rightTable).expandY().fillY().width(300);

        rightTable.add(countDownLabel).row();
        rightTable.add(timerLabel).row();
        for (Building.Use buildingUse : Building.Use.values()) {
            rightTable.add(buildingUseNameLabels.get(buildingUse)).left();
            rightTable.add(buildingUseCountLabels.get(buildingUse)).right().row();
        }
        rightTable.add(new Label("\nHelp:", labelStyle)).row();
        rightTable.add(new Label("Drag a building from below to place it", labelStyle)).left().top().row();
        rightTable.add(new Label("onto the grid. Don't overlap them!", labelStyle)).left().top().row();
        rightTable.add(new Label("Gym  Halls  Lecture Hall  Office  Piazza", labelStyle)).expandX().expandY().bottom();

        assetManager = new AssetManager();

        assetManager.load("gym.png", Texture.class);
        assetManager.load("halls.png", Texture.class);
        assetManager.load("lecturehall.png", Texture.class);
        assetManager.load("offices.png", Texture.class);
        assetManager.load("piazza.png", Texture.class);
        assetManager.load("construction.png", Texture.class);
        assetManager.load("missing_texture.png", Texture.class);

        assetManager.finishLoading();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/Arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = (int) (0.05f * Gdx.graphics.getHeight());
        font = generator.generateFont(parameter);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());
        generator.dispose();

        shapeRenderer = new ShapeRenderer();
        gridRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        touchPos = new Vector2();
        unprojectedTouchPos = new Vector2();

//        buildings = new Array<>();
        draggableBuildings = new Array<>();



        draggableBuildings.add(new GymBuilding(660, 40, true));
        draggableBuildings.add(new HallsBuilding(720, 40, true));
        draggableBuildings.add(new LectureHallBuilding(780, 40, true));
        draggableBuildings.add(new OfficeBuilding(840, 40, true));
        draggableBuildings.add(new PiazzaBuilding(900, 40, true));

        gameTimer = 0f;

        blockRenderer = new ShapeRenderer();

        buildingRectangle = new Rectangle();

        // defaults the
        fullScreen = false;

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                world.buildings.clear();
            }
        });
    }

    @Override
    public void render() {
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) paused = !paused;

        // Toggles fullscreen when F11 is pressed
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)){
            fullScreen = !fullScreen;
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            if (fullScreen) {
                Gdx.graphics.setFullscreenMode(currentMode);
            }
            else {

                Gdx.graphics.setWindowedMode(960, 540);
            }
        }
        if (gameEnded) {
            buildingClicked = null;
            return;
        }

        touchPos.set(Gdx.input.getX(), Gdx.input.getY());
        unprojectedTouchPos.set(viewport.unproject(touchPos));

        isClicked = Gdx.input.isTouched();

        if (Gdx.input.justTouched()) { // Start drag
            for (int i = draggableBuildings.size - 1; i >= 0; i--) {
                Building building = draggableBuildings.get(i);

                if (building.getBounds().contains(unprojectedTouchPos)) {
                    buildingClicked = building.makeCopy();
                    break;
                }
            }
        } else if (!isClicked && buildingClicked != null) { // Click released
            world.addBuilding(buildingClicked); // Places the building if it passes all checks

            buildingClicked = null;
        }

        if (buildingClicked != null) { // Track building to mouse position for drag
            buildingClicked.setCenter(touchPos.x, touchPos.y);
        }
    }

    private void logic() {
        if (paused || gameEnded) return;

        float delta = Gdx.graphics.getDeltaTime();

        world.tickBuildings(delta);

        gameTimer += delta;

        // Ends the game when the timer exceeds 5 minutes
        if (gameTimer >= 300) {
            gameEnded = true;
        }

        stage.act(delta);
    }

    private void draw() {
        ScreenUtils.clear(Color.OLIVE);
        viewport.apply();

        batch.setProjectionMatrix(viewport.getCamera().combined);

        drawGrid(shapeRenderer);

        // Draw red outline for where the logo will snap to
        if (buildingClicked != null) {
            if (world.doesBuildingOverlap(buildingClicked)) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            } else {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            }
            shapeRenderer.setColor(Color.RED);
            Vector2 buildingCoords = buildingClicked.getRawGridCoords();
            shapeRenderer.rect(buildingCoords.x - (buildingClicked.width / 2), buildingCoords.y - (buildingClicked.height / 2), buildingClicked.width, buildingClicked.height);
            shapeRenderer.end();
        }


        blockRenderer.begin(ShapeRenderer.ShapeType.Filled);
        blockRenderer.setColor(Color.DARK_GRAY);

        blockRenderer.rect(rightTable.getX(), rightTable.getY(), rightTable.getWidth(), rightTable.getHeight());

        blockRenderer.end();

        batch.begin();

        drawBuildings(batch, assetManager);

        // Draws a 5-minute countdown timer for the games length
        // If it's a whole minute, it displays :00 for the seconds
        // Otherwise it gets the remainder of gameTimer divided by 60 for the seconds.
        if (60 - (int) gameTimer % 60 == 60) {
            countDownLabel.setText(floorDiv(300 - (int) gameTimer, 60) + ":00");
        }
        else {
            countDownLabel.setText(floorDiv(300 - (int) gameTimer, 60) + ":" + String.format("%02d", 60 - (int) gameTimer % 60));
        }

        timerLabel.setText(String.format("Year: %d, Day: %d", (int) (gameTimer / 60) + 1, (int) ((gameTimer % 60) / (60 / (double) 365)) + 1));
        if (buildingClicked != null) {
            if (world.doesBuildingOverlap(buildingClicked)) {
                font.draw(batch, "Buildings overlap", 20, 520);
            }
        }

        if (paused) {
            font.draw(batch, "Paused, press P to resume", 20, 460);
            font.draw(batch, "Building icons from macrovector on Freepik", 0, 25);
        }

        if (gameEnded) {
            font.draw(batch, "End of the game!", 20, 460);
            font.draw(batch, "Building icons from macrovector on Freepik", 0, 25);
        }

        batch.end();

        for (Building.Use use : Building.Use.values()) {
            buildingUseCountLabels.get(use).setText(world.buildingUseCounts.get(use));
        }

        stage.draw();
    }

    public void drawBuildings(Batch batch, AssetManager assetManager) {
        for (Building building : draggableBuildings) {
            drawBuilding(batch, assetManager, building);
        }
        for (Building building : world.buildings) {
            drawBuilding(batch, assetManager, building);
        }
    }

    private static void drawBuilding(Batch batch, AssetManager assetManager, Building building) {
        Sprite sprite = null;
        if (building.timeUntilBuilt > 0 && !building.built) {
            sprite = new Sprite(assetManager.get(building.unbuiltSpriteName, Texture.class));
        }
        else {
            sprite = new Sprite(assetManager.get(building.spriteName, Texture.class));
        }

        sprite.setSize(building.width, building.height);
        sprite.setPosition(building.x, building.y);
        sprite.draw(batch);
    }

    /**
     * Draws a grid into the viewport using the {@link ShapeRenderer} passed to it.
     * @param gridRenderer The {@link ShapeRenderer} used to draw the grid.
     */
    public static void drawGrid(ShapeRenderer gridRenderer) {
        gridRenderer.begin(ShapeRenderer.ShapeType.Line);

        gridRenderer.setColor(new Color(0x5b7e13ff));

        float gridWidth = (VIEWPORT_WIDTH / 16f);
        float gridHeight = (VIEWPORT_HEIGHT / 9f);

        for (int v = 1; v < 9; v++) {
            gridRenderer.line(0, gridHeight * v, VIEWPORT_WIDTH - 300, gridHeight * v);
        }

        for (int h = 1; h < 11; h++) {
            gridRenderer.line(gridWidth * h, 0, gridWidth * h, VIEWPORT_HEIGHT);
        }

        gridRenderer.end();

    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        gridRenderer.dispose();
        batch.dispose();
        font.dispose();
        assetManager.dispose();
    }
}
