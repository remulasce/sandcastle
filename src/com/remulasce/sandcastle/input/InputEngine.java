package com.remulasce.sandcastle.input;

import java.util.ArrayList;

import com.remulasce.sandcastle.C;

/**
 * 
 * @author Remulasce
 *
 *	Use: 
 *	In game loop construction, create an InputEngine and set it as the app's InputProcessor
 *
 */
public class InputEngine implements InputProcessor {
	
	private static final boolean CLASS_DEBUG	= false;
	private static final String  CLASS_TAG		= "InGn";
	
	/** No gestures happening **/
	public static final int G_NOTHING	= 1; 
	/** One or more touchpoints were just moved **/
	public static final int G_DRAGGING	= 2; 
	/** Two or more touchpoints just moved **/
	public static final int G_PINCHING	= 4;
	/** A tap was just completed, it was the only touchpoint onscreen **/
	public static final int G_TAPPING	= 8;
	/** A single touchpoint is onscreen, may or may not have just moved **/
	public static final int G_HOLDING	= 16;
	
	private ArrayList<TouchPoint> touchPoints	= new ArrayList<TouchPoint>();
	private ArrayList<TouchPoint> newTouches	= new ArrayList<TouchPoint>(); 	//any newly-placed touches
	//A list of just-lifted touchpoints which were taps. We remove tap-touchpoints a frame after release, so the hold
	private ArrayList<TouchPoint> tapEnds		= new ArrayList<TouchPoint>();	//is still registered when the tap is recognized.
	
	/** Whether a new point has been touched somewhere **/
	public boolean	newTouch			= false;
	/** The number of touchpoints currently onscreen **/
	public int		numTouches			= 0;
	/** A bitmask of G_gestures that are currently in progress. **/
	public int		unhandledGestures	= G_NOTHING;
	
	
	boolean justDragged = false;
	boolean justPinched = false;
	boolean justTapped	= false;
	
	/** The average position of all touchpoints. For multitouch scrolling of all fingers **/
	public Vector2 avgTouchPoint	= new Vector2(0,0);
	/** The last average position of all touchpoints. Calculated via each point's pPos, not simple shunting of current avpos **/
	public Vector2 pAvgTouchPoint	= new Vector2(0,0);
	/** The change in the average position of all touchpoints. Calculated via each point's dPos, not current avg-pavg, so
	 * there is never going to be a giant jump in this when a new point is touched or removed, so long as TouchPoint makes sure
	 * it initializes its dPos as 0/pPos as current pos.
	 */
	public Vector2 dAvgTouchPoint	= new Vector2(0,0); //This does not "jump" when a new point is added.
	
	//Avg distance from avg center, for pinch to zoom
	public float avgDistance	= 0;
	public float pAvgDistance	= 0;
	
	/** Get a list of all points that have been only added since the last update **/
	public ArrayList<TouchPoint> getNewTouches() {
		return newTouches;
	}
	
	
	
	
	/** Simply ads a new touchpoint. **/
	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		log("TouchDwn: "+x+" "+y+" "+pointer);
		//Touchdown y is from the top, but drawing is origin at the bottom, so let's put it all in drawing coords.
		TouchPoint touch = new TouchPoint(new Vector2(x,y), pointer);
		touchPoints.add(touch);
		newTouches .add(touch);
		newTouch = true;
		
