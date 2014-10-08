package com.mega.android.pdfViewer;

import android.content.SharedPreferences;
import android.view.KeyEvent;

public class Actions {
	public int zoom;
	public int longZoom;
	public int upDown;
	public int volume;
	public int leftRight;
	public int rightUpDown;
	public int topBottomTap;

	public static final int ZOOM_IN = 1000000;
	public static final int ZOOM_OUT = 1000001;
	public static final int LONG_ZOOM_IN = 1000002;
	public static final int LONG_ZOOM_OUT = 1000003;
	public static final int TOP_TAP = 1000004;
	public static final int BOTTOM_TAP = 1000005;
	
	public final static int ACTION_NONE = 0;
	public final static int ACTION_SCREEN_DOWN = 1;
	public final static int ACTION_SCREEN_UP = 2;
	public final static int ACTION_FULL_PAGE_DOWN = 3;
	public final static int ACTION_FULL_PAGE_UP = 4;
	public final static int ACTION_PREV_PAGE = 5;
	public final static int ACTION_NEXT_PAGE = 6;
	public final static int ACTION_ZOOM_IN_1020 = 7;
	public final static int ACTION_ZOOM_IN_1050 = 8;
	public final static int ACTION_ZOOM_IN_1100 = 9;
	public final static int ACTION_ZOOM_IN_1200 = 10;
	public final static int ACTION_ZOOM_IN_1414 = 11;
	public final static int ACTION_ZOOM_IN_2000 = 12;
	public final static int ACTION_ZOOM_OUT_1020 = 13;
	public final static int ACTION_ZOOM_OUT_1050 = 14;
	public final static int ACTION_ZOOM_OUT_1100 = 15;
	public final static int ACTION_ZOOM_OUT_1200 = 16;
	public final static int ACTION_ZOOM_OUT_1414 = 17;
	public final static int ACTION_ZOOM_OUT_2000 = 18;
	
	public Actions(SharedPreferences pref) {
		this.zoom = Integer.parseInt(pref.getString(Options.PREF_ZOOM_PAIR, ""+Options.PAIR_ZOOM_1414));
		this.longZoom = Integer.parseInt(pref.getString(Options.PREF_LONG_ZOOM_PAIR, ""+Options.PAIR_ZOOM_2000));
		this.upDown = Integer.parseInt(pref.getString(Options.PREF_UP_DOWN_PAIR, ""+Options.PAIR_SCREEN));
		this.volume = Integer.parseInt(pref.getString(Options.PREF_VOLUME_PAIR, ""+Options.PAIR_SCREEN));
		this.leftRight = Integer.parseInt(pref.getString(Options.PREF_UP_DOWN_PAIR, ""+Options.PAIR_PAGE));
		this.rightUpDown = Integer.parseInt(pref.getString(Options.PREF_RIGHT_UP_DOWN_PAIR, ""+Options.PAIR_SCREEN));
		this.topBottomTap = Integer.parseInt(pref.getString(Options.PREF_TOP_BOTTOM_TAP_PAIR, ""+Options.PAIR_NONE));
	}
	
	public static float getZoomValue(int action) {
		switch(action) {
		case ACTION_ZOOM_IN_1020:
			return 1f/1.02f;
		case ACTION_ZOOM_IN_1050:
			return 1f/1.05f;
		case ACTION_ZOOM_IN_1100:
			return 1f/1.1f;
		case ACTION_ZOOM_IN_1200:
			return 1f/1.2f;
		case ACTION_ZOOM_IN_1414:
			return 1f/1.414f;
		case ACTION_ZOOM_IN_2000:
			return 1f/1.414f;
		case ACTION_ZOOM_OUT_1020:
			return 1.02f;
		case ACTION_ZOOM_OUT_1050:
			return 1.05f;
		case ACTION_ZOOM_OUT_1100:
			return 1.1f;
		case ACTION_ZOOM_OUT_1200:
			return 1.2f;
		case ACTION_ZOOM_OUT_1414:
			return 1.414f;
		case ACTION_ZOOM_OUT_2000:
			return 1.414f;
		default:
			return -1f;
		}
	}
	
	public static int getAction(int pairAction, int item) {
		switch(pairAction) {
		case Options.PAIR_SCREEN:
			return item == 0 ? ACTION_SCREEN_UP : ACTION_SCREEN_DOWN;
		case Options.PAIR_PAGE:
			return item == 0 ? ACTION_FULL_PAGE_UP : ACTION_FULL_PAGE_DOWN;
		case Options.PAIR_PAGE_TOP:
			return item == 0 ? ACTION_PREV_PAGE : ACTION_NEXT_PAGE;
		case Options.PAIR_SCREEN_REV:
			return item == 1 ? ACTION_SCREEN_UP : ACTION_SCREEN_DOWN;
		case Options.PAIR_PAGE_REV:
			return item == 1 ? ACTION_FULL_PAGE_UP : ACTION_FULL_PAGE_DOWN;
		case Options.PAIR_PAGE_TOP_REV:
			return item == 1 ? ACTION_PREV_PAGE : ACTION_NEXT_PAGE;
		case Options.PAIR_ZOOM_1020:
			return item == 0 ? ACTION_ZOOM_OUT_1020 : ACTION_ZOOM_IN_1020;
		case Options.PAIR_ZOOM_1050:
			return item == 0 ? ACTION_ZOOM_OUT_1050 : ACTION_ZOOM_IN_1050;
		case Options.PAIR_ZOOM_1100:
			return item == 0 ? ACTION_ZOOM_OUT_1100 : ACTION_ZOOM_IN_1100;
		case Options.PAIR_ZOOM_1200:
			return item == 0 ? ACTION_ZOOM_OUT_1200 : ACTION_ZOOM_IN_1200;
		case Options.PAIR_ZOOM_1414:
			return item == 0 ? ACTION_ZOOM_OUT_1414 : ACTION_ZOOM_IN_1414;
		case Options.PAIR_ZOOM_2000:
			return item == 0 ? ACTION_ZOOM_OUT_2000 : ACTION_ZOOM_IN_2000;
		default:
			return ACTION_NONE;
		}
	}
	
	public int getAction(int key) {
		switch(key) {
		case TOP_TAP:
			return getAction(this.topBottomTap, 0);
		case BOTTOM_TAP:
			return getAction(this.topBottomTap, 1);
		case ZOOM_OUT:
			return getAction(this.zoom, 0);
		case ZOOM_IN:
			return getAction(this.zoom, 1);
		case LONG_ZOOM_OUT:
			return getAction(this.longZoom, 0);
		case LONG_ZOOM_IN:
			return getAction(this.longZoom, 1);
		case 94:
			return getAction(this.rightUpDown, 0);
		case 95:
			return getAction(this.rightUpDown, 1);
		case KeyEvent.KEYCODE_VOLUME_UP:
			return getAction(this.volume, 0);
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			return getAction(this.volume, 1);
		case 92:
		case KeyEvent.KEYCODE_DPAD_UP:
			return getAction(this.upDown, 0);
		case 93:
		case KeyEvent.KEYCODE_DPAD_DOWN:
			return getAction(this.upDown, 1);
		case KeyEvent.KEYCODE_DPAD_LEFT:
			return getAction(this.leftRight, 0);
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			return getAction(this.leftRight, 1);
		default:
			return ACTION_NONE;
		}
	}
}
