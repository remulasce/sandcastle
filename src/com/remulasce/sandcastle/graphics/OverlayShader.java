package com.remulasce.sandcastle.graphics;

import android.opengl.GLES20;

public class OverlayShader {
	public static int a_vPos	=0;
	public static int u_mMat	=0;
	public static int u_vpMat	=0;
	public static int a_texCoord=0;
	public static int u_texSamp	=0;
	
	public static void setPointers(int program) {
		OverlayShader.a_vPos	= GLES20.glGetAttribLocation	(program, "a_vPos");
		OverlayShader.u_mMat	= GLES20.glGetUniformLocation	(program, "u_mMat");
		OverlayShader.u_vpMat	= GLES20.glGetUniformLocation	(program, "u_vpMat");
		OverlayShader.a_texCoord= GLES20.glGetAttribLocation	(program, "a_texCoord");
		OverlayShader.u_texSamp	= GLES20.glGetUniformLocation	(program, "u_texSamp");
	}
	/** Enables:
	 * 		GL_TEXTURE
	 * 		Vertex Attrib Arrays
	 * 	Disables:
	 * 		Depth testing
	 */
	public static void enable() {
		GLES20.glEnable(GLES20.GL_TEXTURE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		
		GLES20.glEnableVertexAttribArray(a_vPos);
		GLES20.glEnableVertexAttribArray(a_texCoord);
	}
	/**	Disables:
	 * 		GL_TEXTURE
	 * 		Vertex Attrib Arrays
	 */
	public static void disable() {
		GLES20.glDisable(GLES20.GL_TEXTURE);
		
		GLES20.glDisableVertexAttribArray(a_vPos);
		GLES20.glDisableVertexAttribArray(a_texCoord);
	}
}
