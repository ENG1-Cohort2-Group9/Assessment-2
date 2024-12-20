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
    Boolean isClicked = false;

    Rectangle buildingRectangle;
    BitmapFont font;

    MapObject objectToPlace = null;
    Array<BuildingObject> selectableBuildings = new Array<>();
    Array<TerrainObject> selectableTerrains = new Array<>();
    int selectableBuildingsIndex = 0;
    int selectableTerrainsIndex = 0;

    MapObject highlightedTile = null;
    float highlightTimer = 5f;

    Boolean paused = true;
    Boolean gameEnded = false;

    private Stage stage;
    private Table sideMenu;
    private Label countDownLabel;
    private Label timerLabel;
    private Label highlightedBuildingLabel;
    private Label selectedBuildingLabel;
    private Label selectedTerrainLabel;
    private TextButton actionButton;
    private final Array<Label> satisfactionCountLabel = new Array<>();
    private final Array<Label> satisfactionVarLabel = new Array<>();
    private float timeEventShownAt = -10f;
    private GameEvent currentEvent = null;

    float[] satisfactionScoreCaps;

    final ScreenManager game;
    final float EVENT_NOTIFICATION_TIME = 5f; // How long event notifications are shown before disappearing

    String uniName = "Guest";



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

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        selectableBuildings = new Array<>();
        selectableBuildings.add(new HallsBuilding(710, 90, 0, true));
        selectableBuildings.add(new GymBuilding(710, 90, 0, true));
        selectableBuildings.add(new LectureHallBuilding(710, 90, 0, true));
        selectableBuildings.add(new PiazzaBuilding(710, 90, 0, true));
        selectableBuildings.add(new Pub(710, 90, 0, true));
        selectableTerrains = new Array<>();
        selectableTerrains.add(new TerrainObject(849, 90, TerrainObject.Feature.LAKE));
        selectableTerrains.add(new TerrainObject(849, 90, TerrainObject.Feature.ROCK));
        selectableTerrains.add(new TerrainObject(849, 90, TerrainObject.Feature.TREE));
        setupSideMenu();

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

        blockRenderer = new ShapeRenderer();

        buildingRectangle = new Rectangle();
    }

    private void setupSideMenu() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label.LabelStyle labelStyle = skin.get(Label.LabelStyle.class);
        TextButtonStyle textButtonStyle = skin.get(TextButtonStyle.class);

        countDownLabel = new Label("Timer", labelStyle);
        timerLabel = new Label("Timer", labelStyle);
        highlightedBuildingLabel = new Label("Building", labelStyle);

        selectedBuildingLabel = new Label("{Building}", labelStyle);
        selectedTerrainLabel = new Label("{Terrain}", labelStyle);

        actionButton = new TextButton("{Action}", textButtonStyle);
        actionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (highlightedTile.isBuilding) {
                    world.demolishBuilding((BuildingObject) highlightedTile);
                }
                else {
                    world.destroyTerrain((TerrainObject) highlightedTile);
                }
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


        satisfactionVarLabel.add(new Label("Building Distance:", labelStyle));
        satisfactionVarLabel.add(new Label("Campus Completion:", labelStyle));
        satisfactionVarLabel.add(new Label("Event Response:", labelStyle));
        satisfactionVarLabel.add(new Label("Building Diversity:", labelStyle));

        satisfactionCountLabel.add(new Label("000%", labelStyle));
        satisfactionCountLabel.add(new Label("000%", labelStyle));
        satisfactionCountLabel.add(new Label("000%", labelStyle));
        satisfactionCountLabel.add(new Label("000%", labelStyle));

        satisfactionScoreCaps = world.satisfaction.getSatisfactionScoreCap();

        sideMenu = new Table();
        sideMenu.pad(10);
        rootTable.right().add(sideMenu).expandY().fillY().width(300);

        sideMenu.add(countDownLabel).expandX().center().colspan(2).row();
        sideMenu.add(timerLabel).expandX().center().colspan(2).row();
        sideMenu.add(new Label("\nCAMPUS SATISFACTION SUMMARY", labelStyle)).left().colspan(2).row();
        for (int i = 0; i < 4; i++) {
            sideMenu.add(satisfactionVarLabel.get(i)).left();
            sideMenu.add(satisfactionCountLabel.get(i)).right().row();
        }
        sideMenu.add(new Label("\nSELECTED TILE", labelStyle)).left().colspan(2).row();
        sideMenu.add(highlightedBuildingLabel).left().colspan(2).row();
        sideMenu.add(actionButton).left().colspan(2).row();

        // Creates the table that contains the build and terrain selection
        Table mapObjectTable = new Table().padBottom(20);
        sideMenu.add(mapObjectTable).fillX().colspan(2);
        mapObjectTable.add(new Label("\nBUILDINGS", labelStyle)).expandX().center().padBottom(10);
        mapObjectTable.add(new Label("\nTERRAIN", labelStyle)).expandX().center().padBottom(10).row();
        mapObjectTable.add(buildingUpButton).expandX().center().padBottom(40);
        mapObjectTable.add(terrainUpButton).expandX().center().padBottom(40).row();
        mapObjectTable.add(selectedBuildingLabel).expandX().center().padTop(40).uniform();
        mapObjectTable.add(selectedTerrainLabel).expandX().center().padTop(40).uniform().row();
        mapObjectTable.add(buildingDownButton).expandX().center().padTop(10);
        mapObjectTable.add(terrainDownButton).expandX().center().padTop(10).row();
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
            objectToPlace = null;
            return;
        }

        touchPos.set(Gdx.input.getX(), Gdx.input.getY());
        unprojectedTouchPos.set(viewport.unproject(touchPos));

        isClicked = Gdx.input.isTouched();

        // A check to see if any building/terrain assets have been clicked
        if (Gdx.input.justTouched()) {
            // Loops through all placed map objects to see if they have been clicked
            for (MapObject mapObject : world.mapObjects) {
                if (mapObject.getBounds().contains(unprojectedTouchPos)) {
                    highlightedTile = mapObject;
                    highlightTimer = 5f;
                    break;
                }
            }

            // Initiates the dragging feature for when a menu building has been selected
            BuildingObject currentBuilding = selectableBuildings.get(selectableBuildingsIndex);
            TerrainObject currentTerrain = selectableTerrains.get(selectableTerrainsIndex);
            if (currentBuilding.getBounds().contains(unprojectedTouchPos)) {
                objectToPlace = selectableBuildings.get(selectableBuildingsIndex).makeCopy();
            }
            else if (currentTerrain.getBounds().contains(unprojectedTouchPos)) {
                objectToPlace = selectableTerrains.get(selectableTerrainsIndex).makeCopy();
            }
        }
        else if (!isClicked && objectToPlace != null) { // Click released
            if (unprojectedTouchPos.x <= VIEWPORT_WIDTH - 300) {
                world.addMapObject(objectToPlace); // Places the building if it passes all checks
            }

            objectToPlace = null;
        }

        if (objectToPlace != null) { // Track building to mouse position for drag
            objectToPlace.setCenter(touchPos.x, touchPos.y);
        }
    }

    private void logic() {
        if (paused || gameEnded) return;

        float delta = Gdx.graphics.getDeltaTime();

        world.worldProcess(delta);

        // Ends the game when the timer exceeds 5 minutes.
        gameEnded = world.getGameEnded();
        if (gameEnded) {
            world.saveScore(uniName, world.getSatisfaction().getSatisfactionScore(), "scores.txt");
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
            Vector2 buildingCoords = objectToPlace.getRawGridCoords();
            shapeRenderer.rect(buildingCoords.x - (objectToPlace.width / 2), buildingCoords.y - (objectToPlace.height / 2), objectToPlace.width, objectToPlace.height);
            shapeRenderer.end();
        }

        // Draws side menu bar
        blockRenderer.begin(ShapeRenderer.ShapeType.Filled);
        blockRenderer.setColor(Color.DARK_GRAY);
        blockRenderer.rect(sideMenu.getX(), sideMenu.getY(), sideMenu.getWidth(), sideMenu.getHeight());
        blockRenderer.end();

        batch.begin();

        drawAssets(batch, assetManager);
        drawSideMenu();

        batch.end();
        stage.draw();
    }

    public void drawAssets(Batch batch, AssetManager assetManager) {
        // Draws all placed buildings and terrain assets
        for (BuildingObject building : world.buildings) {
            drawObject(batch, assetManager, building);
        }
        for (TerrainObject terrain : world.terrain) {
            drawObject(batch, assetManager, terrain);
        }

        // Snaps the dragged object to the grid and draws it, if selected
        if (objectToPlace != null && unprojectedTouchPos.x <= VIEWPORT_WIDTH - 300) {
            objectToPlace.snapToGrid();
            drawObject(batch, assetManager, objectToPlace);
        }

        // Draws the currently displayed building and terrain assets, in the side menu
        drawObject(batch, assetManager, selectableBuildings.get(selectableBuildingsIndex));
        drawObject(batch, assetManager, selectableTerrains.get(selectableTerrainsIndex));
    }

    private void drawObject(Batch batch, AssetManager assetManager, MapObject mapObject) {
        Sprite sprite = new Sprite(assetManager.get(mapObject.spriteName, Texture.class));
        if (mapObject.isBuilding && mapObject.placed) {
            BuildingObject buildingObject = (BuildingObject) mapObject;
            if (!buildingObject.built) {
                sprite = new Sprite(assetManager.get(buildingObject.unbuiltSpriteName, Texture.class));
            }
        }

        sprite.setSize(mapObject.width, mapObject.height);
        sprite.setPosition(mapObject.x, mapObject.y);
        sprite.draw(batch);

        Sprite efficiencySprite = null;
        // Draw '+' or '-' if building efficiency is not the default value, 0.5
        if (mapObject.getEfficiency() > 0.5f) {
            efficiencySprite = new Sprite(assetManager.get("plus.png", Texture.class));
        }
        else if (mapObject.getEfficiency() < 0.5f) {
            efficiencySprite = new Sprite(assetManager.get("minus.png", Texture.class));
        }
        else {
            return;
        }

        efficiencySprite.setSize(mapObject.width * 0.25f, mapObject.height * 0.25f);
        efficiencySprite.setPosition(mapObject.x + mapObject.width * 0.75f, mapObject.y + mapObject.height * 0.75f);
        efficiencySprite.draw(batch);
    }

    /**
     * Draws a grid into the viewport using the {@link ShapeRenderer} passed to it.
     * @param gridRenderer The {@link ShapeRenderer} used to draw the grid.
     */
    public void drawGrid(ShapeRenderer gridRenderer) {
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
        timerLabel.setText(String.format("Year: %d, Day: %d", (int) (gameTime / 60) + 1, (int) ((gameTime % 60) / (60 / (double) 365)) + 1));

        if (60 - (int) gameTime % 60 == 60) {
            countDownLabel.setText(floorDiv(300 - (int) gameTime, 60) + ":00");
        }
        else {
            countDownLabel.setText(floorDiv(300 - (int) gameTime, 60) + ":" + String.format("%02d", 60 - (int) gameTime % 60));
        }

        // Update the satisfaction summary section
        float[] scores = world.satisfaction.getSatisfactionScoreBreakdown();
        for (int i = 0; i < scores.length; i++) {
            float score = (scores[i] / satisfactionScoreCaps[i]) * 100;
            satisfactionCountLabel.get(i).setText(String.format("%02d", (int) score) + "%");
        }

        // Update the main satisfaction score
        font.draw(batch, "Student Satisfaction: " + (int) world.satisfaction.getSatisfactionScore() + "%", 10, 30);

        // Changes the selected label text to whatever is currently selected
        selectedBuildingLabel.setText(selectableBuildings.get(selectableBuildingsIndex).objName);
        selectedTerrainLabel.setText(selectableTerrains.get(selectableTerrainsIndex).objName);

        // Displays options in the menu, whenever a tile is highlighted
        if (highlightedTile != null && highlightTimer <= 5f) {
            highlightTimer -= Gdx.graphics.getDeltaTime();

            highlightedBuildingLabel.setVisible(true);
            actionButton.setVisible(true);

            highlightedBuildingLabel.setText(highlightedTile.objName);
            if (highlightedTile.isBuilding) {
                actionButton.setText("Demolish");
            }
            else {
                actionButton.setText("Destroy");
            }
        }
        else { // Hides the options when the tile is no longer highlighted after 5 seconds
            highlightedBuildingLabel.setVisible(false);
            actionButton.setVisible(false);
        }

        // Controls the highlight timer (5 seconds)
        if (highlightTimer <= 0f) {
            highlightTimer = 5f;
            highlightedTile = null;
        }

        // Displays building overlap text
        if (objectToPlace != null) {
            if (world.doesObjectOverlap(objectToPlace)) {
                font.draw(batch, "Buildings Overlap", 20, 520);
            }
        }

        // Show event text (if there is one)
        if (currentEvent != null && world.getCurrentTime() < timeEventShownAt + EVENT_NOTIFICATION_TIME) {
            font.draw(batch, currentEvent.title, 20, 525);
            font.draw(batch, currentEvent.description, 20, 495);
        }

        if (paused) {
            font.draw(batch, "PAUSED: press P to resume", 20, 460);
        }

        if (gameEnded) {
            font.draw(batch, "End of the game!", 20, 460);
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
