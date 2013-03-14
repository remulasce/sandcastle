package com.remulasce.sandcastle.input;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class EngineTouchListener implements OnTouchListener {

	InputEngine engine;
	
	public EngineTouchListener(InputEngine engine) {
		super();
		this.engine = engine;
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		
		int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
        final int index = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) 	///This is indeed the index, notID,
                >> MotionEvent.ACTION_POINTER_ID_SHIFT;										//because i haven't bothered to update Android
		switch(actionCode) {
		
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			engine.touchDown((int)event.getX(index), (int)event.getY(index), event.getPointerId(index), 21); //Matches LIBGDX  left mouse button.
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_OUTSIDE:
		case MotionEvent.ACTION_CANCEL:
			engine.touchUp((int)event.getX(index), (int)event.getY(index), event.getPointerId(index), 22);
			break;
		case MotionEvent.ACTION_MOVE:
			int pointerCount = event.getPointerCount();
			for (int ii = 0; ii < pointerCount; ii++) {
				engine.touchDragged((int)event.getX(ii), (int)event.getY(ii), event.getPointerId(ii));
			}
			break;
		
		}
		return true;
	}

}
