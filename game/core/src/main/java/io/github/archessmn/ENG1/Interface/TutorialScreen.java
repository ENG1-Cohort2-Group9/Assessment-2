package io.github.archessmn.ENG1.Interface;

import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;


public class TutorialScreen implements Screen {
    public static final Integer VIEWPORT_WIDTH = 960;
    public static final Integer VIEWPORT_HEIGHT = 540;

    FitViewport viewport;
    private Stage stage;
    final ScreenManager game;



    public TutorialScreen(ScreenManager main) {
        this.game = main;
    }

    @Override
    public void show() {

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

    }

    private void logic() {

    }

    private void draw() {

    }

    @Override
    public void dispose() {
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
