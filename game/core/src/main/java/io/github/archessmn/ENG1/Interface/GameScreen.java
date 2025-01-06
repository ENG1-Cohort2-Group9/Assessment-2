package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
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

import java.util.Objects;

import static java.lang.Math.floorDiv;

public class GameScreen implements Screen {
    public static final int VIEWPORT_WIDTH = 960;
    public static final int VIEWPORT_HEIGHT = 540;
    private static final int SIDE_PANEL_WIDTH = 300;
    private static final float EVENT_NOTIFICATION_TIME = 5f; // How long event notifications are shown before disappearing
    private static final float DEMOLISH_COOLDOWN = 0.25f;

    private World world;

    private static AssetManager assetManager;

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
    private Table rootTable;
    private Table sideMenu;
    private Label countDownLabel;
    private Label timerLabel;
    private Label selectedBuildingLabel;
    private Label selectedTerrainLabel;
    private Label selectedBuildingCapacityLabel;
    private TextButton demolishButton;
    private final Array<Label> satisfactionCountLabels = new Array<>();
    private final Array<Label> satisfactionVarLabels = new Array<>();

    private static Sprite notificationBackground;
    private static Sprite notificationImage;

    private float timeEventShownAt = -10f;
    private GameEvent currentEvent = null;
    private Array<Tuple<String, Integer>> displayedActiveEventTimes = new Array<>();

    private float[] satisfactionScoreCaps;

    private boolean demolishMode = false;
    private float demolishCooldownTimer = 0f;

    private final ScreenManager game;

    private String uniName = "Guest";




    public GameScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        world = new World(VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, VIEWPORT_HEIGHT, new GameEventListener[] {new GameEventListener(this::showEventPopup)});

        atlas = new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas"));
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        skin.addRegions(atlas);

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        selectableBuildings = new Array<>();
        selectableBuildings.add(new BuildingObject(710, 160, 0, BuildingName.HALLS));
        selectableBuildings.add(new BuildingObject(710, 160, 0, BuildingName.GYM));
        selectableBuildings.add(new BuildingObject(710, 160, 0, BuildingName.LECTURE_HALL));
        selectableBuildings.add(new BuildingObject(710, 160, 0, BuildingName.PIAZZA));
        selectableBuildings.add(new BuildingObject(710, 160, 0, BuildingName.PUB));
        selectableTerrains = new Array<>();
        selectableTerrains.add(new TerrainObject(849, 160, TerrainObject.Feature.LAKE));
        selectableTerrains.add(new TerrainObject(849, 160, TerrainObject.Feature.ROCK));
        selectableTerrains.add(new TerrainObject(849, 160, TerrainObject.Feature.TREE));

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

        assetManager.load("ui/notification_background.png", Texture.class);

        assetManager.finishLoading();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        sideMenu = new Table();
        sideMenu.pad(10);
        rootTable.add(sideMenu).expandY().fillY().width(SIDE_PANEL_WIDTH);
        rootTable.right();
        initSideMenu();

