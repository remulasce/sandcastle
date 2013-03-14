package com.remulasce.sandcastle.game.objects;

public abstract class Entity {
	public float	x, y, z;
	public boolean	shouldDie;

	public abstract void setTransform(float[] mMatrix);
	
	
}
