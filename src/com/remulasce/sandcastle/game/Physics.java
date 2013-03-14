package com.remulasce.sandcastle.game;

import java.util.ArrayList;

import com.remulasce.sandcastle.C;
import com.remulasce.sandcastle.game.objects.Block;
import com.remulasce.sandcastle.game.objects.Block.Face;
import com.remulasce.sandcastle.game.objects.Ground;

public class Physics {

	private static final boolean	LOG_PHYSICS	= true;
	
	
	private World world;
	//small hack- everybody on the ground connects to ground, which conveniently
	//	can withstand any sort of pressure.
	private Block ground;
	
	//We have a queue only for future multithreading excellence.
	//For now, the queue is executed immediately upon
	private ArrayList<PhysicsEvent> eventQueue = new ArrayList<PhysicsEvent>();
	
	//Hack. If we change something that requires the physics to tick again,
	//	just set this flag, I guess.
	private boolean flagDirty = false;
	
	public Physics(World world) {
		this.world	= world;
		this.ground = new Ground();
		this.ground.z = -1;
	}
	
	
	public void addEvent(PhysicsEvent event) {
		eventQueue.add(event);
	}
	
	
	/** Do ALL the events! In order, of course. Well, really at the same
	 * time, since they all do the same thing for now. Then kill them.
	 * This should usually be called immediately after addEvent, unless
	 * you're special and know that multiple events need to be executed
	 * simultaneously. Like, adding a 2x1 block */
	public void executeEventQueue() {
//		log("Executing queue of "+eventQueue.size());
		

		while (eventQueue.size() > 0 || flagDirty) {
			flagDirty = false;
			//////////////////////////
			//	Create face links
			////
			for (int ii=eventQueue.size()-1; ii>=0; ii--) {
				executeEvent(eventQueue.get(ii));
				eventQueue.remove(ii);
			}
			
			
			////////////////////////////
			//	Find load path
			////
			
			findLoadPaths();
			
			////////////////////////////
			//	Find face failures
			////
			driveLoad();
			ArrayList<Block> faceFails = new ArrayList<Block>();
			for (Block each : world.statics) {
				//Sketch, but needs big redesign of Block's role in checkstructure
				//	before fixing
				if (each.loadSlipped()) {
					each.disconnectAll();
					faceFails.add(each);
					blockFell(each);
				}
			}
			world.fell(faceFails);
			if (faceFails.size() > 0) {
				executeEventQueue();
			}
			
			//////////////////////////////
			//	Find block failures
			////
			driveLoad();
			ArrayList<Block> blockFails = new ArrayList<Block>();
			for (Block each : world.statics) {
				if (each.loadDisintegrated()) {
					each.disconnectAll();
					blockDisGrated(each);
					blockFails.add(each);
				}
			}
			world.disintegrated(blockFails);
			if (blockFails.size() > 0) {
				executeEventQueue();
			}
		}
		
	}
	
	private void executeEvent(PhysicsEvent event) {
		switch (event.type) {
		//umm?
		case NONE:
			log("WHY? PhysicsEventType NONE received");
			break;
		// If a block's added, it needs its faces, and others need it
		case BLOCK_ADDED:
			log("Physics event ADD "+event.dbug());
			blockAdded((GenericBlockEvent)event);
			break;
		// Disconnect a removed block from whatever it was connecting
		case BLOCK_REMOVED:
			log("Physics event REMOVE "+event.dbug());
			blockRemoved((GenericBlockEvent)event);
			break;
			
			
			
		}
	}
	
