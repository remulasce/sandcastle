package com.remulasce.sandcastle;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.remulasce.sandcastle.game.Renderer;
import com.remulasce.sandcastle.game.Simulation;
import com.remulasce.sandcastle.game.World;
import com.remulasce.sandcastle.input.EngineTouchListener;
import com.remulasce.sandcastle.input.InputEngine;

public class SandCastleActivity extends Activity {
	
	private World		world;
	private Simulation	simulation;
	private Renderer	renderer;
	private InputEngine	input;
	
	private SandCastleGLView glView;
	
	
	//whether game is paused, regardless of activity state.
	private boolean		paused	= false;
	
	
    /** Called when the activity is first created.
     * 
     * Here we acquire the game world we're going to be playing with.
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        C.log("SandCastleActivity OnCreate");
        
        glView		= new SandCastleGLView(this);
        setContentView(glView);
        
        
        //spawn world with some test things.
        world		= new World();
        //Setup a test world instead of loading a real one
        world.loadTest();

        
        
        
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	//Create things
    	renderer	= new Renderer(this);
    	simulation	= new Simulation();
    	input		= new InputEngine();
    	//Link them up
    	renderer	.setWorld(world);
    	simulation	.setWorld(world);
    	simulation	.setInputEngine(input);
    	glView		.setInputEngine(input);
    	glView		.setRenderer(renderer);
    	
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if (!paused) {
    		//Play!
    		glView.onResume();
    		simulation.start();
    	}
    	else {
    		//TODO make sure pause menu is showing
    	}
    	
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	if (!paused) {
    		simulation.stop();
    		glView.onPause();
    		//TODO bring up pause menu
    	}
    	
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	//close down running stuff
    	simulation.kill();
    	renderer.kill();
    	
    	//Eliminate references so GC can collect memory
    	simulation	= null;
    	renderer	= null;
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
}


class SandCastleGLView extends GLSurfaceView {

	public SandCastleGLView(Context context) {
		super(context);
		C.log("SandCastleGLView Constructor");
		
		this.setEGLContextClientVersion(2);
	}
	public void setSandCastleRenderer(Renderer renderer) {
		this.setRenderer(renderer);
	}
	public void setInputEngine(InputEngine input) {
		this.setOnTouchListener(new EngineTouchListener(input));
	}
	
}