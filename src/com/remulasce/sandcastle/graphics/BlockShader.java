package com.remulasce.sandcastle.graphics;

import android.opengl.GLES20;

public class BlockShader {
	public static int a_vPos	=0;
	public static int a_texCoord=0;
	public static int u_mMat	=0;
	public static int u_vpMat	=0;
	public static int u_texSamp	=0;
	
	public static void setPointers(int program) {
		a_vPos		= GLES20.glGetAttribLocation	(program, "a_vPos");
		a_texCoord	= GLES20.glGetAttribLocation	(program, "a_texCoord");
		u_mMat		= GLES20.glGetUniformLocation	(program, "u_mMat");
		u_vpMat		= GLES20.glGetUniformLocation	(program, "u_vpMat");
		u_texSamp	= GLES20.glGetUniformLocation	(program, "u_texSamp");
	}
}