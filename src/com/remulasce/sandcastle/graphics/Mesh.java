package com.remulasce.sandcastle.graphics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.content.Context;

public class Mesh {
	int meshID;
	
	public float vertices[];
	public float normals[];
	public float colors[];
	public float texCoords[];
	public short indices[];
	
	public FloatBuffer vb;
	public FloatBuffer cb;
	public FloatBuffer nb;
	public FloatBuffer tb;
	public ShortBuffer ib;
	
	
	public void allocateVertex() {
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vb = vbb.asFloatBuffer();
		vb.put(vertices);
		vb.position(0);
	}
	
	public void allocateTexture() {
		ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
		tbb.order(ByteOrder.nativeOrder());
		tb = tbb.asFloatBuffer();
		tb.put(texCoords);
		tb.position(0);
	}
	
	public void allocateColor() {
		ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
		cbb.order(ByteOrder.nativeOrder());
		cb = cbb.asFloatBuffer();
		cb.put(colors);
		cb.position(0);
	}
	
	public void allocateNormal() {
		ByteBuffer nbb = ByteBuffer.allocateDirect(normals.length * 4);
		nbb.order(ByteOrder.nativeOrder());
		nb = nbb.asFloatBuffer();
		nb.put(normals);
		nb.position(0);
	}
	
	public void allocateIndex() {
		ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
		ibb.order(ByteOrder.nativeOrder());
		ib = ibb.asShortBuffer();
		ib.put(indices);
		ib.position(0);
	}

	
	/** Load geometry from a .obj file in the res/raw file, via its resource id,
	 * and store the geometry into the provided Mesh.
	 * 
	 * Currently only loads position and normal data. Allocates index, buffer, normal, tex.
	 * Index is used, but it goes 012345.... because of complications in .obj files.
	 * So identical positios/uv/normal vertexes will be duplicated if used more than once.
	 * 
	 * @param resId the Android res id of the mesh we want to load
	 */
	public static void loadMesh(Mesh dst, Context context, int resId) {
		InputStream inputStream = context.getResources().openRawResource(resId);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		String line;
		
		// Temporary lists of all possible xs. We'll have to combine them later,
		// because vertexes are formed with combinations of them defined by the faces,
		// and the faces try to reuse existing portions, which is NO.
		ArrayList<Float> pv = new ArrayList<Float>();	//Possible vertex positions
		ArrayList<Float> pt = new ArrayList<Float>();	//Possible uv positions. Unused currently.
		ArrayList<Float> pn = new ArrayList<Float>();	//Possible vertex normals
		
		// Final lists of things, in correct index order, before they get put into arrays.
		ArrayList<Float> fv = new ArrayList<Float>();
		ArrayList<Float> ft = new ArrayList<Float>();
		ArrayList<Float> fn = new ArrayList<Float>();
		//This one is just straight up indices.
		ArrayList<Short> i = new ArrayList<Short>();
		try {
			while((line = reader.readLine()) != null) {
				//Get all possible combinations
				if (line.startsWith("v ")) {
					processVertex(pv, line);
				}
				if (line.startsWith("vn ")) {
					processNormal(pn, line);
				}
				if (line.startsWith("vt ")) {
					processTexture(pt, line);
				}
				//Faces immediately create the finals, since we should already know
				//enough whenever we come across one.
				//We only support tri-faces.
				if (line.startsWith("f ")) {
					processFace(i, line, pv, pt, pn, fv, ft, fn);
				}
				
			}
			
			//messily convert the arraylists to the actual arrays
			dst.vertices = new float[fv.size()];
			for (int ii = 0; ii < fv.size(); ii++) {
				//hope none of the arraylist Floats are null
				dst.vertices[ii] = fv.get(ii);
			}
			dst.allocateVertex();
			
			dst.texCoords = new float[ft.size()];
			for (int ii = 0; ii < ft.size(); ii++) {
				dst.texCoords[ii] = ft.get(ii);
			}
			dst.allocateTexture();
			
			dst.normals = new float[fn.size()];
			for (int ii = 0; ii < fn.size(); ii++) {
				dst.normals[ii] = fn.get(ii);
			}
			dst.allocateNormal();
			
			dst.indices = new short[i.size()];
			for (int ii = 0; ii < i.size(); ii++) {
				//hope none of the arraylist Shorts are null
				dst.indices[ii] = i.get(ii);
			}
			dst.allocateIndex();
			
			
		} catch (IOException e) {e.printStackTrace();}
		
	}
	/** Assume format: v xxx yyy zzz
	 */
	private static void processVertex(ArrayList<Float> v, String line) {
		String[] components = line.split(" ");
		//start at 1 to ignore the v
		for (int ii = 1; ii <= 3; ii++) {
			v.add(Float.parseFloat(components[ii]));
		}
	}
	private static void processTexture(ArrayList<Float> t, String line) {
		String[] components = line.split(" ");
		for (int ii = 1; ii <= 2; ii++) {
			t.add(Float.parseFloat(components[ii]));
		}
	}
	private static void processNormal(ArrayList<Float> n, String line) {
		String[] components = line.split(" ");
		for (int ii = 1; ii <= 3; ii++) {
			n.add(Float.parseFloat(components[ii]));
		}
	}
	/** Assume format: f xxx/aou yyy/;oeu zzz/o.u
	 * where xxx, yyy, zzz are the indices, and /oeau is anything.
	 * the /aoeu can include an additional /aue. We don't care.
	 */
	private static void processFace(ArrayList<Short> i, String line,
			ArrayList<Float> pv, ArrayList<Float> pt, ArrayList<Float> pn,
			ArrayList<Float> fv, ArrayList<Float> ft, ArrayList<Float> fn) {
		String[] components = line.split(" ");
		//For each point in the face
		for (int ii = 1; ii <= 3; ii++) {
			//get just the first number, which is the vertex #.
			//pos [0], uv [1], norm[2]
			String[] parts = components[ii].split("/");
//			i.add((short) (Short.parseShort(parts[0])-1));
			
			//Position
			//Second time in 2 days screwed by .obj index start
			fv.add(pv.get(3*(Short.parseShort(parts[0])-1)+0));
			fv.add(pv.get(3*(Short.parseShort(parts[0])-1)+1));
			fv.add(pv.get(3*(Short.parseShort(parts[0])-1)+2));
			//Texture
			ft.add(pt.get(2*(Short.parseShort(parts[1])-1)+0));
			ft.add(pt.get(2*(Short.parseShort(parts[1])-1)+1));
			//Normal
			fn.add(pn.get(3*(Short.parseShort(parts[2])-1)+0));
			fn.add(pn.get(3*(Short.parseShort(parts[2])-1)+1));
			fn.add(pn.get(3*(Short.parseShort(parts[2])-1)+2));
			
			//Index
			//not done
			
		}
	}
	
}
