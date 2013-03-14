package com.remulasce.sandcastle.game;

import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.remulasce.sandcastle.C;
import com.remulasce.sandcastle.R;
import com.remulasce.sandcastle.game.objects.Block;
import com.remulasce.sandcastle.graphics.BlockShader;
import com.remulasce.sandcastle.graphics.DepthShader;
import com.remulasce.sandcastle.graphics.GUtils;
import com.remulasce.sandcastle.graphics.Mesh;
import com.remulasce.sandcastle.graphics.OverlayShader;
import com.remulasce.sandcastle.graphics.SandShader;
import com.remulasce.sandcastle.graphics.TestShader;
import com.remulasce.sandcastle.input.Joystick;

public class Renderer implements android.opengl.GLSurfaceView.Renderer {
	
	
	private static final boolean	DEBUG_FRAMETIME	= false;
	private static final long		DEBUG_INTERVAL	= 5000;	//ms
	private long	lastDebugDisplay	= 0;	//When we last showed the debug stuff
	
	//Don't recalculate the shadows every frame, because that would be slow.
	private static final boolean	LIMIT_SHADOW_UPDATE	= true;
	private static final boolean	LOG_SHADOW_TIME		= false;	//log depth render time
	private static final long		SHADOW_UPDATE_INTERVAL	= 500;
	private long 	lastShadowUpdate	= 0;
	private DecimalFormat df = new DecimalFormat("00.00");
	
	private int[] shaders = new int[6];
	private static final int s_test		= 0;	//Dev test
	private static final int s_block	= 1;	//Noshadow general texture
	private static final int s_sDepth	= 2;	//Depth-color for shadow map
	private static final int s_cDepth	= 3;	//Depth-color-pick for camera
	private static final int s_sand		= 3;	//Shadow general texture
	private static final int s_ui		= 4;	//textured noshadow for ui
	private static final int s_dev		= 5;	//testing without breaking
	
	
	
	private int[]	textures		= new int[12];
	private final int TEX_TEST		= 0;
	private final int TEX_SAND		= 1;
	private final int TEX_GROUND	= 2;
	private final int DEPTH_SHADOW	= 3;	// Depth from light, for shadows
	private final int DEPTH_CAMERA	= 6;	// Depth from camera, for clip+pick
	private final int TEX_JOY_T		= 4;	//Joystick top mover
	private final int TEX_JOY_B		= 5;	//Joystick background area
	
	
	
	//Special stuff for shadows
	private static final int LIGHT_W = 1024;
	private static final int LIGHT_H = 1024;
	
	private int[]	frameBuffers	= new int[2];
	private static final int	screenFBuffer	= 1;
	private static final int	depthFBuffer	= 0;
	
	private int[]	renderBuffers	= new int[1];
	private static final int	depthRBuffer	= 0;
	
	
	
	private World	world;
	private Context	context;
	
	
	private float[] mMatrix		= new float[16];	//Model matrix
	private float[] vMatrix		= new float[16];	//View matrix
	private float[] pMatrix		= new float[16];	//Projection matrix
	private float[] vpMatrix	= new float[16];	//V*P matrix
	
	private float[] lPos		= new float[4];		//Light position
	private float[] lmvpMatrix	= new float[16];	//Light MVP matrix
	private float[] lpMatrix	= new float[16];	
	
	private float[] uivpMatrix	= new float[16];	//UI 2D vp matrix
	
	//Vertex buffers && models. Meshes should eventually all be transfered 
	//	to VBOs, since I think most devices should have plenty enough memory.
	private int[]	meshBuffers	= new int[3];
	private static final int	BLOCK_VBUFFER	= 0;
	private static final int	BLOCK_TBUFFER	= 1;
	private static final int	BLOCK_NBUFFER	= 2;
	
	private Mesh blockMesh;
	private Mesh groundMesh;
	private Mesh planeMesh;
	
	
	//Debug display stuff
	long lastRender		= System.currentTimeMillis();
	

	
	public Renderer(Context context) {
		this.context = context;
	}
	
	
	public void setWorld(World world) {
		this.world = world;
	}
	
	
	
