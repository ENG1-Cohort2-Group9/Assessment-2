package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import static com.badlogic.gdx.graphics.Color.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import static com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.archessmn.ENG1.GameModel.*;
import io.github.archessmn.ENG1.GameModel.Objects.*;

import java.util.Objects;

import static java.lang.Math.floorDiv;

import static io.github.archessmn.ENG1.GameModel.GridUtils.*;
import static io.github.archessmn.ENG1.GameModel.Objects.TerrainObject.Feature.*;
import static io.github.archessmn.ENG1.GameModel.Objects.BuildingName.*;
import static io.github.archessmn.ENG1.GameModel.World.*;


public class GameScreen implements Screen {
    public static final int VIEWPORT_WIDTH = 960;
    public static final int VIEWPORT_HEIGHT = 540;
    private static final int SIDE_PANEL_WIDTH = 300;
    public static final int MAP_WIDTH = VIEWPORT_WIDTH - SIDE_PANEL_WIDTH;

    public static final int TILE_WIDTH = MAP_WIDTH / GRID_WIDTH;
    public static final int TILE_HEIGHT = VIEWPORT_HEIGHT/ GRID_HEIGHT;

    private static final float EVENT_NOTIFICATION_TIME = 5f; // How long event notifications are shown before disappearing
    private static final float DEMOLISH_COOLDOWN = 0.25f;

    private World world;

    private AssetManager assetManager;

    private TextureAtlas atlas;
    private Skin skin;

    private ShapeRenderer gridRenderer;
    private ShapeRenderer shapeRenderer;
    private ShapeRenderer blockRenderer;

    private SpriteBatch batch;

    private FitViewport viewport;

    private Vector2 touchPos;
    private Vector2 unprojectedTouchPos;
    private boolean isClicked = false;

    private BitmapFont headingFont;
    private BitmapFont bodyFont;
    private BitmapFont constructionFont;

    private MapObject objectToPlace = null;
    private Array<BuildingObject> selectableBuildings = new Array<>();
    private Array<TerrainObject> selectableTerrains = new Array<>();
    private int selectableBuildingsIndex = 0;
    private int selectableTerrainsIndex = 0;

    private boolean paused = true;
    private boolean gameEnded = false;

    private Stage stage;
    private Table sideMenu;
    private Label countDownLabel;
    private Label timerLabel;
    private Label selectedBuildingLabel;
    private Label selectedTerrainLabel;
    private Label selectedBuildingCapacityLabel;
    private TextButton demolishButton;
    private Pixmap demolishCursor;
    private final Array<Label> satisfactionCountLabels = new Array<>();
    private final Array<Label> satisfactionVarLabels = new Array<>();

    // Public and static references for the notification UI
    public static Sprite notificationBackground;
    public static Sprite notificationImage;

    private Array<Tuple<String, Integer>> displayedActiveEventTimes = new Array<>();

    private float[] satisfactionScoreCaps;

    private boolean demolishMode = false;
    private float demolishCooldownTimer = 0f;

    final ScreenManager game;

    private String uniName = "Guest";


    public GameScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        world = new World(VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, VIEWPORT_HEIGHT, new GameEventHandler[] {this::showEventPopup}, this::showAchievementPopup);

        atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.addRegions(atlas);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        selectableBuildings = new Array<>();
        selectableBuildings.add(new BuildingObject(710, 160, 0, HALLS));
        selectableBuildings.add(new BuildingObject(710, 160, 0, GYM));
        selectableBuildings.add(new BuildingObject(710, 160, 0, LECTURE_HALL));
        selectableBuildings.add(new BuildingObject(710, 160, 0, PIAZZA));
        selectableBuildings.add(new BuildingObject(710, 160, 0, PUB));
        selectableTerrains = new Array<>();
        selectableTerrains.add(new TerrainObject(849, 160, LAKE));
        selectableTerrains.add(new TerrainObject(849, 160, ROCK));
        selectableTerrains.add(new TerrainObject(849, 160, TREE));

        assetManager = new AssetManager();

