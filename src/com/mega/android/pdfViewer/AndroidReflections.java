package com.mega.android.pdfViewer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.view.MotionEvent;

// #ifdef pro
// 
// 
// import android.util.Log;
// import android.view.View;
// #endif

/**
 * Find newer methods using reflection and call them if found.
 * @todo cache method
 * @todo check android runtime version insead of trying to access method by name
 * @todo reuse code
 */
final public class AndroidReflections {
	
// #ifdef pro
// 	private final static String TAG = "cx.hell.android.pdfview";
// 
// 	public static void setScrollbarFadingEnabled(View view, boolean fadeScrollbars) {
// 		Class<View> viewClass = View.class;
// 		Method sfeMethod = null;
// 		try {
// 			sfeMethod = viewClass.getMethod("setScrollbarFadingEnabled", boolean.class);
// 		} catch (NoSuchMethodException e) {
// 			// nwm
// 			Log.d(TAG, "View.setScrollbarFadingEnabled not found");
// 			return;
// 		}
// 		try {
// 			sfeMethod.invoke(view, fadeScrollbars);
// 		} catch (InvocationTargetException e) {
// 			/* should not throw anything according to Android Reference */
// 			/* TODO: ui error handling */
// 			throw new RuntimeException(e);
// 		} catch (IllegalAccessException e) {
// 			/* TODO: wat do? */
// 			Log.w(TAG, "View.setScrollbarFadingEnabled exists, but is not visible: " + e);
// 		}
// 	}
// #endif
	
	public static int getMotionEventPointerCount(MotionEvent motionEvent) {
		Class<MotionEvent> motionEventClass = MotionEvent.class;
		Method getPointerCountMethod = null;
		try {
			getPointerCountMethod = motionEventClass.getMethod("getPointerCount");
		} catch (NoSuchMethodException e) {
			return 0;
		}
		
		try {
			Integer r = (Integer)getPointerCountMethod.invoke(motionEvent);
			return r;
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static float getMotionEventX(MotionEvent motionEvent, int pointerIndex) {
		Class<MotionEvent> motionEventClass = MotionEvent.class;
		Method getXMethod = null;
		try {
			getXMethod = motionEventClass.getMethod("getX", int.class);
		} catch (NoSuchMethodException e) {
			return 0;
		}
		
		try {
			Float r = (Float)getXMethod.invoke(motionEvent, pointerIndex);
			return r;
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static float getMotionEventY(MotionEvent motionEvent, int pointerIndex) {
		Class<MotionEvent> motionEventClass = MotionEvent.class;
		Method getYMethod = null;
		try {
			getYMethod = motionEventClass.getMethod("getY", int.class);
		} catch (NoSuchMethodException e) {
			return 0;
		}
		
		try {
			Float r = (Float)getYMethod.invoke(motionEvent, pointerIndex);
			return r;
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
}
