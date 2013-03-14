package com.remulasce.sandcastle.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;

import com.remulasce.sandcastle.C;
import com.remulasce.sandcastle.game.Physics.EventType;
import com.remulasce.sandcastle.game.objects.Block;
import com.remulasce.sandcastle.graphics.Camera;
import com.remulasce.sandcastle.input.Joystick;

public class World {

	public static final float WORLD_EDGE	= 100f;
	
	public Semaphore lock = new Semaphore(1, true);
	public Block selectBlock;
	
	//Ok yeah, it's public, but that doesn't mean you should use it directly.
	public ArrayList<Block> blocks		= new ArrayList<Block>();
//	public ArrayList<Block> supports	= new ArrayList<Block>();
	//No longer supports, just a single big Ground block
	//The blocks on the ground, where all load ends.
	//Falling blocks, which should check for reconnection
	public ArrayList<Block> falling		= new ArrayList<Block>();
	public ArrayList<Block> disGrated	= new ArrayList<Block>();
	//Disintegrated blocks, which should not support weight.
	
	//Blocks that should need to apply load & support weight. Regular blocks.
	public ArrayList<Block> statics		= new ArrayList<Block>();
	
	//Camera is maintained as part of world.
	public Camera camera	= new Camera();
	public Physics physics	= new Physics(this);
	public Joystick	joyL	= new Joystick();
	public Joystick	joyR	= new Joystick();
	
	//Amount of time, in seconds, this world has been played.
	public float gameTime = 0;
	
	/** For development, load a simple test world */
	public void loadTest() {
		C.log("CReatiNG Tests ===================");
		
		
		camera.reset();
		
		
//		makeTestStack();
//		makeTestCastle();
//		makeTestBridge();
//		makeFpsTest();
		makeTestTest();
		makeCursorBlock();
		
		
		
		physics.executeEventQueue();
	}
	private void makeCursorBlock() {
		selectBlock = new Block();
	}
	
	private void makeFpsTest() {
		for (int zz = 0; zz <= 2; zz++) {
			for (int ii=-3; ii<4; ii++) {
				Block noo = new Block();
				noo.x = ii;
				noo.y = 3;
				noo.z = zz;
				addBlock(noo);
				noo = new Block();
				noo.x = ii;
				noo.y = -3;
				noo.z = zz;
				addBlock(noo);
			}
			
			for (int ii=-2; ii < 3; ii++) {
				Block noo = new Block();
				noo.y = ii;
				noo.x = 3;
				noo.z = zz;
				addBlock(noo);
				noo = new Block();
				noo.y = ii;
				noo.x = -3;
				noo.z = zz;
				addBlock(noo);
			}
			
		}
		
	}
	
	private void makeTestTest() {
		float[] tests = {};/*
				-2, 0, 0,
				-2, 0, 1,
				-1, 0, 1,
				1, 0, 1,
				2, 0, 1,
				2, 0, 0,
				
		};*/
		makeLevel(tests);
	}

	private void makeTestStack() {
		float[] tests = {
				0,0,0,
				0,0,2,
				0,0,4,
				0,0,6,
				0,0,8,
		};
		
		makeLevel(tests);
	}
	
	private void makeTestBridge() {
		float[] tests = {
				-1,0,0,
				1,0,0,
				1,0,1,
				-1,0,1,
				-1,0,2,
				1,0,2,
				0,0,2,
		};
		
		makeLevel(tests);
		
	}

	private void makeLevel(float[] coords) {
		for (int ii=0; ii<coords.length/3; ii++) {
			makeTestBlock(coords[3*ii],coords[3*ii+1],coords[3*ii+2]);
		}
		
	}
	private void makeTestBlock(float x, float y, float z) {
		Block noo = new Block();
		noo.x = x; noo.y = y; noo.z = z;
		addBlock(noo);
	}
	
	private void makeTestCastle() {
		for (int zz = 0; zz <= 6; zz++) {
			for (int ii=-6; ii<7; ii++) {
				Block noo = new Block();
				noo.x = ii;
				noo.y = 6;
				noo.z = zz*2;
				addBlock(noo);
				noo = new Block();
				noo.x = ii;
				noo.y = -6;
				noo.z = zz*2;
				addBlock(noo);
			}
			
			for (int ii=-5; ii < 6; ii++) {
				Block noo = new Block();
				noo.y = ii;
				noo.x = 6;
				noo.z = zz*2;
				addBlock(noo);
				noo = new Block();
				noo.y = ii;
				noo.x = -6;
				noo.z = zz*2;
				addBlock(noo);
			}
			
		}
	}
	
	public void addBlock(Block add) {
		blocks	.add(add);
		statics	.add(add);
		physics.addEvent(physics.new GenericBlockEvent(EventType.BLOCK_ADDED, add));
	}
	
	/** Take blocks out of the static list and add to falling list */
	public void fell(Collection<Block> fell) {
		falling.addAll		(fell);
		statics.removeAll	(fell);
//		supports.removeAll	(fell);
	}
	/** Take blocks out of the static list and add to disintegrated list */
	public void disintegrated(Collection<Block> disGrated) {
		disGrated.addAll	(disGrated);
		statics.removeAll	(disGrated);
//		supports.removeAll	(disGrated);
	}
	/** Block reformed into the castle, should be static and not fell/other */
	public void reform(Collection<Block> reformed) {
		statics		.addAll(reformed);
		disGrated	.removeAll(reformed);
		falling		.removeAll(reformed);
		
	}
	
	

	
	public void removeBlocks(Collection<Block> rem) {
		blocks.removeAll(rem);
	}
	
}
