package com.remulasce.sandcastle.input;

import android.graphics.Rect;
import android.opengl.Matrix;

public class Joystick {
	private static final boolean NORMALIZE = false;	//constrain input to a circle 
	public static final float DEAD_ZONE	= .08f;		//Lowest possible d value, save 0.
	public static final int BACK_SIZE	= 200;
	public static final int TOP_SIZE	= 60;
	
	private Rect	inputArea;	//The area, in pixels, this occupies
	public float	dx, dy;		//The position of the stick, [-1,1]. + is up, right.
	
	private boolean	holding = false;	//Whether the joy is currently touched
	private boolean enabled	= true;
	
	private TouchPoint	curTouch;		//The currently tracking touchpoint
	
	
	public Joystick() {
		inputArea = new Rect();
	}
	
	public void handleInput(InputEngine input) {
		//If there's a new touch and we don't alread have one, start tracking it as our touch.
		if ( !holding && input.newTouch && enabled) {
			//See if we can find a new touch that is in our bonuds
			for (TouchPoint each : input.getNewTouches()) {
				if (inputArea.contains((int)each.x(), (int)each.y())) {
					holding  = true;
					curTouch = each;
				}
			}
		}
		
		//If our following touch was lifted, we should reset.
		if (holding && !curTouch.isPressed()) {
			holding = false;
			dx=0;dy=0;
		}
		
		//If holding, update the top & displacement
		if (holding) {
			//Update normalized displacement
			Vector2 pos  = curTouch.getPos();
			
			dx = (pos.x-inputArea.centerX())/(inputArea.width()/2);
			dy = -(pos.y-inputArea.centerY())/(inputArea.height()/2);
			//Keep displacement within 1 for a circle
			if (NORMALIZE && dx*dx+dy*dy > 1) {
				float len = (float) Math.sqrt(dx*dx+dy*dy);
				dx /= len;
				dy /= len;
			}
			if (!NORMALIZE) {
				if (dx < -1)	{ dx = -1;	}
				if (dx > 1)		{ dx = 1;	}
				if (dy < -1)	{ dy = -1;	}
				if (dy > 1)		{ dy = 1;	}
			}
			//Dead zone
			if (Math.abs(dx) < DEAD_ZONE) {	dx = 0;	}
			if (Math.abs(dy) < DEAD_ZONE) { dy = 0;	}
			
			//If we are being used, there's definitely no gestures going on.
			input.handleAllGestures();
		}
	}
	public boolean isHolding() {
		return this.holding;
	}
	public void setPos(int x, int y) {
		inputArea.set(x, y, x+BACK_SIZE, y+BACK_SIZE);
	}
	public void setTransformBack(float[] mMatrix) {
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0, getBackX(), getBackY(), 0);
	}
	public void setTransformTop(float[] mMatrix) {
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0,
				getTopX(), getTopY(),
				0);
	}
	
	
	//Get methods for whatever's drawing us
	/** Return the Y coordinate of the center of the joystick's background */
	public float getBackY() {
		return inputArea.centerY();
	}
	public float getBackX() {
		return inputArea.centerX();
	}
	
	/** Returns the Y coordinate of the center of the movable 'top' of the joystick.
	 * Not the top of the background, but the bit that sohuld move with user.
	 */
	public float getTopY() {
		return inputArea.centerY() - inputArea.height()/2 * dy;
	}
	public float getTopX() {
		return inputArea.centerX() + inputArea.width()/2 * dx;
	}
	
	
}
