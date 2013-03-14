package com.remulasce.sandcastle.graphics;

import com.remulasce.sandcastle.C;

import android.opengl.Matrix;

/** Camera for fully-navigable sand castle world.
 * Well, effectively fully-navigable. We cheat a bit, but the limitations of the
 * shortcut are identical to other design requirements, so it actually is a good fit.
 * 
 * We support 3D panning and 2D rotating
 * Steps:
 * 	1) Rotate about X axis (up-down rotation)
 * 	2) Rotate about Z axis (left-right rotation)
 * 	3) Pan world in 3D	(global, so happens after local rotations)
 * 
 * This way, the things that need to be local rotations are, and globals aren't.
 * */
public class Camera {
	public static final float CAMERA_FOV	= 40;
	private static final float Z_NEAR		= 1f;
	private static final float Z_FAR		= 100;
	

	private float x, y, z;
	private float rAttitude;	//Rotation looking up or down, degrees
	private float rTurn;		//Rotation looking left or right, d
	private float[] pMatrix	= new float[16];
	private float[] vMatrix	= new float[16];
	
	
	private boolean dirty	= true;	//Whether the whole matrix needs to be reset
	public Camera() {
//		reset();
	}
	/** Reset matrices to default position, 0,-10,4 looking 0,0,0,
	 * handle screen size changes. */
	public void reset() {
		x = 0; y = -10; z = 8;
		rAttitude = 70; rTurn = 0;
		Matrix.setIdentityM(pMatrix, 0);
		Matrix.setIdentityM(vMatrix, 0);
		GUtils.perspectiveM(pMatrix, 0, CAMERA_FOV, ((float)C.SCR_WIDTH)/C.SCR_HEIGHT, Z_NEAR, Z_FAR);

		Matrix.translateM(vMatrix, 0, 0, 0, -10);
	}
	/** Return the camera's VP matrix, to be used in rendering */
	public void setVPMatrix(float[] matrix) {
		if (dirty) {
			dirty = false;
			Matrix.setIdentityM(vMatrix, 0);
			Matrix.rotateM(vMatrix, 0, -rAttitude, 1, 0, 0);
			Matrix.rotateM(vMatrix, 0, -rTurn, 0, 0, 1);
			Matrix.translateM(vMatrix, 0, -x, -y, -z);
		}
		
		
		Matrix.multiplyMM(matrix, 0, pMatrix, 0, vMatrix, 0);
	}
	
	/** Directly translate the camera by xyz */
	public void pan(float x, float y, float z) {
		//negative for this being the view matrix taken care of in setvp
		this.x+=x;this.y+=y;this.z+=z;
		dirty = true;
	}
	/** Move in relation to current orientation. +Y up, +X right, Z unaffected. */
	public void move(float x, float y, float z) {
		this.y += Math.cos(Math.toRadians(rTurn))*y + Math.sin(Math.toRadians(rTurn))*x;
		this.x += -Math.sin(Math.toRadians(rTurn))*y + Math.cos(Math.toRadians(rTurn))*x;
		this.z += z;
		dirty = true;
	}
	/** Look a bit straight up. Looks from local z, as in, aligned with y pixels. degs. */
	public void rotateUp(float r) {
		rAttitude += r;
		dirty = true;
	}
	/** Look right, x-coordinate pixel style. Degrees. */
	public void rotateRight(float r) {
		//Why is this one minus? Just don't ask.
		rTurn -= r;
		dirty = true;
	}
	
	public float getX() { return x; }
	public float getY() { return y; }
	public float getZ() { return z; }
	public float getAttitude()	{ return rAttitude; }
	public float getRot()		{ return rTurn;		}
}
