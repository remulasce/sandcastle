package com.remulasce.sandcastle;
import android.util.Log;

public class C {

	
	private static final boolean DEBUG_LOG = true;
	
	public static int SCR_WIDTH		= 0;
	public static int SCR_HEIGHT	= 0;
	
	
	public static void log(String message) {
		if (DEBUG_LOG) {
			Log.d("FastSpace", message);
		}
	}
}