		return false;
	}

	/**A down-touchpoint has been moved. x, y is the new touchpoint. 
	 * Any touchDragged call will set the touchDragged flag
	 * Also, due to implementation details, any touchpoint moving will call touchDragged on all touchpoints,
	 * even if they have not moved.
	 * **/
	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		log("TouchDrg: "+x+" "+y+" "+pointer);
		for (TouchPoint iPoint : touchPoints) {
			if (iPoint.pointer == pointer) {
				iPoint.updatePos(new Vector2(x,y));
			}
		}
		justDragged = true;
		
		return true;
	}

	/** This will set the justTapped flag if the point just lifted was a tap. **/
	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		synchronized (touchPoints) {
			log("TouchUp:  "+x+" "+y+" "+pointer);
			TouchPoint toKill = new TouchPoint();
			for (TouchPoint iPoint : touchPoints) {
				if (iPoint.pointer == pointer) {
					
					//And this touch can't be a newtouch. If it was quickly placed and rele
					newTouches. remove(toKill);
					
					//If it was a tap, keep it around another frame so we see the touchpoint & tap at once
					if (iPoint.itsATap) {
						justTapped = true;
						tapEnds	.add(iPoint);
						//no lifted() because we want to pretend the touch is still being, yaknow, touched.
					} else {
						//Kill a non-tap right when we get out of this loop
						toKill = iPoint;
						//Notify the touch that it has been lifted
						iPoint.lifted();
					}
				}
			}
			touchPoints.remove(toKill);
		}
		return true;
	}
	
	/**
	 *	Call this once you are done accepting touchpoints. This calls each point to
	 *	check for tappedness. Do this right before you start handling input, and not
	 *	more than once.
	 *	This also calls calcActivePoints.  
	 */
	public void prep() {
		synchronized (touchPoints) {
			for(TouchPoint iPoint : touchPoints){
				iPoint.update();
			}
			calcActivePoints();
		}
	}
	/**
	 * Call this to calculate the gestures & avgpoints for all unhandled points.
	 * Recall this every time a gesture is handled to make sure it stays accurate.
	 */
	public void calcActivePoints() {
		updateAvgPoint();
		if (justTapped) {		//If we were just tapped, the touchpoint is maintained for an extra frame
			if (touchPoints.size() == 1) {
				unhandledGestures |= G_TAPPING;
			}
		}
		justTapped = false;
		if (touchPoints.size() == 1) {
			unhandledGestures |= G_HOLDING;
		}
		if (touchPoints.size() >= 1 && justDragged) {
			unhandledGestures |= G_DRAGGING;
		}
		else {
			unhandledGestures &= ~G_DRAGGING;
		}
		if (touchPoints.size() >= 2 && justDragged) {
			unhandledGestures |= G_PINCHING;
		}
		else {
			unhandledGestures &= ~G_PINCHING;
		}
		justDragged = false;
		
		
		numTouches = touchPoints.size();
		//System.out.print("Gesture Bitmask: " + unhandledGestures + "\n");
	}
	/** Call to notify we're done getting input this frame, setup stuff to pick up
	 * new inputs.
	 * Clears all gestures as well.
	 */
	public void reset() {
		//Reset the points for the next go around
		for (TouchPoint iPoint : touchPoints) {
			iPoint.flush();	//Tell the points that we got their p and d information.
		}
		//No touches are new any more!
		newTouches.clear();
		newTouch = false;
		for (TouchPoint each : tapEnds) {
			touchPoints.remove(each);
			each.lifted();
			touchPoints.size();
		}
		tapEnds.clear();
		handleAllGestures();
	}
	/** Removes a gesture from the in-progress gestures. **/
	public void gestureHandled(int gestureCode) {
		unhandledGestures &= ~gestureCode;
	}
	/** Remove all unhandled gestures.
	 * Call at end of input loop to prevent unhandled gestures from falling
	 * to the next frame, or if you really think you handled the entirety of
	 * all possible inputs and yet somehow do not account for all specific types.
	 */
	public void handleAllGestures() {
		unhandledGestures = G_NOTHING;
	};  
	void updateAvgPoint() {
		//this is for scrolling, which needs a pick ray based on screen coords. Since there is a slight bit of
		//perspective and I want the screen to move perfectly under the fingers, the actual touchpoint is kinda required.
		//This doesn't really matter for multiple points, as you won't notice it, but w/e
		Vector2 newPoint = new Vector2(0,0);
		for (TouchPoint iPoint : touchPoints) {
			newPoint.add(iPoint.iPos);
		}
		if (touchPoints.size() > 0) {
			//newPoint.mul(1/touchPoints.size());
			
			newPoint.x /= touchPoints.size();
			newPoint.y /= touchPoints.size();
			
			avgTouchPoint.x = newPoint.x;
			avgTouchPoint.y = newPoint.y;
		} //<--- See this curly brace? and the one after the if statement? It took three hours to figure out they were missing!
		
		
		
		//System.out.print("in Update Avg:   " + avgTouchPoint.x + "\n");
		
		
		//Do the previous center point. This is slightly better than just shunting new->old before recalculation
		//in the case that a new touch point is added. Maybe. I feel like neither thinking about this nor recoding
		//it. So we're shipping it.
		newPoint.x = 0;
		newPoint.y = 0;
		for (TouchPoint iPoint : touchPoints) {
			newPoint.add(iPoint.pPos);
		}
		if (touchPoints.size() > 0) {
			newPoint.x /= touchPoints.size();
			newPoint.y /= touchPoints.size();
			if (!(Double.isNaN(newPoint.x) || Double.isNaN(newPoint.y))) {
				pAvgTouchPoint.x = newPoint.x;
				pAvgTouchPoint.y = newPoint.y;
			}
		}
		
		//Delta average touch point, for panning.
		
		//We add up each individual one instead of avg-pavg so we don't get a sudden jump when
		//a new touchpoint is added. It's more of a total delta, not avg point delta.
		newPoint.x = 0; newPoint.y = 0;
		for (TouchPoint iPoint : touchPoints) {
			newPoint.add(iPoint.delta);
		}
		dAvgTouchPoint = newPoint.cpy().mul(1F/Math.max(1,touchPoints.size()));
			
			
		//Average distance from average center for pinch-to-zoom
		float newDistance = 0;
		for (TouchPoint iPoint : touchPoints) {
			newDistance += iPoint.iPos.dst(avgTouchPoint);
		}
		avgDistance = newDistance;
		newDistance = 0;
		for (TouchPoint iPoint : touchPoints) {
			newDistance += iPoint.pPos.dst(pAvgTouchPoint);
		}
		pAvgDistance = newDistance;
		
		//System.out.print("Avg Distance:  " + avgDistance + "\n");
		//System.out.print("Avg PDistance: " + pAvgDistance + "\n");
		if (touchPoints.size() > 1) {
			touchPoints.size();
		}
	}
	
	//This stuff is only used in libgdx for the desktop version.
	@Override
	public boolean touchMoved(int arg0, int arg1) {return false;}
	@Override
	public boolean keyDown(int arg0) {return false;}
	@Override
	public boolean keyTyped(char arg0) {return false;}
	@Override
	public boolean keyUp(int arg0) {return false;}
	@Override
	public boolean scrolled(int arg0) {return false;}
	
	private static void log(String message) {
		if (CLASS_DEBUG) {
			C.log(CLASS_TAG + " | "+ message);
		}
	}
}
