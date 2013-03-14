package com.remulasce.sandcastle.game;

import java.util.ArrayList;

import android.util.FloatMath;

import com.remulasce.sandcastle.C;
import com.remulasce.sandcastle.game.Physics.EventType;
import com.remulasce.sandcastle.game.objects.Block;
import com.remulasce.sandcastle.input.InputEngine;
import com.remulasce.sandcastle.input.TouchPoint;

public class Simulation {

	private static final boolean	LOG_UPDATE_TIME			= false;
	private static final boolean	LOG_OBJECT_PICKING		= false;
	private static final boolean	LOG_BLOCK_ACTIONS		= true;
	private static final boolean	LOG_CAMERA				= true;
	private static final long		DEBUG_UPDATE_INTERVAL	= 5000;	//ms
	private long					lastDebugDisp			= 0;
	
	
	//Fastest a tick can follow a preceeding tick, ms. 1000/x = ticks/s
	private static final long		MIN_SIM_TIME			= 10;
	
	private World		world;
	private InputEngine	input;

	private boolean cursorDirty	= true;	//Whether the cursor is on a new block now
	private float curX,curY,curZ;	//Where the cursor is currently resting
	
	//Input stuff:
	ArrayList<Block> newBlocks = new ArrayList<Block>();
	
	
	
	private Thread		simThread;
	private boolean		run		= false;
	
	public Simulation() {
		
	}
	
	public void setWorld(World world) {
		this.world = world;
	}
	public void setInputEngine(InputEngine input) {
		this.input = input;
	}
	
	
	public void start() {
		if (!run) {
			C.log("Simulation start");
			run			= true;
			simThread	= new Thread(new SimulationThread());
			simThread	.start();
			lastDebugDisp	= System.currentTimeMillis();
		}
		else {
			C.log("Warning: Tried to start a started simulation");
		}
	}
	
	public void stop() {
		if (run) {
			C.log("Simulation set stop");
			run			= false;
			simThread	= null;
		}
		else {
			C.log("Warning: Tried to stop a stopped simulation");
		}
	}
	//Final, memory-freeing gesture. Doesn't really do anything atm
	public void kill() {
		run = false;
	}
	
	
	
	
	private class SimulationThread implements Runnable {

		private long lastUpdate;	//UTC Last time sim was ticked. In millis.
		private float d;			//Current time since last simulation tick. in S.
		
		
		@Override
		public void run() {
			lastUpdate = System.currentTimeMillis();
			while (run) {
				try {
					world.lock.acquire();

					updateClock();
					
					//stuff
					
					doInput();
					
					doEvents();
					
					doUpdate();
					
					
					world.lock.release();

					doLogging();
					
					sleepTillNext();
					
					
				} catch (InterruptedException e ) {
					C.log("Bad simulation sleep");
				}
			}
			C.log("Simulation ending");
		}
		
		private void doUpdate() {
			for (Block each : world.blocks) {
				each.update(d);
			}
		}
		
