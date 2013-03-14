package com.remulasce.sandcastle.game.objects;

import java.util.ArrayList;

import android.opengl.Matrix;

import com.remulasce.sandcastle.C;

public class Block extends Entity{

	//////==========Physics=========/////////
	protected float	weight;			//How much this weighs

	protected float	pressStr;		//How much this can be loaded before collapsing, incl itself.
	protected float	shearStr;		//How much load one side of this can hold.
	
	
	//////=========Simulation=======/////////
	public boolean	falling;		//Whether this is falling, and thus should not be simmed.
	public boolean	disintegrated;	//Whether this object can't support weight
	
	
	//////==========Support=========//////////
	//	Faces to all blocks adjacent to us
	protected ArrayList<Face>	faces		= new ArrayList<Face>();
	//	Faces to the blocks we drive our load to
	protected ArrayList<Face>	loadFaces	= new ArrayList<Face>();
	protected int				loadSteps	= -1;
	//	How far we are from ground.

	protected float	load;			//Simple: How much is trying to disintegrate this.
	
	
	
	public Block() {
		super();
		pressStr	= 3.76f;
		shearStr	= 1.8f;
		weight		= 1;
	}
	
	
	public void update(float d) {
		if (falling) {
			z -= d;
			if (z < 0) {
				z = 0;
			}
		}
	}
	
	
	
	
	
	public void connect(Block other) {
		faces.add(new Face(other));
	}
	/** Disconnect this block from the other block */
	public void disconnect(Block other) {
		Face f = getFace(other);
		if (f!=null) faces.remove(f);
	}
	/** Disconect this from all faces, and also disconnect everybody
	 * connected to this
	 */
	public void disconnectAll() {
		for (Face each : faces) {
			each.connected.disconnect(this);
		}
		faces.clear();
		resetLoadPath();
	}
	public Face getFace(Block connected) {
		for (Face each : faces) { 
			if (each.connected == connected) {
				return each;
			}
		}
		C.log(dbug()+" Could not find connecting face");
		return null;
	}
	/** Take load from a connected block and pass it down */
	public void receiveLoad(float amnt) {
		this.load += amnt;
		for (Face each : loadFaces) {
			each.applyForce(amnt);
		}
	}
	/** Drive our weight down the load path */
	public void driveWeight() {
		receiveLoad(weight);
	}
	/** Clear all load, for things */
	public void resetLoad() {
		load = 0;
		for (Face each : loadFaces) {
			each.resetLoad();
		}
	}
	/** Check if we face-slipped */
	public boolean loadSlipped() {
		if (!hasLoadPath()) { return true; }
		for (Face each : loadFaces) {
			if (each.shearFailed) {
				return true;
			}
		}
		return false;
	}
	/** Check to see if we got crushed */
	public boolean loadDisintegrated() {
		return load > pressStr;
	}
	
	/** Return whether this has any load paths at all. Usually for next active faces */
	public boolean hasLoadPath() {
		return loadFaces.size() > 0;
	}
	/** Clear the load path, in prep for a new one. Frees the loadFaces list
	 * for temporary load paths, until the path is finalized.
	 */
	public void resetLoadPath() {
		loadFaces . clear();
		loadSteps = -1;
	}
	/** Return the number of load steps this block has */
	public int getLoadSteps() {
		return loadSteps;
	}
	/** Ask us o consider a new load path! We have our own list of faces, but we consider
	 * them in this order because that way it's correct from ground up.
	 * Requirement: Path must already have a Face with us.
	 */
	public void presentLoadPath(Block path) {
		//Add it to the potential load use candidates
		loadFaces.add(getFace(path));
		//Update our loadsteps so the next guy knows his
		if (path.getLoadSteps()+1 < loadSteps || loadSteps == -1) {
			loadSteps = path.getLoadSteps()+1;
		}
	}
	/** Once everybody has presented load paths, choose the one/combination we'll
	 * actually put our load into.
	 */
	public void finalizeLoadPath() {
		int loads = 0; //An optimal path counts 2, sub 1
		for (int ii=loadFaces.size()-1; ii>=0; ii--) {
			//This is an optimal path
			if (loadFaces.get(ii).connected.getLoadSteps()+1 == loadSteps) {
				loads += 2;
			}
			//This is one less than optimal
			else if (loadFaces.get(ii).connected.getLoadSteps()+2 == loadSteps) {
				loads += 1;
			}
			//Not optimal at all, so remove this. That's why we use int i from back.
			else {
				loadFaces.remove(ii);
			}
		}
		//Now, we distribute the path. 
		for (Face each : loadFaces) {
			//Optimal faces  
			if (each.connected.getLoadSteps()+1 == loadSteps) {
				each.loadRatio = 2f/loads;
			}
			//Suboptimal faces
			else if (each.connected.getLoadSteps()+2 == loadSteps) {
				each.loadRatio = 1f/loads;
			}
			
		}
	}

	
	/////////Simulation methods////////////
	/** Physics says we're no longer supported, so fall */
	public void fall() {
		C.log(dbug()+" fell");
		falling = true;
	}
	/** Physics says we were disintegrated, so disintegrate. */
	public void disintegrate() {
		C.log(dbug()+" disintegrated");
		disintegrated = true;
	}
	
	
	/** Get all faces we have */
	public ArrayList<Face> getFaces() {
		return faces;
	}
	
	
	//A connection between two blocks
	public class Face {
		//The other block this face connects to, if any.
		public Block connected;
		//The direction this block faces, to determine direction of any force.
		public float nx, ny, nz; 
		
		private float load = 0;		// The load that was attempted to be transmitted
		//through this face. Reset at each update.
		
		private float loadRatio=1;
		private boolean shearFailed = false;
		
		
		public Face(Block other) {
			connected = other;
			nx = clamp1(other.x-x); ny = clamp1(other.y-y); nz = clamp1(other.z-z);
			if (other instanceof Ground) { nx=0;ny=0;nz=-1; } //hack, no slip ground
		}
		
		public float getAppliedForce(float amnt) {
			return  amnt * loadRatio;
		}
		
		public boolean shearFailed() {	return shearFailed;	 }
		
		/**Apply force from this object to the connected one.
		 * Takes loadRatio into account */
		public void applyForce(float amnt) {
			if (connected != null) {
				float applied = getAppliedForce(amnt);
				
				if (nz >= 0 && load + applied > shearStr) {
					C.log("Face took too much shear ("+(load+applied)+")");
					applied = shearStr - load;
					shearFailed = true;
				}
				load += applied;
				connected.receiveLoad(applied);
			}
		}
		public void resetLoad() {
			load = 0;
		}
	}

	/**Can't believe I have to do this. Make sure x falls in [-1, 1] */
	private static float clamp1(float x) {
		if (x > 1)	{ return 1;	}
		if (x < -1)	{ return -1;}
		return x;
	}
	/** Short identifying string to append to messages. Has location. */
	public String dbug() {	return "("+x+", "+y+", "+z+") ";	}
	
	@Override
	public void setTransform(float[] mMatrix) {
		Matrix.setIdentityM(mMatrix, 0);
		Matrix.translateM(mMatrix, 0, x, y, z);
		if (disintegrated) {
			Matrix.scaleM(mMatrix, 0, .5f, .5f, .5f);
		} else {
			float s = (load/2 + .5f*pressStr) / pressStr;
			//Matrix.scaleM(mMatrix, 0, 1, 1, s);
		}
	}
	
	
}