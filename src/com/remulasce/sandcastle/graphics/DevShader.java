package com.remulasce.sandcastle.graphics;

import android.opengl.GLES20;

public class DevShader {
	public static int a_vPos;
	public static int a_tCoord;
	public static int a_nDir;
	public static int u_mMat;
	public static int u_vpMat;
	public static int u_lPos;
	public static int u_lMat;
	public static int u_texSamp;
	public static int u_depthSamp;
	
	public static void setPointers(int program) {
		a_vPos		= GLES20.glGetAttribLocation	(program, "a_vPos");
		a_tCoord	= GLES20.glGetAttribLocation	(program, "a_tCoord");
		a_nDir		= GLES20.glGetAttribLocation	(program, "a_nDir");
		u_mMat		= GLES20.glGetUniformLocation	(program, "u_mMat");
		u_vpMat		= GLES20.glGetUniformLocation	(program, "u_vpMat");
		u_lPos		= GLES20.glGetUniformLocation	(program, "u_lPos");
		u_lMat		= GLES20.glGetUniformLocation	(program, "u_lMat");
		u_texSamp	= GLES20.glGetUniformLocation	(program, "u_texSamp");
		u_depthSamp	= GLES20.glGetUniformLocation	(program, "u_depthSamp");
	}
}
