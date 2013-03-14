package com.remulasce.sandcastle.game.objects;

import android.opengl.Matrix;

import com.remulasce.sandcastle.graphics.Mesh;


public class Ground extends Block {
	public Mesh mesh;
	
	public Ground() {
		super();
		
		this.pressStr = 10000;
		this.shearStr = 10000;
		this.loadSteps= 0;
	}

	@Override
	public void setTransform(float[] mMatrix) {
		Matrix.setIdentityM(mMatrix, 0);
	}
	@Override
	public boolean hasLoadPath() {
		return true;
	}
	@Override
	public int getLoadSteps() {
		return 0;
	}
	@Override
	public void receiveLoad(float amnt) {
		//Nothing. No drive-backs.
	}
}
