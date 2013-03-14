package com.remulasce.sandcastle.graphics;

import android.opengl.GLES20;

public class DepthShader {

	public static int a_vPos;		//Vertex position, in pixels
	public static int u_mMat;
	public static int u_vpMat;		//The (hopefully orthographic) vp matrix
	
	public static void setPointers(int program) {
		a_vPos		= GLES20.glGetAttribLocation(program, "a_vPos");
		u_mMat		= GLES20.glGetUniformLocation(program, "u_mMat");
		u_vpMat		= GLES20.glGetUniformLocation(program, "u_vpMat");
	}
	
}