	@Override
	public void onDrawFrame(GL10 arg0) {
		
		{
		try {
			world.lock.acquire();
			
			
			calculateCamera();
			
			
			drawObjects();
			drawUI();
			
			
			doLogging();
			doClock();
			
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		world.lock.release();
		}
	}
	
	private void drawObjects() {
		
		if (!LIMIT_SHADOW_UPDATE || System.currentTimeMillis() > lastShadowUpdate + SHADOW_UPDATE_INTERVAL) {
			lastShadowUpdate = System.currentTimeMillis();
			drawDepth();
			if (LOG_SHADOW_TIME) {
				float frametime = System.currentTimeMillis() - lastShadowUpdate;
				C.log("Depth frametime ms: "+frametime);
			}
		}
		
			
		//Get out of depth mode and into real mode
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, C.SCR_WIDTH, C.SCR_HEIGHT);
		
		//Now get out of this framebuffer ant back to the normal one
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		//We turned it off for ui, and since only part of this is legit, we have to reenable
		//for everybody now here.
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glCullFace(GLES20.GL_BACK);
		drawGround();
		drawBlocks(); 
		
	}
	
	private void drawDepth() {
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[depthFBuffer]);
//		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, LIGHT_W, LIGHT_H);
		

		//Attach the depth texture to the output of the framebuffer
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[DEPTH_SHADOW], 0);
		//And give it a depth buffer by attaching the renderbuffer to the depth attachment point
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderBuffers[depthRBuffer]);
		
		GLES20.glUseProgram(shaders[s_sDepth]);
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_FRONT);
		
		GLES20.glEnableVertexAttribArray(DepthShader.a_vPos);
		
		//View type matrices
		GLES20.glUniformMatrix4fv(DepthShader.u_vpMat, 1, false, lmvpMatrix, 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[DEPTH_SHADOW]);
		
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0, 0, 0, -.5f);
		
		GLES20.glVertexAttribPointer(DepthShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, groundMesh.vb);
		GLES20.glUniformMatrix4fv(DepthShader.u_mMat, 1, false, mMatrix, 0);
		
		GLES20.glDrawArrays (GLES20.GL_TRIANGLES, 0, planeMesh.vertices.length/3);
		
		
		for (Block each : world.blocks) {
			each.setTransform(mMatrix);
			
//			GLES20.glVertexAttribPointer	(DepthShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, blockMesh.vb);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_VBUFFER]);
			GLES20.glVertexAttribPointer(DepthShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			GLES20.glUniformMatrix4fv		(DepthShader.u_mMat, 1, false, mMatrix, 0);
			
			GLES20.glDrawArrays			(GLES20.GL_TRIANGLES, 0, blockMesh.vertices.length/3);
		}
		
		
		
		GLES20.glDisableVertexAttribArray(DepthShader.a_vPos);
		
	}
	
	private void drawBlocks() {
		GLES20.glUseProgram(shaders[s_sand]);
		
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glEnable(GLES20.GL_TEXTURE);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);
		
		GLES20.glEnableVertexAttribArray(SandShader.a_vPos);
		GLES20.glEnableVertexAttribArray(SandShader.a_tCoord);
		GLES20.glEnableVertexAttribArray(SandShader.a_nDir);
		
		//View type matrices
		GLES20.glUniformMatrix4fv(SandShader.u_vpMat, 1, false, vpMatrix, 0);
		GLES20.glUniform4fv(SandShader.u_lPos, 1, lPos, 0);
		GLES20.glUniformMatrix4fv(SandShader.u_lMat, 1, false, lmvpMatrix, 0);


		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEX_SAND]);
		GLES20.glUniform1i(SandShader.u_texSamp, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[DEPTH_SHADOW]);
		GLES20.glUniform1i(SandShader.u_depthSamp, 1);
		
		
		for (Block each : world.blocks) {
			each.setTransform(mMatrix);
			
//			GLES20.glVertexAttribPointer	(SandShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, blockMesh.vb);
			
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_VBUFFER]);
			GLES20.glVertexAttribPointer(SandShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_TBUFFER]);
			GLES20.glVertexAttribPointer	(SandShader.a_tCoord, 2, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_NBUFFER]);
			GLES20.glVertexAttribPointer	(SandShader.a_nDir, 3, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			GLES20.glUniformMatrix4fv		(SandShader.u_mMat, 1, false, mMatrix, 0);
			
			GLES20.glDrawArrays			(GLES20.GL_TRIANGLES, 0, blockMesh.vertices.length/3);
		}
		
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendColor(0, 0, 0, .5f);
//		GLES20.glBlendEquation(GLES20.GL_FUNC_SUBTRACT);
		GLES20.glBlendFunc(GLES20.GL_CONSTANT_ALPHA, GLES20.GL_ONE);
		
		//DRaw testblock, if avail
		//With vertex buffers!
		if (world.selectBlock != null) {
			world.selectBlock.setTransform(mMatrix);
			
//			GLES20.glVertexAttribPointer	(SandShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, blockMesh.vb);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_VBUFFER]);
			GLES20.glVertexAttribPointer(SandShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_TBUFFER]);
			GLES20.glVertexAttribPointer	(SandShader.a_tCoord, 2, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_NBUFFER]);
			GLES20.glVertexAttribPointer	(SandShader.a_nDir, 3, GLES20.GL_FLOAT, false, 0, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			GLES20.glUniformMatrix4fv		(SandShader.u_mMat, 1, false, mMatrix, 0);
			
			GLES20.glDrawArrays			(GLES20.GL_TRIANGLES, 0, blockMesh.vertices.length/3);
		}
		
		
		
		GLES20.glDisable(GLES20.GL_BLEND);
