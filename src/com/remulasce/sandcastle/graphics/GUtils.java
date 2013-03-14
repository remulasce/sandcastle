package com.remulasce.sandcastle.graphics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class GUtils {
	/**
     * Define a projection matrix in terms of a field of view angle, an
     * aspect ratio, and z clip planes
     * @param m the float array that holds the perspective matrix
     * @param offset the offset into float array m where the perspective
     * matrix data is written
     * @param fovy field of view in y direction, in degrees
     * @param aspect width to height aspect ratio of the viewport
     * @param zNear
     * @param zFar
     */
    public static void perspectiveM(float[] m, int offset,
          float fovy, float aspect, float zNear, float zFar) {
        float f = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0));
        float rangeReciprocal = 1.0f / (zNear - zFar);

        m[offset + 0] = f / aspect;
        m[offset + 1] = 0.0f;
        m[offset + 2] = 0.0f;
        m[offset + 3] = 0.0f;

        m[offset + 4] = 0.0f;
        m[offset + 5] = f;
        m[offset + 6] = 0.0f;
        m[offset + 7] = 0.0f;

        m[offset + 8] = 0.0f;
        m[offset + 9] = 0.0f;
        m[offset + 10] = (zFar + zNear) * rangeReciprocal;
        m[offset + 11] = -1.0f;

        m[offset + 12] = 0.0f;
        m[offset + 13] = 0.0f;
        m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
        m[offset + 15] = 0.0f;
    }
    
	public static int loadShader(Context context, int type, int shaderCode) {
		//either GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER
		int shader = GLES20.glCreateShader(type);
		StringBuffer sb = new StringBuffer();
		try {
			InputStream inputStream = context.getResources().openRawResource(shaderCode);
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String read = in.readLine();
			while (read != null) {
				sb.append(read + "\n");
				read = in.readLine();
			}
		} catch (Exception e) {
			Log.e("Error", "Could not read shader");
			
			throw new RuntimeException("Shader read failed");	}
		//Add the source to the shader and compile
		GLES20.glShaderSource(shader, sb.toString());
		GLES20.glCompileShader(shader);
		//Make sure it compiled
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("shader", "Could not compile shader " + shaderCode + ":");
            Log.e("shader", "Problem: "+GLES20.glGetShaderInfoLog(shader));
            Log.e("shader", " "+GLES20.glGetError());
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
		return shader;
	}
	
	public static int loadTexture(final Context context, final int resourceId)
	{
	    final int[] textureHandle = new int[1];
	 
	    GLES20.glGenTextures(1, textureHandle, 0);
	 
	    if (textureHandle[0] != 0)
	    {
	        final BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inScaled = false;   // No pre-scaling
	 
	        // Read in the resource
	        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
	 
	        // Bind to the texture in OpenGL
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
	 
	        // Set filtering
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
	 
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	        
	        
	        // Load the bitmap into the bound texture.
	        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	 
	        // Recycle the bitmap, since its data has been loaded into OpenGL.
	        bitmap.recycle();
	    }
	 
	    if (textureHandle[0] == 0)
	    {
	        throw new RuntimeException("Error loading texture.");
	    }
	 
	    return textureHandle[0];
	}
}