        assetManager.load("gym.png", Texture.class);
        assetManager.load("halls.png", Texture.class);
        assetManager.load("lecturehall.png", Texture.class);
        assetManager.load("pub.png", Texture.class);
        assetManager.load("piazza.png", Texture.class);
        assetManager.load("lake.jpg", Texture.class);
        assetManager.load("rock.png", Texture.class);
        assetManager.load("tree.png", Texture.class);
        assetManager.load("construction.png", Texture.class);
        assetManager.load("rubble.png", Texture.class);
        assetManager.load("missingTexture.png", Texture.class);
        assetManager.load("LectureView.png", Texture.class);
        assetManager.load("Flooding.png", Texture.class);
        assetManager.load("GymHype.png", Texture.class);
        assetManager.load("TournamentWon.png", Texture.class);
        assetManager.load("TreeHype.png", Texture.class);
        assetManager.load("TooManyBuildings.png", Texture.class);
        assetManager.load("RockClimbing.png", Texture.class);
        assetManager.load("LongBoi.png", Texture.class);
        assetManager.load("ActiveEventBg.png", Texture.class);
        assetManager.load("Achievement.png", Texture.class);

        assetManager.load("ui/notification_background.png", Texture.class);

        assetManager.finishLoading();

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        sideMenu = new Table();
        sideMenu.pad(10);
        rootTable.add(sideMenu).expandY().fillY().width(SIDE_PANEL_WIDTH);
        rootTable.right();
        initSideMenu();

        NotificationHandler.initNotification(assetManager.get("ui/notification_background.png", Texture.class));

        shapeRenderer = new ShapeRenderer();
        gridRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        touchPos = new Vector2();
        unprojectedTouchPos = new Vector2();