	////////////
	///   Add block
	private void blockAdded(GenericBlockEvent event) {
		connectNewBlock(event.block);
	}
	private void connectNewBlock(Block block) {
		//Connect block to adjacent blocks
		for (Block each : world.statics) {
			if (each == block) { continue; }
			if (each.x == block.x && each.y == block.y && each.z == block.z) {
				log("Cannot place block inside another ("+block.x+", "+block.y+")");
				return;
			}
			else {
				
				float dx = Math.abs(block.x - each.x);
				float dy = Math.abs(block.y - each.y);
				float dz = Math.abs(block.z - each.z);
				
				//If block is neighboring a block, then connect them
				 if (  dx <= 1
						&& dy <= 1
						&& dz <= 1
						&& dx+dy+dz == 1) { //last bit is for now, only adjacent.
					block.connect(each);
					each.connect(block);
				}
			}
		}
		//If it's zero, connect to the ground, too.
		if (block.z == 0) {
			block.connect(ground);
			ground.connect(block);
		}
	}
	
	///
	////////////
	/// Remove block
	
	private void blockRemoved(GenericBlockEvent event) {
		disconnectRemovedBlock(event.block);
	}
	private void disconnectRemovedBlock(Block block) {
		block.disconnectAll();
	}
	
	
	
	/** Determine the path that load takes to ground for all blocks 
	 * Relies upon the support list to be correct*/
	private void findLoadPaths() {
		
		for (Block each : world.statics) { each.resetLoadPath(); }
		
		//The current wave of blocks recently corrected for steps
		ArrayList<Block> activeBlocks	= new ArrayList<Block>();
		
		//The next wave of blocks, so we don't mess with actives midloop
		ArrayList<Block> nextActives	= new ArrayList<Block>();
		
		activeBlocks.add(ground);
		//So long as there's still blocks we haven't reached connected to support
		while (activeBlocks.size() > 0) {
			//Go to the most recently update blocks
			for (Block active : activeBlocks) {
				if (active.disintegrated || active.falling) { continue; }
				//and set its adjacent blocks to itself, if it's the closest.
				for (Face other : active.getFaces()) {
					//If this is the first time each has been connected, also add
					//	it to the next actives pile, to continue connecting next turn
					if (!other.connected.hasLoadPath()) {
						nextActives.add(other.connected);
					}
					other.connected.presentLoadPath(active);
				}
			}
			activeBlocks.clear();
			activeBlocks.addAll(nextActives);
			nextActives.clear();
		}
		for (Block each : world.statics) {
			each.finalizeLoadPath();
		}
	}
	/** Drive the load from all blocks down to ground, then check to see what broke.
	 * If anything broke, immediately do the physics activity associated with that,
	 * 	and run the physics again.
	 * 
	 * This way, anything that will fail fails immediately, instead of waiting for the next tick.
	 */
	private void driveLoad() {
		for (Block each : world.statics) {
			each.resetLoad();
		}
		for (Block each : world.statics) {
			each.driveWeight();
		}		
	}
	
	
	/** Convenience only, if block fell in tick, add to event queue */
	private void blockFell(Block ff) {
		log("Block fell "+ff.dbug());
		ff.falling = true;
		GenericBlockEvent event = new GenericBlockEvent(EventType.BLOCK_REMOVED, ff);
		addEvent(event);
	}
	/** Convenience only, if block disintegrated in tick, add to event queue */
	private void blockDisGrated(Block dd) {
		log("Block disgrated "+dd.dbug());
		dd.disintegrated = true;
		GenericBlockEvent event = new GenericBlockEvent(EventType.BLOCK_REMOVED, dd);
		addEvent(event);
	}
	
	
	enum EventType { NONE, BLOCK_ADDED, BLOCK_REMOVED }
	public abstract class PhysicsEvent {
		public EventType type = EventType.NONE;
		public abstract String dbug();
	}
	
	public class GenericBlockEvent extends PhysicsEvent {
		public Block block;	//The Block changed or important to this event
		public GenericBlockEvent(EventType type, Block affected) {
			this.type	= type;
			this.block	= affected;
		}
		@Override
		public String dbug() {
			return block.dbug();
		}
	}
	
	
	private static void log(String message) {
		if (LOG_PHYSICS) {
			C.log(message);
		}
	}
}