		private void doInput() {
			input.prep();
			
			world.joyL.handleInput(input);
			world.joyR.handleInput(input);
			
			if (world.joyL.dx != 0 || world.joyL.dy != 0) {
				world.camera.move(6*world.joyL.dx * d, 6*world.joyL.dy * d, 0);
				cursorDirty	= true;
			}
			if (world.joyR.dx != 0 || world.joyR.dy != 0) {
				world.camera.rotateUp		(60*world.joyR.dy*d);
				world.camera.rotateRight	(60*world.joyR.dx*d);
				cursorDirty = true;
			}
			
			for (TouchPoint each : input.getNewTouches()) {
				if (Math.abs(each.x()-C.SCR_WIDTH/2) < 50
						&& Math.abs(each.y()-(C.SCR_HEIGHT-100)) < 50) {
					logBlockActions("Block placed: ("+curX+","+curY+","+curZ+")");
					Block noo = new Block();
					noo.x = curX; noo.y = curY; noo.z = curZ;
					world.addBlock(noo);
					world.physics.executeEventQueue();
					cursorDirty = true;
				}
			}
			
			
			
			if (cursorDirty) {
				calculateCursor();
				cursorDirty = false;
			}
			
			
			input.reset();
		}
		
		
		private void calculateCursor() {
			//Start from the coordinate of the camera, if the next unit in
			//	the viewline is a block, we're selecting the spcae before that block
			
			
			
			//Current known good block, if it was rounded
			float tx, ty, tz;
			//Amount to change by each step.
			float dx, dy, dz;
			//How far down the line we've gone.
			float d = 0;
			
			//The rounded, integer current/final coordinates 
			float fx, fy, fz;
			/* D drives tx, ty, and tz via dx, dy, and dz. Every step of d
			 * should land inside the next adjacent block of the viewline,
			 * so to avoid skipping, we calculate the amount of d to add
			 * by seeing which t would first be "flipped" into the next
			 * integer, with help from dxyz.  
			 */

			
			//static type camera position
			final float cx = world.camera.getX();
			final float cy = world.camera.getY();
			final float cz = world.camera.getZ();

			//TX: Current testing position
			//FX: Current integer final position
			//CX: The camera position
			//DX: Speed of change of each component over D
			tx = cx; ty = cy; tz = cz;

			fx = Math.round(tx);
			fy = Math.round(ty);
			fz = Math.round(tz);

			dz = -FloatMath.cos((float)Math.toRadians(world.camera.getAttitude()));
			//Amount the line deviates from straight up and down per step
			float side	= FloatMath.sin((float)Math.toRadians(world.camera.getAttitude()));
			
			dx = 	-(float)(side * Math.sin(Math.toRadians(world.camera.getRot())));
			dy = 	(float)(side * Math.cos(Math.toRadians(world.camera.getRot())));
			
			
			
			boolean done = false;
			while (!done) {
				//How many d steps each axis would take to get to a new block

//				logObjectPicking("Testing: ("+tx+", "+ty+", "+tz+")");
				
				float nextX		= Math.round(tx + 0.51 * Math.signum(dx) );
				float nextY		= Math.round(ty + 0.51 * Math.signum(dy) );
				float nextZ		= Math.round(tz + 0.51 * Math.signum(dz) );
				
				float timeX = (nextX - tx) / dx;
				float timeY = (nextY - ty) / dy;
				float timeZ = (nextZ - tz) / dz;
				
				
				
				//If we're going to cross te a new X block first
				if (((timeX < timeY) || isntReal(timeY)) && ((timeX < timeZ) || isntReal(timeZ)) && !isntReal(timeX)) {
					nextY = fy;
					nextZ = fz;
					d += timeX;
				} else
				//Y first
				if (((timeY < timeX) || isntReal(timeX)) && ((timeY < timeZ) || isntReal(timeZ)) && !isntReal(timeY)) {
					nextX = fx;
					nextZ = fz;
					d += timeY;
				} else
				// Z first
				if (((timeZ < timeX) || isntReal(timeX)) && ((timeZ < timeY) || isntReal(timeY)) && !isntReal(timeZ)) {
					//Set the next test block to the next coordinates
					nextX = fx;
					nextY = fy;
					//nextZ is already set
					//For next round, this si how far the line is extended
					d += timeZ;
				} else {
					//Problem, but let's just ignore it
					logObjectPicking("Problem with NaNs");
					break;
				}
				
				
//				logObjectPicking("Testing against blocks: ("+nextX+", "+nextY+", "+nextZ+")");
				//Test whether the increase fonud us a block
				for (Block each : world.blocks) {
					if (
							Float.compare(nextX, each.x) == 0 &&
							Float.compare(nextY, each.y) == 0 &&
							Float.compare(nextZ, each.z) == 0) {
						
						//We found a block, use the previous values
						//... which are still in f etc
						done = true;
						break;
					}
				}
				//Or if we just hit ground.
				if (nextZ <= -1 || Math.abs(nextZ) > World.WORLD_EDGE || Math.abs(nextX) > World.WORLD_EDGE || Math.abs(nextY) > World.WORLD_EDGE) {
//					C.log("Value floated out ("+nextX+", "+nextY+", "+nextZ+")");
					done = true;
				}
				
				if (done) {
					break;
				}
				
				
				//Extend the testing position down the line for next pass
				tx = cx + dx * d;
				ty = cy + dy * d;
				tz = cz + dz * d;
				
				fx = nextX;
				fy = nextY;
				fz = nextZ;
				
			}
			
			
			logObjectPicking("Selected square: ("+fx+", "+fy+", "+fz+")");
			
			curX = fx; curY = fy; curZ = fz;
			
			world.selectBlock.x = fx;
			world.selectBlock.y = fy;
			world.selectBlock.z = fz;
			
		}
		private boolean isntReal(float check) {
			if (Float.isNaN(check) || Float.isInfinite(check)) {
				return true;
			}
			return false;
		}
		
		/** Figure out if any simulation special events happened, which might
		 * need to be passed on to the physics engine. Eg. Block hits ground.
		 * Needs to be added to physics.
		 */
		private void doEvents() {
			ArrayList<Block> hits = new ArrayList<Block>();
			for (Block each : world.falling) {
				if (each.z <= 0) {
					hits.add(each);
					continue;
				}
				for (Block other : world.statics) {
					if (each == other) { C.log("Bad: Block was both static and falling "
							+each.dbug()+other.dbug()); }
					//If it hits a static object
					if (		each.x == other.x
							&& each.y == other.y
							&& Math.abs(each.z-other.z) <= 1) {
						hits.add(each);
					}
				}
			}
			
			world.reform(hits);
			for (Block each : hits) {
				each.falling = false;
				each.z = Math.round(each.z);
				world.physics.addEvent(
						world.physics.new GenericBlockEvent(
								EventType.BLOCK_ADDED, each
								)
						);
			}
			if (hits.size() != 0) {
				world.physics.executeEventQueue();
				//let's try this to make sure we get simultaneous events:
				doEvents();
			}
		}
		
		private void updateClock() {
			long t			= System.currentTimeMillis();
			d				= (t - lastUpdate) / 1000f;
			lastUpdate		= t;
		}
		
		private void sleepTillNext() {
			try {
				Thread.sleep(Math.max(0, MIN_SIM_TIME-(System.currentTimeMillis() - lastUpdate)));
			} catch (InterruptedException e) {
				C.log("Bad simulation sleep");
			}
		}
		
		private void doLogging() {
			if (LOG_UPDATE_TIME &&
					System.currentTimeMillis() > lastDebugDisp + DEBUG_UPDATE_INTERVAL) {
				
				long time		= System.currentTimeMillis();
				long frameTime	= time - lastUpdate;
				C.log("Simulation ftime ms: "+ frameTime);
				
				lastDebugDisp = time;
			}
		}
		
	}
	
	private static void logCamera(String message) {
		if (LOG_CAMERA) {
			C.log(message);
		}
	}
	private static void logObjectPicking(String message) {
		if (LOG_OBJECT_PICKING) {
			C.log(message);
		}
	}
	private static void logBlockActions(String message) {
		if (LOG_BLOCK_ACTIONS) {
			C.log(message);
		}
	}
}