        blockRenderer = new ShapeRenderer();
    }

    private void initSideMenu() {
        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);
        TextButtonStyle textButtonStyle = skin.get(TextButtonStyle.class);

        countDownLabel = new Label("Timer", labelStyle);
        timerLabel = new Label("Timer", labelStyle);

        selectedBuildingLabel = new Label("{Building}", labelStyle);
        selectedTerrainLabel = new Label("{Terrain}", labelStyle);
        selectedBuildingCapacityLabel = new Label("{Capacity}\n ", labelStyle);
        selectedBuildingLabel.setFontScale(0.9f);
        selectedTerrainLabel.setFontScale(0.9f);
        selectedBuildingCapacityLabel.setFontScale(0.7f);

        demolishButton = new TextButton("Demolish Mode: OFF", textButtonStyle);
        demolishButton.setColor(DARK_GRAY);
        demolishButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                demolishMode = !demolishMode;
            }
        });

        demolishCursor = new Pixmap(Gdx.files.internal("ui/demolish_cursor.png"));

        // Initialises the scroll buttons and their actions
        TextButton buildingUpButton = new TextButton("^", textButtonStyle);
        buildingUpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectableBuildingsIndex == selectableBuildings.size - 1) {
                    selectableBuildingsIndex = 0;
                }
                else {
                    selectableBuildingsIndex++;
                }
            }
        });
        TextButton buildingDownButton = new TextButton("v", textButtonStyle);
        buildingDownButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectableBuildingsIndex == 0) {
                    selectableBuildingsIndex = selectableBuildings.size - 1;
                }
                else {
                    selectableBuildingsIndex--;
                }
            }
        });
        TextButton terrainUpButton = new TextButton("^", textButtonStyle);
        terrainUpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectableTerrainsIndex == selectableTerrains.size - 1) {
                    selectableTerrainsIndex = 0;
                }
                else {
                    selectableTerrainsIndex++;
                }
            }
        });
        TextButton terrainDownButton = new TextButton("v", textButtonStyle);
        terrainDownButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectableTerrainsIndex == 0) {
                    selectableTerrainsIndex = selectableTerrains.size - 1;
                } else {
                    selectableTerrainsIndex--;
                }
            }
        });


        satisfactionVarLabels.add(new Label("Building Distance:", labelStyle));
        satisfactionVarLabels.add(new Label("Campus Completion:", labelStyle));
        satisfactionVarLabels.add(new Label("Event Response:", labelStyle));
        satisfactionVarLabels.add(new Label("Building Capacity:", labelStyle));

        satisfactionCountLabels.add(new Label("000%", labelStyle));
        satisfactionCountLabels.add(new Label("000%", labelStyle));
        satisfactionCountLabels.add(new Label("000%", labelStyle));
        satisfactionCountLabels.add(new Label("000%", labelStyle));

        satisfactionScoreCaps = world.getSatisfaction().getSatisfactionScoreCaps();

        sideMenu.add(countDownLabel).expandX().center().colspan(2).row();
        sideMenu.add(timerLabel).expandX().center().colspan(2).row();
        sideMenu.add(new Label("\nCAMPUS SATISFACTION SUMMARY", labelStyle)).left().colspan(2).row();
        for (int i = 0; i < 4; i++) {
            satisfactionVarLabels.get(i).setFontScale(0.95f);
            satisfactionCountLabels.get(i).setFontScale(0.95f);
            sideMenu.add(satisfactionVarLabels.get(i)).left();
            sideMenu.add(satisfactionCountLabels.get(i)).right().row();
        }

        // Creates the table that contains the build and terrain selection
        Table selectableObjectTable = new Table().padBottom(20).padTop(10);
        sideMenu.add(selectableObjectTable).fillX().colspan(2).row();
        selectableObjectTable.add(new Label("\nBUILDINGS", labelStyle)).expandX().center().padBottom(10);
        selectableObjectTable.add(new Label("\nTERRAIN", labelStyle)).expandX().center().padBottom(10).row();
        selectableObjectTable.add(buildingUpButton).expandX().center().padBottom(30);
        selectableObjectTable.add(terrainUpButton).expandX().center().padBottom(30).row();
        selectableObjectTable.add(selectedBuildingLabel).expandX().center().padTop(50).uniform();
        selectableObjectTable.add(selectedTerrainLabel).expandX().center().padTop(50).uniform().row();
        selectableObjectTable.add(selectedBuildingCapacityLabel).expandX().center().uniform().row();
        selectableObjectTable.add(buildingDownButton).expandX().center().padTop(10);
        selectableObjectTable.add(terrainDownButton).expandX().center().padTop(10).row();

        sideMenu.add(demolishButton).colspan(2).expandX().row();
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

        parameter.size = (int) (0.045f * Gdx.graphics.getHeight());
        parameter.shadowColor = BLACK;
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = 2;
        headingFont = generator.generateFont(parameter);
        headingFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        parameter.size = (int) (0.030f * Gdx.graphics.getHeight());
        parameter.shadowColor = BLACK;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;
        bodyFont = generator.generateFont(parameter);
        bodyFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        parameter.size = (int) (0.04f * Gdx.graphics.getHeight());
        parameter.shadowColor = CLEAR;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;
        parameter.color = DARK_GRAY;
        parameter.borderColor = WHITE;
        parameter.borderWidth = 1;
        constructionFont = generator.generateFont(parameter);
        constructionFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        generator.dispose();
    }

    private void input() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) demolishMode = !demolishMode;

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            paused = !paused;
            // If the user pressed p to pause the game, rather than unpause it, then the pause count is incremented.
            if (paused) {
                world.getAchievementManager().incrementPauseCount();
            }
        }

        if (gameEnded) {
            objectToPlace = null;
            return;
        }

        touchPos.set(Gdx.input.getX(), Gdx.input.getY());
        unprojectedTouchPos.set(viewport.unproject(touchPos));

        isClicked = Gdx.input.isTouched();

        mapDemolitionCheck();
        selectableObjectsCheck();
    }

    /**
     * This method handles map demolition input checks
     */
    private void mapDemolitionCheck() {
        // Checks for MapObject demolition
        if (Gdx.input.justTouched() && unprojectedPosIsInsideWorld(unprojectedTouchPos) && demolishMode) {
            GridCoordTuple clickedGridSquare = getGridCoords(unprojectedTouchPos.x, unprojectedTouchPos.y);
            MapObject clickedObject = world.getMapObjectAt(clickedGridSquare);

            if (clickedObject != null) {
                demolishCooldownTimer = DEMOLISH_COOLDOWN;
                clickedObject.beginDemolition(world.getCurrentTime(), DEMOLITION_TIME);
            }
        }

        // Manages the demolish cooldown timer
        if (demolishCooldownTimer > 0f) {
            demolishCooldownTimer -= Gdx.graphics.getDeltaTime();
        }
        else {
            demolishCooldownTimer = 0f;
        }
    }

    /**
     * This method handles selectable objects and their placements input checks
     */
    private void selectableObjectsCheck() {
        if (Gdx.input.justTouched() && unprojectedPosIsInsideScreen(unprojectedTouchPos)) {
            // Initiates the dragging feature for when a menu building has been selected
            BuildingObject currentBuilding = selectableBuildings.get(selectableBuildingsIndex);
            TerrainObject currentTerrain = selectableTerrains.get(selectableTerrainsIndex);
            if (currentBuilding.contains(unprojectedTouchPos) && !demolishMode) {
                try {
                    objectToPlace = (BuildingObject) selectableBuildings.get(selectableBuildingsIndex).clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (currentTerrain.contains(unprojectedTouchPos) && !demolishMode) {
                try {
                    objectToPlace = (TerrainObject) selectableTerrains.get(selectableTerrainsIndex).clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if (!isClicked && objectToPlace != null && !demolishMode) { // Click released
            if (unprojectedTouchPos.x <= VIEWPORT_WIDTH - SIDE_PANEL_WIDTH) {
                world.addMapObject(objectToPlace); // Places the building if it passes all checks
            }

            objectToPlace = null;
        }

        if (objectToPlace != null && unprojectedPosIsInsideScreen(unprojectedTouchPos)) { // Track building to mouse position for drag
            objectToPlace.setCentre(touchPos.x, touchPos.y);
            objectToPlace.updateGridCoords();
        }
    }


    private void logic() {
        // Ends the game when the timer exceeds 5 minutes.
        gameEnded = world.getGameEnded();
        if (gameEnded) {
            ScoreManager.saveScore(uniName, world.getSatisfaction().getSatisfactionScore(), "scores.txt");
        }

        world.getAchievementManager().updateAchievements((int) world.getCurrentTime());

        if (paused || gameEnded) return;

        float delta = Gdx.graphics.getDeltaTime();

        world.process(delta);

        for (int i = 0; i < displayedActiveEventTimes.size; i++) {
            if (world.getCurrentTime() > displayedActiveEventTimes.get(i).y) {
                displayedActiveEventTimes.removeIndex(i);
                i--;
            }
        }

        stage.act(delta);
    }

    private void draw() {
        ScreenUtils.clear(OLIVE);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Collision detection for buildings already placed
        if (objectToPlace != null) {
            // If placing building, draw the grid
            if (isClicked) drawGrid(shapeRenderer);

            if (world.doesObjectOverlap(objectToPlace)) {
                shapeRenderer.begin(Filled);
            } else {
                shapeRenderer.begin(Line);
            }
            shapeRenderer.setColor(RED);
            Vector2 buildingCoords =  getGridSquareScreenCoords(objectToPlace.getGridCoords());
            shapeRenderer.rect(buildingCoords.x, buildingCoords.y, objectToPlace.width, objectToPlace.height);
            shapeRenderer.end();
        }

        // Draws side menu bar
        blockRenderer.begin(Filled);
        blockRenderer.setColor(DARK_GRAY);
        blockRenderer.rect(sideMenu.getX(), sideMenu.getY(), sideMenu.getWidth(), sideMenu.getHeight());
        blockRenderer.end();

        batch.begin();

        drawAssets(batch, assetManager);
        updateSideMenu();
        drawActiveEvents(batch);
        drawConstructionPercents();
        drawWorldUI();

        NotificationHandler.updateNotificationProcess(world.getCurrentTime());
        drawNotification(batch);

        batch.end();
        stage.draw();
    }

    /**
     * Renders icons on the left hand side of the screen to show what events are in effect. Draws from bottom to top,
     * bottom justified, so the most recent icon will be at the top a descending list
     */
    private void drawActiveEvents(SpriteBatch batch) {
        float bottomMargin = 35f;
        float leftMargin = 5f;
        float iconMargin = 5f;
        float screenIconSize = 80f;
        Sprite bgSprite = new Sprite(assetManager.get("ActiveEventBg.png", Texture.class));
        bgSprite.setSize(screenIconSize * 1.05f, screenIconSize * 1.05f);

        float iconYPos = bottomMargin + screenIconSize / 2f;
        for (Tuple<String, Integer> pair : displayedActiveEventTimes) {
            bgSprite.setCenter(leftMargin + screenIconSize / 2f, iconYPos);
            bgSprite.draw(batch);


            Sprite sprite = new Sprite(assetManager.get(pair.x, Texture.class));
            sprite.setSize(screenIconSize, screenIconSize);
            sprite.setCenter(leftMargin + screenIconSize / 2f, iconYPos);
            sprite.draw(batch);

            // Only show the timer if it will end within the game time
            if (pair.y < GAME_LENGTH_SECONDS) {
                constructionFont.draw(batch, (pair.y - MathUtils.round(world.getCurrentTime())) + "s", leftMargin + screenIconSize, iconYPos);
            }

            iconYPos += screenIconSize + iconMargin;
        }
    }

    private void drawAssets(Batch batch, AssetManager assetManager) {
        // Draws all placed buildings and terrain assets
        for (MapObject mapObject : world.getMapObjects()) {
            drawObject(batch, assetManager, mapObject);
        }

        // Snaps the dragged object to the grid and draws it, if selected
        if (objectToPlace != null && unprojectedTouchPos.x <= VIEWPORT_WIDTH - SIDE_PANEL_WIDTH) {
            drawObject(batch, assetManager, objectToPlace);
        }

        // Draws the currently displayed building and terrain assets, in the side menu
        drawSelectableObject(batch, assetManager, selectableBuildings.get(selectableBuildingsIndex));
        drawSelectableObject(batch, assetManager, selectableTerrains.get(selectableTerrainsIndex));
    }

    private void drawObject(Batch batch, AssetManager assetManager, MapObject mapObject) {
        Sprite sprite = new Sprite(assetManager.get(mapObject.spriteName, Texture.class));
        if (mapObject instanceof BuildingObject buildingObject && mapObject.placed) {
            if (!buildingObject.built) {
                sprite = new Sprite(assetManager.get(buildingObject.unbuiltSpriteName, Texture.class));
            }
        }
        if (mapObject.placed && mapObject.toBeDemolished) {
            sprite = new Sprite(assetManager.get("rubble.png", Texture.class));
        }

        Vector2 position = mapObject.getSnappedScreenPosition();
        sprite.setSize(mapObject.width, mapObject.height);
        sprite.setPosition(position.x, position.y);
        sprite.draw(batch);
    }

    /**
     * Unique drawing function required to draw buildings offset from the grid. Fewer checks are required for selectable
     * objects (e.g. they will never be under construction)
     */
    private void drawSelectableObject(Batch batch, AssetManager assetManager, MapObject mapObject) {
        Sprite sprite = new Sprite(assetManager.get(mapObject.spriteName, Texture.class));

        Vector2 position = mapObject.getUnsnappedScreenPos();
        sprite.setSize(mapObject.width, mapObject.height);
        sprite.setPosition(position.x, position.y);
        sprite.draw(batch);
    }

    /**
     * Draws a grid into the viewport using the {@link ShapeRenderer} passed to it.
     * @param gridRenderer The {@link ShapeRenderer} used to draw the grid.
     */
    private void drawGrid(ShapeRenderer gridRenderer) {
        gridRenderer.begin(Line);

        gridRenderer.setColor(new Color(0x5b7e13ff));

        float gridWidth = ( (VIEWPORT_WIDTH - SIDE_PANEL_WIDTH) / (float)GRID_WIDTH);
        float gridHeight = ( VIEWPORT_HEIGHT / (float)GRID_HEIGHT);

        for (int v = 1; v < 9; v++) {
            gridRenderer.line(0, gridHeight * v, VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, gridHeight * v);
        }

        for (int h = 1; h < 11; h++) {
            gridRenderer.line(gridWidth * h, 0, gridWidth * h, VIEWPORT_HEIGHT);
        }

        gridRenderer.end();
    }

    /**
     * Updates the relevant text on the side menu
     */
    private void updateSideMenu() {
        // Draws a 5-minute countdown timer for the games length
        // If it's a whole minute, it displays :00 for the seconds
        // Otherwise it gets the remainder of gameTimer divided by 60 for the seconds.
        float gameTime = world.getCurrentTime();
        timerLabel.setText(String.format("Year: %d, Day: %d", (int) (gameTime / 60) + 1, (int) ((gameTime % 60) / (60 / (double) 365)) + 1));

        if (60 - (int) gameTime % 60 == 60) {
            countDownLabel.setText(floorDiv((int)(GAME_LENGTH_SECONDS) - (int) gameTime, 60) + ":00");
        }
        else {
            countDownLabel.setText(floorDiv((int)(GAME_LENGTH_SECONDS) - (int) gameTime, 60) + ":" + String.format("%02d", 60 - (int) gameTime % 60));
        }

        // Update the satisfaction summary section
        float[] scores = world.getSatisfaction().getSatisfactionScoreBreakdown();
        for (int i = 0; i < scores.length; i++) {
            float score = (scores[i] / satisfactionScoreCaps[i]) * 100;
            satisfactionCountLabels.get(i).setText(String.format("%01d", (int) score) + "%");
        }

        // Changes the cursor and demolish button appearance
        if (!demolishMode) {
            if (Objects.equals(demolishButton.getColor().toString(), "ff0000ff")) { // Only sets the cursor when the mode has just changed
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            demolishButton.setText("Demolish Mode: OFF");
            demolishButton.setColor(DARK_GRAY);

            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }
        else {
            if (Objects.equals(demolishButton.getColor().toString(), "3f3f3fff")) { // Only sets the cursor when the mode has just changed
                Gdx.graphics.setCursor(Gdx.graphics.newCursor(demolishCursor, 0, 16));
            }

            demolishButton.setText("Demolish Mode: ON");
            demolishButton.setColor(RED);
        }

        // Changes the selected label text to whatever is currently selected
        selectedBuildingLabel.setText(selectableBuildings.get(selectableBuildingsIndex).objName);
        selectedTerrainLabel.setText(selectableTerrains.get(selectableTerrainsIndex).objName);

        // Updates the building capacity text
        BuildingObject selectedBuilding = selectableBuildings.get(selectableBuildingsIndex);
        StringBuilder capacityText = new StringBuilder();
        for (Use use : selectedBuilding.getUses()) { // Combines the various uses of the selected building, into 1 string
            capacityText.append(use.getStringShortName()).append(": ").append(selectedBuilding.getUseCapacity(use)).append(" students").append("\n");
        }
        if (selectedBuilding.getUses().length > 1) { // Removes the last "new line" of the string
            capacityText.delete(capacityText.length() - 1, capacityText.length());
        }
        selectedBuildingCapacityLabel.setText(capacityText);
        selectedBuildingCapacityLabel.setAlignment(2);
    }

    /**
     * Draws the notification, if it is to be displayed.
     * @param batch The {@link SpriteBatch} used to draw the sprites
     */
    private void drawNotification(SpriteBatch batch) {
        if (NotificationHandler.showNotif && world.getCurrentTime() < NotificationHandler.notifEndDisplayTime) {
            if (NotificationHandler.notifImage == null) {
                notificationBackground.setOriginBasedPosition(0, VIEWPORT_HEIGHT);
                notificationBackground.draw(batch);
                headingFont.draw(batch, NotificationHandler.notifTitle, 20, 525);
                bodyFont.draw(batch, NotificationHandler.notifText, 20, 490);
            }
            else {
                notificationBackground.setOriginBasedPosition(0, VIEWPORT_HEIGHT);
                notificationBackground.draw(batch);
                notificationImage.setOriginBasedPosition(10, VIEWPORT_HEIGHT - 10);
                notificationImage.draw(batch);
                headingFont.draw(batch, NotificationHandler.notifTitle, 110, 525);
                bodyFont.draw(batch, NotificationHandler.notifText, 110, 490);
            }
        }
    }

    /**
     * Draws all UI elements that appear in the world
     */
    private void drawWorldUI() {
        // Update the main satisfaction score
        headingFont.draw(batch, "Student Satisfaction: " + (int) world.getSatisfaction().getSatisfactionScore() + "%", 20, 40);

        // Displays object overlap text
        if (objectToPlace != null) {
            if (world.doesObjectOverlap(objectToPlace)) {
                bodyFont.draw(batch, "Something is already there...", 450, 30);
            }
        }

        if (paused) {
            headingFont.draw(batch, "TIMER PAUSED: press P to resume", 130, 280);
        }

        if (gameEnded) {
            headingFont.draw(batch, "End of the game!", 20, 460);
        }

        if (demolishMode) {
            batch.end();

            shapeRenderer.begin(Filled);
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
            shapeRenderer.rectLine(0, 4, VIEWPORT_WIDTH - SIDE_PANEL_WIDTH + 4, 4, 8, FIREBRICK, FIREBRICK);
            shapeRenderer.rectLine(VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, 4, VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, VIEWPORT_HEIGHT - 4, 8, FIREBRICK, FIREBRICK);
            shapeRenderer.rectLine(VIEWPORT_WIDTH - SIDE_PANEL_WIDTH + 4, VIEWPORT_HEIGHT - 4, 0, VIEWPORT_HEIGHT - 4, 8, FIREBRICK, FIREBRICK);
            shapeRenderer.rectLine(4, VIEWPORT_HEIGHT - 4, 4, 4, 8, FIREBRICK, FIREBRICK);
            shapeRenderer.end();

            batch.begin();
        }
    }

    private void showEventPopup(GameEvent event) {
        if (Objects.equals(event.iconName, "missingTexture.png"))
            NotificationHandler.displayNotification(event.title, event.description, world.getCurrentTime());
        else
            NotificationHandler.displayNotification(event.title, event.description, assetManager.get(event.iconName, Texture.class), world.getCurrentTime());

        // This is always called AFTER the event is handled by world. Therefore, we can check activeEvents to find out
        // if the new event is an active one
        if (world.hasActiveEvent(event)) {
            displayedActiveEventTimes.add(new Tuple<>(event.iconName, MathUtils.round(world.getEndTimeOfActiveEvent(event))));
        }

        paused = true;
    }

    private void showAchievementPopup(Achievement achievement) {
        NotificationHandler.displayNotification(achievement.getTitle(), achievement.getDescription(), assetManager.get("Achievement.png", Texture.class), world.getCurrentTime());
    }

    private void drawConstructionPercents() {
        for (BuildingObject building : world.getBuildings(false)) {
            if (!building.toBeDemolished && building.getConstructionDuration() < GAME_LENGTH_SECONDS) {
                Vector2 buildingPos = getGridSquareScreenCoords(building.getGridCoords());
                constructionFont.draw(batch, String.format("%02d", (int) building.getConstructionPercent(world.getCurrentTime())) + "%", buildingPos.x + 8, buildingPos.y + 40);
            }
        }
    }

    /**
     * @return True if the given (unprojected) position is within the bounds of the screen (including the UI)
     */
    private boolean unprojectedPosIsInsideScreen(Vector2 position) {
        return position.x < VIEWPORT_WIDTH && position.y < VIEWPORT_HEIGHT && position.x > 0 && position.y > 0;
    }

    /**
     * @return True if the given (unprojected) position is within the bounds of the world
     */
    private boolean unprojectedPosIsInsideWorld(Vector2 position) {
        return position.x < VIEWPORT_WIDTH - SIDE_PANEL_WIDTH && position.y < VIEWPORT_HEIGHT && position.x > 0 && position.y > 0;
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        blockRenderer.dispose();
        gridRenderer.dispose();
        batch.dispose();
        headingFont.dispose();
        bodyFont.dispose();
        constructionFont.dispose();
        assetManager.dispose();
        demolishCursor.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