//		GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
		
		GLES20.glDisableVertexAttribArray(SandShader.a_vPos);
		GLES20.glDisableVertexAttribArray(SandShader.a_tCoord);
		GLES20.glDisableVertexAttribArray(SandShader.a_nDir);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_TEXTURE);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
	}
	
	private void drawGround() {
		GLES20.glUseProgram(shaders[s_sand]);
		
		//No need for depth test, we're first here.
		//Simple geometry, no culling either
		GLES20.glEnable(GLES20.GL_TEXTURE);
		
		GLES20.glEnableVertexAttribArray(SandShader.a_vPos);
		GLES20.glEnableVertexAttribArray(SandShader.a_tCoord);
		GLES20.glEnableVertexAttribArray(SandShader.a_nDir);
		
		GLES20.glUniformMatrix4fv(SandShader.u_vpMat, 1, false, vpMatrix, 0);
		GLES20.glUniformMatrix4fv(SandShader.u_lMat, 1, false, lmvpMatrix, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEX_GROUND]);
		GLES20.glUniform1i(SandShader.u_texSamp, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[DEPTH_SHADOW]);
		GLES20.glUniform1i(SandShader.u_depthSamp, 1);
		
		
		//////Now draw
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.scaleM(mMatrix, 0, .3f, .3f, 1f);
		Matrix.translateM(mMatrix, 0, 0, 0, -.5f);
		
		GLES20.glVertexAttribPointer	(SandShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, groundMesh.vb);
		GLES20.glVertexAttribPointer	(SandShader.a_tCoord, 2, GLES20.GL_FLOAT, false, 0, groundMesh.tb);
		GLES20.glVertexAttribPointer	(SandShader.a_nDir, 3, GLES20.GL_FLOAT, false, 0, groundMesh.nb);
		GLES20.glUniformMatrix4fv		(SandShader.u_mMat, 1, false, mMatrix, 0);
		
		GLES20.glDrawArrays			(GLES20.GL_TRIANGLES, 0, groundMesh.vertices.length/3);
		
		
		GLES20.glDisableVertexAttribArray(SandShader.a_vPos);
		GLES20.glDisableVertexAttribArray(SandShader.a_tCoord);
		GLES20.glDisableVertexAttribArray(SandShader.a_nDir);
		
		GLES20.glDisable(GLES20.GL_TEXTURE);
		
		
		
	}
	
	private void drawUI() {
		GLES20.glUseProgram(shaders[s_ui]);
		OverlayShader.enable();
		
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		
		
		//Draw joysticks, starting with the backs of each, then tops.
		GLES20.glUniformMatrix4fv(OverlayShader.u_vpMat, 1, false, uivpMatrix, 0);
		//Load joystick background texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEX_JOY_B]);
		GLES20.glUniform1i(OverlayShader.u_texSamp, 0);
		//Load plain ui mesh
		GLES20.glVertexAttribPointer	(OverlayShader.a_vPos, 3, GLES20.GL_FLOAT, false, 0, planeMesh.vb);
		GLES20.glVertexAttribPointer	(OverlayShader.a_texCoord, 2, GLES20.GL_FLOAT, false, 0, planeMesh.tb);
		//Set model matrix for first joystick
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0, world.joyL.getBackX(), world.joyL.getBackY(), 0);
		Matrix.scaleM(mMatrix, 0, Joystick.BACK_SIZE, Joystick.BACK_SIZE, 1);
		GLES20.glUniformMatrix4fv(OverlayShader.u_mMat, 1, false, mMatrix, 0);
		//Draw the back of the first joystick
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, planeMesh.vertices.length/3);
		//Set modelmatrix of second joystick back
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0, world.joyR.getBackX(), world.joyR.getBackY(), 0);
		Matrix.scaleM(mMatrix, 0, Joystick.BACK_SIZE, Joystick.BACK_SIZE, 1);
		GLES20.glUniformMatrix4fv(OverlayShader.u_mMat, 1, false, mMatrix, 0);
		//Draw back of second joystick
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, planeMesh.vertices.length/3);
		
		//Set texture for joystick tops
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEX_JOY_T]);
		GLES20.glUniform1i(OverlayShader.u_texSamp, 0);
		//Set model matrix for first joystick top
		//WOW, just noticed the already-in-place convenience methods. Nice job, me.
		world.joyL.setTransformTop(mMatrix);
		Matrix.scaleM(mMatrix, 0, Joystick.TOP_SIZE, Joystick.TOP_SIZE, 1);
		GLES20.glUniformMatrix4fv(OverlayShader.u_mMat, 1, false, mMatrix, 0);
		//Draw top of left joystick
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, planeMesh.vertices.length/3);
		//Set modelmatrix for
		world.joyR.setTransformTop(mMatrix);
		Matrix.scaleM(mMatrix, 0, Joystick.TOP_SIZE, Joystick.TOP_SIZE, 1);
		GLES20.glUniformMatrix4fv(OverlayShader.u_mMat, 1, false, mMatrix, 0);
		//Draw top of right matrix
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, planeMesh.vertices.length/3);
		
		
		//Draw the action tray
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[TEX_SAND]);
		GLES20.glUniform1i(OverlayShader.u_texSamp, 0);
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0, C.SCR_WIDTH/2, C.SCR_HEIGHT-100, 0);
		Matrix.scaleM(mMatrix, 0, 100, 100, 1);
		
		GLES20.glUniformMatrix4fv(OverlayShader.u_mMat, 1, false, mMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, planeMesh.vertices.length/3);
		
		
		
		GLES20.glDisable(GLES20.GL_BLEND);
		OverlayShader.disable();
	}
	
	private void calculateCamera() {
		world.camera.setVPMatrix(vpMatrix);
		
	}
	
	
	private void doLogging() {
		if (DEBUG_FRAMETIME && System.currentTimeMillis() > lastDebugDisplay + DEBUG_INTERVAL) {
			float frametime = (System.currentTimeMillis()-lastRender);
			C.log(df.format(1000/frametime)+" fps "+df.format(frametime)+" ms frametime");
			lastDebugDisplay = System.currentTimeMillis();
		}
	}
	private void doClock() {
		lastRender = System.currentTimeMillis();
	}

	@Override
	public void onSurfaceChanged(GL10 arg0, int width, int height) {
		GLES20.glViewport(0,0,width,height);
		C.SCR_WIDTH = width; C.SCR_HEIGHT = height;
		
//		world.camera.setProjection(pMatrix, ((float)width)/height);
		world.camera.reset();
		
		calculateCamera();
		calculateUIProjection();
		calculateShadowProjection();
		calculateUIPositions();
	}
	/** Recalculate the positions of screen-size-dependent ui things, like
	 * stuff that needs to stay in specific corners.
	 */
	private void calculateUIPositions() {
		world.joyL.setPos(40, C.SCR_HEIGHT-Joystick.BACK_SIZE-40);
		world.joyR.setPos(C.SCR_WIDTH-Joystick.BACK_SIZE-40, C.SCR_HEIGHT-Joystick.BACK_SIZE-40);

	}
	/** create an orthographic, 1:1 pixel scale vp matrix for displaying textures
	 * at pixel-defined places. Origin is in the top-left of the screen.
	 */
	private void calculateUIProjection() {
		float[] vpMatrix	= new float[16];
		Matrix.setIdentityM(vpMatrix, 0);
		float[] pMatrix = new float[16];
		Matrix.setIdentityM(pMatrix, 0);
		Matrix.orthoM(pMatrix, 0,
				0, C.SCR_WIDTH,
				C.SCR_HEIGHT, 0,
				1, 10);
		Matrix.setLookAtM(vpMatrix, 0, 0, 0, 4, 0, 0, 0, 0, 1, 0);
		
		Matrix.multiplyMM(uivpMatrix, 0, pMatrix, 0, vpMatrix, 0);
	}
	
	private void calculateShadowProjection() {
		Matrix.setIdentityM(lpMatrix, 0);
		Matrix.setIdentityM(lmvpMatrix, 0);

		lPos = new float[] { -6, -10, 14, 0 };
		Matrix.setLookAtM(lmvpMatrix, 0, -6, -10, 14, 0, 2, 0, 0, 1, 0);
		GUtils.perspectiveM(pMatrix, 0, 60, 1, 6, 52);
//		Matrix.orthoM(lpMatrix, 0, -10, 10, -10, 10, 1, 200);
		
		Matrix.multiplyMM(lmvpMatrix, 0, pMatrix, 0, lmvpMatrix, 0);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glClearColor(.5f, .6f, 1f, 1f);
		

		loadTextures();
		loadShaders();
		loadModels();
		loadShadows();
	}
	
	private void loadModels() {
		
		blockMesh	= new Mesh();
		Mesh.loadMesh(blockMesh, context, R.raw.m_sandblock);
		groundMesh	= new Mesh();
		Mesh.loadMesh(groundMesh, context, R.raw.m_testground);
		planeMesh	= new Mesh();
		Mesh.loadMesh(planeMesh, context, R.raw.m_uiplane);
		
		
		GLES20.glGenBuffers(meshBuffers.length, meshBuffers, 0);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_VBUFFER]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, blockMesh.vb.capacity() * 4, blockMesh.vb, GLES20.GL_STATIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_TBUFFER]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, blockMesh.tb.capacity() * 4, blockMesh.tb, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, meshBuffers[BLOCK_NBUFFER]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, blockMesh.nb.capacity() * 4, blockMesh.nb, GLES20.GL_STATIC_DRAW);
		
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}
	
	private void loadShadows() {
		
		GLES20.glDisable(GLES20.GL_DITHER);
		GLES20.glClearDepthf(1.0f);
		GLES20.glDepthMask(true);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		
		
		
		//Get names for all the things we're going to make
		GLES20.glGenFramebuffers(1, frameBuffers, 0);
		GLES20.glGenRenderbuffers(1, renderBuffers, 0);
		GLES20.glGenTextures(1, textures, DEPTH_SHADOW);
		
		//Setup the depth texture we draw to in depth rendering
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[DEPTH_SHADOW]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		
		GLES20.glActiveTexture(textures[DEPTH_SHADOW]);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, LIGHT_W, LIGHT_H, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffers[depthRBuffer]);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, LIGHT_W, LIGHT_H);
		
	}
	
	private void loadShaders() {
		loadResShader(s_test, R.raw.sv_test, R.raw.sf_test);
		TestShader.setPointers(shaders[s_test]);
		loadResShader(s_block, R.raw.sv_block, R.raw.sf_block);
		BlockShader.setPointers(shaders[s_block]);
		loadResShader(s_sDepth, R.raw.sv_depth, R.raw.sf_depth);
		DepthShader.setPointers(shaders[s_sDepth]);
		loadResShader(s_sand, R.raw.sv_sand_shadow, R.raw.sf_sand_shadow);
		SandShader.setPointers(shaders[s_sand]);
		loadResShader(s_ui, R.raw.sv_overlay, R.raw.sf_overlay);
		OverlayShader.setPointers(shaders[s_ui]);
		loadResShader(s_dev, R.raw.sv_sand_dev, R.raw.sf_sand_dev);
		
	}
	
	private void loadTextures() {
		textures[TEX_TEST]		= GUtils.loadTexture(context, R.raw.t_test);
		textures[TEX_SAND]		= GUtils.loadTexture(context, R.raw.t_sand);
		textures[TEX_GROUND]	= GUtils.loadTexture(context, R.raw.t_beach);
		textures[TEX_JOY_B]		= GUtils.loadTexture(context, R.raw.t_joystick_back);
		textures[TEX_JOY_T]		= GUtils.loadTexture(context, R.raw.t_joystick_top);
	}

	private void loadResShader(int name, int vertex, int fragment) {
		int vertexShader	= GUtils.loadShader(context, GLES20.GL_VERTEX_SHADER,	vertex);
		int fragmentShader	= GUtils.loadShader(context, GLES20.GL_FRAGMENT_SHADER,	fragment);
		
		shaders[name] = GLES20.glCreateProgram();			//makes a new program, and returns its ID, presumably
		GLES20.glAttachShader(shaders[name], vertexShader);
		GLES20.glAttachShader(shaders[name], fragmentShader);
		GLES20.glLinkProgram (shaders[name]);
	}
	
	//final, memory-freeing gesture
	public void kill() {
		
	}

}
