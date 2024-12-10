package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
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
import io.github.archessmn.ENG1.GameModel.Objects.*;

import java.util.HashMap;
import static java.lang.Math.floorDiv;

public class GameScreen implements Screen {
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


    Rectangle buildingRectangle;
    BitmapFont font;
    Boolean isClicked = false;
    Building buildingToPlace = null;
    Building selectedAsset = null;
    float selectionTimer = 5f;

    Boolean paused = true;
    Boolean gameEnded = false;
    Boolean fullScreen = false;

    private Stage stage;
    private Table sideMenu;
    private Label countDownLabel;
    private Label timerLabel;
    private Label selectedBuildingLabal;
    private TextButton actionButton;
    private final HashMap<Use, Label> buildingUseCountLabels = new HashMap<>();
    private final HashMap<Use, Label> buildingUseNameLabels = new HashMap<>();
    private float timeEventShownAt = -10f;
    private GameEvent currentEvent = null;

    final ScreenManager game;
    final float EVENT_NOTIFICATION_TIME = 5f; // How long event notifications are shown before disappearing



    public GameScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        // 300 here represents the pixel width of the UI on the right hand side
        world = new World(VIEWPORT_WIDTH - 300, VIEWPORT_HEIGHT, new GameEventListener(this::showEventPopup));

        atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.addRegions(atlas);

        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);
        TextButtonStyle textButtonStyle = skin.get(TextButtonStyle.class);

        countDownLabel = new Label("Timer", labelStyle);
        timerLabel = new Label("Timer", labelStyle);
        selectedBuildingLabal = new Label("Building", labelStyle);

        actionButton = new TextButton("Action", textButtonStyle);
        actionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedAsset.uses[0] == Use.TERRAIN) {
                    world.destroyTerrain(selectedAsset);
                }
                else if (selectedAsset.uses[0] != null) {
                    world.destroyBuilding(selectedAsset);
                }
            }
        });

        for (Use buildingUse : Use.values()) {
            String useName = buildingUse.toString().charAt(0) + buildingUse.toString().substring(1).toLowerCase();
            buildingUseNameLabels.put(buildingUse, new Label(useName + " buildings:", labelStyle));
        }
        for (Use buildingUse : Use.values()) {
            buildingUseCountLabels.put(buildingUse, new Label("0", labelStyle));
        }


        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        sideMenu = new Table();

        sideMenu.pad(10);

        rootTable.right().add(sideMenu).expandY().fillY().width(300);

        sideMenu.add(countDownLabel).row();
        sideMenu.add(timerLabel).row();
        for (Use buildingUse : Use.values()) {
            sideMenu.add(buildingUseNameLabels.get(buildingUse)).left();
            sideMenu.add(buildingUseCountLabels.get(buildingUse)).right().row();
        }
        sideMenu.add(new Label("\nSelection:", labelStyle)).left().row();
        sideMenu.add(selectedBuildingLabal).left().top().row();
        sideMenu.add(actionButton).left().top().row();
        sideMenu.add(new Label("Gym  Halls  Lecture Hall  Pub     Piazza", labelStyle)).expandX().expandY().bottom();

        assetManager = new AssetManager();

        assetManager.load("gym.png", Texture.class);
        assetManager.load("halls.png", Texture.class);
        assetManager.load("lecturehall.png", Texture.class);
        assetManager.load("offices.png", Texture.class);
        assetManager.load("piazza.png", Texture.class);
        assetManager.load("lake.jpg", Texture.class);
        assetManager.load("rock.png", Texture.class);
        assetManager.load("tree.png", Texture.class);
        assetManager.load("construction.png", Texture.class);
        assetManager.load("missing_texture.png", Texture.class);
        assetManager.load("plus.png", Texture.class);
        assetManager.load("minus.png", Texture.class);

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

        draggableBuildings = new Array<>();

        draggableBuildings.add(new GymBuilding(660, 40, 0, true));
        draggableBuildings.add(new HallsBuilding(720, 40, 0, true));
        draggableBuildings.add(new LectureHallBuilding(780, 40, 0, true));
        draggableBuildings.add(new Pub(840, 40, 0, true));
        draggableBuildings.add(new PiazzaBuilding(900, 40, 0, true));

        blockRenderer = new ShapeRenderer();

        buildingRectangle = new Rectangle();

        // defaults the screen to be minimised on launch
        fullScreen = false;
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) paused = !paused;

        if (gameEnded) {
            buildingToPlace = null;
            return;
        }

        touchPos.set(Gdx.input.getX(), Gdx.input.getY());
        unprojectedTouchPos.set(viewport.unproject(touchPos));

        isClicked = Gdx.input.isTouched();

        // A check to see if any building/terrain assets have been clicked
        if (Gdx.input.justTouched()) {
            // Loops through all placed terrain assets to see if they have been clicked
            for (Building terrainAsset : world.terrain) {
                if (terrainAsset.getBounds().contains(unprojectedTouchPos)) {
                    selectedAsset = terrainAsset;
                    selectionTimer = 5f;
                    break;
                }
            }

            // Loops through all placed building assets to see if they have been clicked
            for (Building buildingAsset : world.buildings) {
                if (buildingAsset.getBounds().contains(unprojectedTouchPos)) {
                    selectedAsset = buildingAsset;
                    selectionTimer = 5f;
                    break;
                }
            }

            // Initiates the dragging feature for when a menu building has been selected
            for (int i = draggableBuildings.size - 1; i >= 0; i--) {
                Building building = draggableBuildings.get(i);

                if (building.getBounds().contains(unprojectedTouchPos)) {
                    buildingToPlace = building.makeCopy(world.getCurrentTime());
                    break;
                }
            }
        } else if (!isClicked && buildingToPlace != null) { // Click released
            if (unprojectedTouchPos.x <= VIEWPORT_WIDTH - 300) {
                world.addBuilding(buildingToPlace); // Places the building if it passes all checks
            }

            buildingToPlace = null;
        }

        if (buildingToPlace != null) { // Track building to mouse position for drag
            buildingToPlace.setCenter(touchPos.x, touchPos.y);
        }
    }

    private void logic() {
        if (paused || gameEnded) return;

        float delta = Gdx.graphics.getDeltaTime();

        world.worldProcess(delta);

        // Ends the game when the timer exceeds 5 minutes.
        // This should probably be moved from this class to World, it already has a timer
        gameEnded = world.getGameEnded();

        stage.act(delta);
    }

    private void draw() {
        ScreenUtils.clear(Color.OLIVE);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Collision detection for buildings already placed
        if (buildingToPlace != null) {
            // If placing building, draw the grid
            if (isClicked) drawGrid(shapeRenderer);

            if (world.doesBuildingOverlap(buildingToPlace)) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            } else {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            }
            shapeRenderer.setColor(Color.RED);
            Vector2 buildingCoords = buildingToPlace.getRawGridCoords();
            shapeRenderer.rect(buildingCoords.x - (buildingToPlace.width / 2), buildingCoords.y - (buildingToPlace.height / 2), buildingToPlace.width, buildingToPlace.height);
            shapeRenderer.end();
        }

        // Draws side menu bar
        blockRenderer.begin(ShapeRenderer.ShapeType.Filled);
        blockRenderer.setColor(Color.DARK_GRAY);
        blockRenderer.rect(sideMenu.getX(), sideMenu.getY(), sideMenu.getWidth(), sideMenu.getHeight());
        blockRenderer.end();

        batch.begin();

        drawBuildings(batch, assetManager);
        drawSideMenu();

        batch.end();

        for (Use use : Use.values()) {
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
        for (Building building : world.terrain) {
            drawBuilding(batch, assetManager, building);
        }
        if (buildingToPlace != null) {
            drawBuilding(batch, assetManager, buildingToPlace);
        }
    }

    private static void drawBuilding(Batch batch, AssetManager assetManager, Building building) {
        Sprite sprite = null;
        if (!building.built) {
            sprite = new Sprite(assetManager.get(building.unbuiltSpriteName, Texture.class));
        }
        else {
            sprite = new Sprite(assetManager.get(building.spriteName, Texture.class));
        }

        sprite.setSize(building.width, building.height);
        sprite.setPosition(building.x, building.y);
        sprite.draw(batch);

        Sprite efficiencySprite = null;
        // Draw '+' or '-' if building efficiency is not the default value, 0.5
        if (building.getEfficiency() > 0.5f) {
            efficiencySprite = new Sprite(assetManager.get("plus.png", Texture.class));
        }
        else if (building.getEfficiency() < 0.5f) {
            efficiencySprite = new Sprite(assetManager.get("minus.png", Texture.class));
        }
        else {
            return;
        }

        efficiencySprite.setSize(building.width * 0.25f, building.height * 0.25f);
        efficiencySprite.setPosition(building.x + building.width * 0.75f, building.y + building.height * 0.75f);
        efficiencySprite.draw(batch);
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

    public void drawSideMenu() {
        // Draws a 5-minute countdown timer for the games length
        // If it's a whole minute, it displays :00 for the seconds
        // Otherwise it gets the remainder of gameTimer divided by 60 for the seconds.
        float gameTime = world.getCurrentTime();
        if (60 - (int) gameTime % 60 == 60) {
            countDownLabel.setText(floorDiv(300 - (int) gameTime, 60) + ":00");
        }
        else {
            countDownLabel.setText(floorDiv(300 - (int) gameTime, 60) + ":" + String.format("%02d", 60 - (int) gameTime % 60));
        }

        if (selectedAsset != null && selectionTimer <= 5f) {
            selectionTimer -= Gdx.graphics.getDeltaTime();

            selectedBuildingLabal.setVisible(true);
            actionButton.setVisible(true);

            selectedBuildingLabal.setText(selectedAsset.objName);
            if (selectedAsset.uses[0] == Use.TERRAIN) {
                actionButton.setText("Destroy");
            }
            else {
                actionButton.setText("Demolish");
            }
        }
        else {
            selectedBuildingLabal.setVisible(false);
            actionButton.setVisible(false);
        }

        if (selectionTimer <= 0f) {
            selectionTimer = 5f;
            selectedAsset = null;
        }


        timerLabel.setText(String.format("Year: %d, Day: %d", (int) (gameTime / 60) + 1, (int) ((gameTime % 60) / (60 / (double) 365)) + 1));
        if (buildingToPlace != null) {
            if (world.doesBuildingOverlap(buildingToPlace)) {
                font.draw(batch, "Buildings overlap", 20, 520);
            }
        }

        // Show event text (if there is one)
        if (currentEvent != null && world.getCurrentTime() < timeEventShownAt + EVENT_NOTIFICATION_TIME) {
            font.draw(batch, currentEvent.title, 20, 525);
            font.draw(batch, currentEvent.description, 20, 495);
        }

        if (paused) {
            font.draw(batch, "Paused, press P to resume", 20, 460);
            font.draw(batch, "Building icons from macrovector on Freepik", 0, 25);
        }

        if (gameEnded) {
            font.draw(batch, "End of the game!", 20, 460);
            font.draw(batch, "Building icons from macrovector on Freepik", 0, 25);
        }
    }


    public void showEventPopup(GameEvent event) {
        currentEvent = event;
        timeEventShownAt = world.getCurrentTime();
        paused = true;
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        gridRenderer.dispose();
        batch.dispose();
        font.dispose();
        assetManager.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
