package com.remulasce.sandcastle.input;




public class Gesture {
	enum Type {
		PAN,
		ZOOM,
		TAP
		}
	//The touch point pointers of each location
	public int p1Pointer = 0;
	public int p2Pointer = 1;
	//Each touch's location. A tap or pan will only use 1, zoom needs two points.
	public Vector2 position1 = new Vector2(0,0);
	public Vector2 position2 = new Vector2(0,0);
	//The change in position since last tick
	public Vector2 dp1 = new Vector2(0,0);
	public Vector2 dp2 = new Vector2(0,0);
	
	public Gesture(Vector2 p1) {
		position1 = p1;
	}
	//
	public void Update(Vector2 pointer1New) {
		dp1 = new Vector2(pointer1New.x - position1.x, pointer1New.y - position1.y);
		position1 = pointer1New;
		
	}
}
