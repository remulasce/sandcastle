package com.remulasce.sandcastle.graphics;

import android.opengl.GLES20;

public class TestShader {
	public static int a_vPos	=0;
	public static int u_vpMat	=0;
	public static int u_mMat	=0;
	
	public static void setPointers(int program) {
		TestShader.a_vPos	= GLES20.glGetAttribLocation(program, "a_vPos");
		TestShader.u_vpMat	= GLES20.glGetUniformLocation(program, "u_vpMat");
		TestShader.u_mMat	= GLES20.glGetUniformLocation(program, "u_mMat");
	}
}
