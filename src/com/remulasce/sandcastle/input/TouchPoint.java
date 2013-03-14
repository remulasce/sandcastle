package com.remulasce.sandcastle.input;



public class TouchPoint {
	private static final long TAP_TIME = 500;	//How short ms a touch-release must be to be a tap 
	
	Vector2 iPos  = new Vector2(0,0);	//The current position of this touch point
	Vector2 pPos  = new Vector2(0,0);	//The previous position of this particular touch point
	Vector2 delta = new Vector2(0,0);	//iPos-pPos, nice and done for scrolling
	int pointer;						//The id of this touch point
	
	boolean pressed = true;				//Whether this touch point is, well, still a touchpoint.
	
	Vector2 initPos = new Vector2(0,0);	//The first place this point was touched.
	private long startTime = -1;		//When this point was first touched
	boolean itsATap = false;			//It's a tap! (didn't last long)
	
	public TouchPoint() {}
	public TouchPoint(Vector2 pos, int maPointer) {
		iPos	= pos.cpy();
		pPos	= pos.cpy();
		initPos = pos.cpy();
		pointer = maPointer;
		startTime	= System.currentTimeMillis();
	}
	
	public Vector2 getPos() { return iPos; }
	public float x() { return iPos.x; }
	public float y() { return iPos.y; }
	
	
	/** Set a new position. pPos will not be changed, and delta will be cumalative since last flush **/
	public void updatePos(Vector2 newPos) {
		iPos	= newPos.cpy();
		delta.x	= (iPos.x - pPos.x);
		delta.y	= (iPos.y - pPos.y);
	}
	public void update() {
		if (iPos.dst(initPos) < 4  && System.currentTimeMillis() - startTime > 10 && System.currentTimeMillis() - startTime < TAP_TIME) {
			itsATap = true;
		}
		else {
			itsATap = false;
		}
	}
	/** Reset the delta to 0 and pPos to iPos
	 * The point's updateposition might be called more quickly than the engine can update,
	 * so keep track of them between flushes, not updates. Otherwise, in a 2-update case,
	 * the first update's pPos and delta woud be wiped out by the second's before the engine
	 * can see the first's.
	 */
	public void flush() {
		delta.set(0,0);
		pPos.set(iPos);
	}
	
	/** Return the unique identifying id of this touchpoint. **/
	public int getPointer() {
		return pointer;
	}
	/** Returns the system time ms at which the touch was first put down **/
	public long getStart() {
		return this.startTime;
	}
	/** Tell the touchpoint that it has been lifted.
	 * Usually a touchpoint would be garbage-collected when it is lifted, since the inputengine
	 * no longer maintains references to lifted points, but if, say, a joystick got a hold of
	 * this point to keep track of only it, Joystick would like to know that this particular point
	 * has been lifted, ever if its pointer has been quickly reassigned.
	 */
	public void lifted() {
		pressed = false;
	}
	public boolean isPressed() {
		return pressed;
	}
}