        NotificationHandler.initNotification();

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
        demolishButton.setColor(Color.DARK_GRAY);
        demolishButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                demolishMode = !demolishMode;
            }
        });

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

        satisfactionScoreCaps = world.satisfaction.getSatisfactionScoreCap();

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
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/Arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = (int) (0.045f * Gdx.graphics.getHeight());
        parameter.shadowColor = Color.BLACK;
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = 2;
        headingFont = generator.generateFont(parameter);
        headingFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        parameter.size = (int) (0.030f * Gdx.graphics.getHeight());
        parameter.shadowColor = Color.BLACK;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;
        bodyFont = generator.generateFont(parameter);
        bodyFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        parameter.size = (int) (0.04f * Gdx.graphics.getHeight());
        parameter.shadowColor = Color.CLEAR;
        parameter.shadowOffsetX = 0;
        parameter.shadowOffsetY = 0;
        parameter.color = Color.DARK_GRAY;
        parameter.borderColor = Color.WHITE;
        parameter.borderWidth = 1;
        constructionFont = generator.generateFont(parameter);
        constructionFont.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight());

        generator.dispose();
    }

    private void input() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) paused = !paused;
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) demolishMode = !demolishMode;

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
        if (Gdx.input.justTouched() && unprojectedPosIsInsideScreen(unprojectedTouchPos) && demolishMode) {
            GridCoordTuple clickedGridSquare = GridUtils.getGridCoords(unprojectedTouchPos.x, unprojectedTouchPos.y);
            MapObject clickedObject = world.getMapObjectAt(clickedGridSquare);

            if (clickedObject != null) {
                demolishCooldownTimer = DEMOLISH_COOLDOWN;
                clickedObject.beginDemolition(world.getCurrentTime(), world.DEMOLITION_TIME);
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
            world.saveScore(uniName, world.getSatisfaction().getSatisfactionScore(), "scores.txt");
        }

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
        ScreenUtils.clear(Color.OLIVE);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Collision detection for buildings already placed
        if (objectToPlace != null) {
            // If placing building, draw the grid
            if (isClicked) drawGrid(shapeRenderer);

            if (world.doesObjectOverlap(objectToPlace)) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            } else {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            }
            shapeRenderer.setColor(Color.RED);
            Vector2 buildingCoords =  GridUtils.getGridSquareScreenCoords(objectToPlace.getGridCoords());
            shapeRenderer.rect(buildingCoords.x, buildingCoords.y, objectToPlace.width, objectToPlace.height);
            shapeRenderer.end();
        }

        // Draws side menu bar
        blockRenderer.begin(ShapeRenderer.ShapeType.Filled);
        blockRenderer.setColor(Color.DARK_GRAY);
        blockRenderer.rect(sideMenu.getX(), sideMenu.getY(), sideMenu.getWidth(), sideMenu.getHeight());
        blockRenderer.end();

        batch.begin();

        drawAssets(batch, assetManager);
        updateSideMenu();
        drawActiveEvents(batch);
        drawConstructionPercents();
        drawWorldUI();

        NotificationHandler.checkNotificationTTL(world.getCurrentTime());
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
            if (pair.y < World.GAME_LENGTH_SECONDS) {
                bodyFont.draw(batch, (pair.y - MathUtils.round(world.getCurrentTime())) + "s", leftMargin + screenIconSize, iconYPos);
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
        gridRenderer.begin(ShapeRenderer.ShapeType.Line);

        gridRenderer.setColor(new Color(0x5b7e13ff));

        float gridWidth = ( VIEWPORT_WIDTH / (float)GridUtils.GRID_WIDTH);
        float gridHeight = ( VIEWPORT_HEIGHT / (float)GridUtils.GRID_HEIGHT);

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
            countDownLabel.setText(floorDiv((int)(World.GAME_LENGTH_SECONDS) - (int) gameTime, 60) + ":00");
        }
        else {
            countDownLabel.setText(floorDiv((int)(World.GAME_LENGTH_SECONDS) - (int) gameTime, 60) + ":" + String.format("%02d", 60 - (int) gameTime % 60));
        }

        // Update the satisfaction summary section
        float[] scores = world.satisfaction.getSatisfactionScoreBreakdown();
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
            demolishButton.setColor(Color.DARK_GRAY);

            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }
        else {
            if (Objects.equals(demolishButton.getColor().toString(), "3f3f3fff")) { // Only sets the cursor when the mode has just changed
                Pixmap demolishCursor = new Pixmap(Gdx.files.internal("ui/demolish_cursor.png"));
                Gdx.graphics.setCursor(Gdx.graphics.newCursor(demolishCursor, 0, 16));
                demolishCursor.dispose();
            }

            demolishButton.setText("Demolish Mode: ON");
            demolishButton.setColor(Color.RED);
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
            if (Objects.equals(NotificationHandler.notifImagePath, "")) {
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
        headingFont.draw(batch, "Student Satisfaction: " + (int) world.satisfaction.getSatisfactionScore() + "%", 20, 40);

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

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
            shapeRenderer.rectLine(0, 4, VIEWPORT_WIDTH - SIDE_PANEL_WIDTH + 4, 4, 8, Color.FIREBRICK, Color.FIREBRICK);
            shapeRenderer.rectLine(VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, 4, VIEWPORT_WIDTH - SIDE_PANEL_WIDTH, VIEWPORT_HEIGHT - 4, 8, Color.FIREBRICK, Color.FIREBRICK);
            shapeRenderer.rectLine(VIEWPORT_WIDTH - SIDE_PANEL_WIDTH + 4, VIEWPORT_HEIGHT - 4, 0, VIEWPORT_HEIGHT - 4, 8, Color.FIREBRICK, Color.FIREBRICK);
            shapeRenderer.rectLine(4, VIEWPORT_HEIGHT - 4, 4, 4, 8, Color.FIREBRICK, Color.FIREBRICK);
            shapeRenderer.end();

            batch.begin();
        }

        // Show event text (if there is one)
        if (currentEvent != null && world.getCurrentTime() < timeEventShownAt + EVENT_NOTIFICATION_TIME) {
            headingFont.draw(batch, currentEvent.title, 20, 525);
            headingFont.draw(batch, currentEvent.description, 20, 495);
        }
    }

    private void showEventPopup(GameEvent event) {
        currentEvent = event;
        timeEventShownAt = world.getCurrentTime();
        // This is always called AFTER the event is handled by world. Therefore, we can check activeEvents to find out
        // if the new event is an active one
        if (world.hasActiveEvent(event)) {
            displayedActiveEventTimes.add(new Tuple<>(event.iconName, MathUtils.round(world.getEndTimeOfActiveEvent(event))));
        }

        paused = true;
    }

    private void drawConstructionPercents() {
        for (BuildingObject building : world.getBuildings()) {
            if (!building.isBuilt() && !building.toBeDemolished) {
                Vector2 buildingPos = GridUtils.getGridSquareScreenCoords(building.getGridCoords());
                constructionFont.draw(batch, String.format("%02d", (int) building.getConstructionPercent(world)) + "%", buildingPos.x + 8, buildingPos.y + 40);
            }
        }
    }

    /**
     * @return True if the given (unprojected) position is within the bounds of the screen (including the UI)
     */
    private boolean unprojectedPosIsInsideScreen(Vector2 position) {
        return position.x < VIEWPORT_WIDTH && position.y < VIEWPORT_HEIGHT && position.x > 0 && position.y > 0;
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        gridRenderer.dispose();
        batch.dispose();
        headingFont.dispose();
        bodyFont.dispose();
        constructionFont.dispose();
        assetManager.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    /**
     * This class manages the notification holder that can be displayed. The notification can have text or both text
     * and an image displayed, and this notification is displayed within a set game time.
     */
    public static class NotificationHandler {
        public static boolean showNotif = false;
        public static float notifEndDisplayTime;

        public static final int NOTIFICATION_DISPLAY_TIME = 10; // The in-game time that the notification will be displayed for

        public static String notifTitle = "";
        public static String notifText = "";
        public static String notifImagePath = "";

        /**
         * This creates the background image for the notification. It must be called first before any other method
         * can be.
         */
        public static void initNotification() {
            notificationBackground = new Sprite(assetManager.get("ui/notification_background.png", Texture.class));
            notificationBackground.setOrigin(0, 100);
        }

        /**
         * A new notification is displayed with the given title and text, for the set duration.
         * @param title The header title for the notification (max. 30 characters)
         * @param text The body text for the notification (max. 112 characters)
         * @param currentTime The time that the notification will be start the countdown from
         */
        public static void displayNotification(String title, String text, float currentTime) {
            notifTitle = title;
            notifText = text;
            notifImagePath = "";

            // Validation length checks
            if (notifText.length() > 112) {
                throw new IllegalArgumentException("Notification text given is more than 112 characters");
            }
            else if (notifTitle.length() > 30) {
                throw new IllegalArgumentException("Notification title given is more than 30 characters");
            }

            // Responsible for ensuring the text wraps correctly
            if (notifText.length() > 56) {
                int firstLineEndIndex = notifText.substring(0, 56).lastIndexOf(" ");
                StringBuilder stringBuilder = new StringBuilder(notifText);
                if (firstLineEndIndex == -1) {
                    stringBuilder.insert(56, "\n");
                    stringBuilder.replace(57, 58, "");
                }
                else {
                    stringBuilder.insert(firstLineEndIndex, "\n");
                    stringBuilder.replace(firstLineEndIndex + 1, firstLineEndIndex + 2, "");
                }
                notifText = stringBuilder.toString();
            }

            showNotif = true;
            notifEndDisplayTime = currentTime + NOTIFICATION_DISPLAY_TIME;
        }

        /**
         * A new notification is displayed with the given title, text and icon, for the set duration.
         * @param title The header title for the notification (max. 20 characters)
         * @param text The body text for the notification (max. 70 characters)
         * @param imagePath The image path for the notification icon
         * @param currentTime The time that the notification will be start the countdown from
         */
        public static void displayNotification(String title, String text, String imagePath, float currentTime) {
            notifTitle = title;
            notifText = text;
            notifImagePath = imagePath;

            notificationImage = new Sprite(assetManager.get(notifImagePath, Texture.class));
            notificationImage.setOrigin(0, 80);
            notificationImage.setSize(80, 80);

            // Validation length checks
            if (notifText.length() > 70) {
                throw new IllegalArgumentException("Notification text given is more than 70 characters");
            }
            else if (notifTitle.length() > 20) {
                throw new IllegalArgumentException("Notification title given is more than 20 characters");
            }

            // Responsible for ensuring the text wraps correctly
            if (notifText.length() > 35) {
                int firstLineEndIndex = notifText.substring(0, 35).lastIndexOf(" ");
                StringBuilder stringBuilder = new StringBuilder(notifText);
                if (firstLineEndIndex == -1) {
                    stringBuilder.insert(35, "\n");
                    stringBuilder.replace(36, 37, "");
                }
                else {
                    stringBuilder.insert(firstLineEndIndex, "\n");
                    stringBuilder.replace(firstLineEndIndex + 1, firstLineEndIndex + 2, "");
                }
                notifText = stringBuilder.toString();
            }

            showNotif = true;
            notifEndDisplayTime = currentTime + NOTIFICATION_DISPLAY_TIME;
        }

        /**
         * Checks to see if the current notification has passed it's TTL and thus should be hidden.
         * @param currentTime The current time
         */
        public static void checkNotificationTTL(float currentTime) {
            if (showNotif && currentTime > notifEndDisplayTime) {
                notifTitle = "";
                notifText = "";
                notifImagePath = "";

                showNotif = false;
                notifEndDisplayTime = 0;
            }
        }
    }
}
