package com.remulasce.sandcastle.input;

public interface InputProcessor {

	boolean touchDown(int x, int y, int pointer, int button);

	boolean touchDragged(int x, int y, int pointer);

	boolean touchUp(int x, int y, int pointer, int button);

	boolean touchMoved(int arg0, int arg1);

	boolean keyDown(int arg0);

	boolean keyTyped(char arg0);

	boolean keyUp(int arg0);

	boolean scrolled(int arg0);

}
