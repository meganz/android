package com.mega.android.pdfViewer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mega.android.R;

public class Options extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private final static String TAG = "com.mega.android.pdfViewer.Options";

	public final static String PREF_TAG = "Options";
	public final static String PREF_ZOOM_ANIMATION = "zoomAnimation";
	public final static String PREF_DIRS_FIRST = "dirsFirst";
	public final static String PREF_SHOW_EXTENSION = "showExtension";
	public final static String PREF_ORIENTATION = "orientation";
	public final static String PREF_FULLSCREEN = "fullscreen";
	public final static String PREF_PAGE_ANIMATION = "pageAnimation";
	public final static String PREF_FADE_SPEED = "fadeSpeed";
	public final static String PREF_RENDER_AHEAD = "renderAhead";
	public final static String PREF_COLOR_MODE = "colorMode";
	public final static String PREF_OMIT_IMAGES = "omitImages";
	public final static String PREF_VERTICAL_SCROLL_LOCK = "verticalScrollLock";
	public final static String PREF_BOX = "boxType";
	public final static String PREF_SIDE_MARGINS = "sideMargins2"; // sideMargins was boolean
	public final static String PREF_TOP_MARGIN = "topMargin";
	public final static String PREF_EXTRA_CACHE = "extraCache";
	public final static String PREF_DOUBLE_TAP = "doubleTap";
	public final static String PREF_VOLUME_PAIR = "volumePair";
	public final static String PREF_ZOOM_PAIR = "zoomPair";
	public final static String PREF_LONG_ZOOM_PAIR = "longZoomPair";
	public final static String PREF_UP_DOWN_PAIR = "upDownPair";
	public final static String PREF_LEFT_RIGHT_PAIR = "leftRightPair";
	public final static String PREF_RIGHT_UP_DOWN_PAIR = "rightUpDownPair";
	public final static String PREF_EINK = "eink";
	public final static String PREF_NOOK2 = "nook2";
	public final static String PREF_KEEP_ON = "keepOn";
	public final static String PREF_SHOW_ZOOM_ON_SCROLL = "showZoomOnScroll";
	public final static String PREF_HISTORY = "history";
	public final static String PREF_TOP_BOTTOM_TAP_PAIR = "topBottomTapPair";
	public final static String PREF_PREV_ORIENTATION = "prevOrientation";
	
	public final static int PAGE_NUMBER_DISABLED = 100;
	public final static int ZOOM_BUTTONS_DISABLED = 100;
	
	public final static int DOUBLE_TAP_NONE = 0;
	public final static int DOUBLE_TAP_ZOOM = 1;
	public final static int DOUBLE_TAP_ZOOM_IN_OUT = 2;
	
	public final static int PAIR_NONE = 0;
	public final static int PAIR_SCREEN = 1;
	public final static int PAIR_PAGE = 2;
	public final static int PAIR_ZOOM_1020 = 3;
	public final static int PAIR_ZOOM_1050 = 4;
	public final static int PAIR_ZOOM_1100 = 5;
	public final static int PAIR_ZOOM_1200 = 6;
	public final static int PAIR_ZOOM_1414 = 7;
	public final static int PAIR_ZOOM_2000 = 8;
	public final static int PAIR_PAGE_TOP = 9;
	public final static int PAIR_SCREEN_REV = 10;
	public final static int PAIR_PAGE_REV = 11;
	public final static int PAIR_PAGE_TOP_REV = 12;
	
	public final static int COLOR_MODE_NORMAL = 0;
	public final static int COLOR_MODE_INVERT = 1;
	public final static int COLOR_MODE_GRAY = 2;
	public final static int COLOR_MODE_INVERT_GRAY = 3;
	public final static int COLOR_MODE_BLACK_ON_YELLOWISH = 4;
	public final static int COLOR_MODE_GREEN_ON_BLACK = 5;
	public final static int COLOR_MODE_RED_ON_BLACK = 6;
	private final static int[] foreColors = { 
		Color.BLACK, Color.WHITE, Color.BLACK, Color.WHITE,
		Color.BLACK, Color.GREEN, Color.RED };
	private final static int[] backColors = {
		Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK,
		Color.rgb(239, 219, 189),
		Color.BLACK, Color.BLACK };
	
	private static final float[][] colorMatrices = {
		null, /* COLOR_MODE_NORMAL */
		
		{-1.0f,  0.0f,   0.0f,   0.0f, 255.0f, /* COLOR_MODE_INVERT */
		  0.0f, -1.0f,   0.0f,   0.0f, 255.0f,
		  0.0f,  0.0f,  -1.0f,   0.0f, 255.0f,
		  0.0f,  0.0f,   0.0f,   0.0f, 255.0f}, 
		
		{0.3f, 0.59f, 0.11f, 0.0f,   0.0f, /* COLOR_MODE_GRAY */
		 0.3f, 0.59f, 0.11f, 0.0f,   0.0f,
		 0.3f, 0.59f, 0.11f, 0.0f,   0.0f,
		 0.0f, 0.0f,  0.0f,  0.0f, 255.0f},
		
		{-0.3f, -0.59f, -0.11f, 0.0f, 255.0f, /* COLOR_MODE_INVERT_GRAY */
		 -0.3f, -0.59f, -0.11f, 0.0f, 255.0f,
		 -0.3f, -0.59f, -0.11f, 0.0f, 255.0f,
		  0.0f,  0.0f,   0.0f,  0.0f, 255.0f}, 

		{0.94f, 0.02f, 0.02f, 0.0f, 0.0f, /* COLOR_MODE_BLACK_ON_YELLOWISH */
		 0.02f, 0.86f, 0.02f, 0.0f, 0.0f,
		 0.02f, 0.02f, 0.74f, 0.0f, 0.0f,
		 0.0f,  0.0f,  0.0f,  1.0f, 0.0f},
		
		{ 0.0f,  0.0f,   0.0f,  0.0f,   0.0f, /* COLOR_MODE_GREEN_ON_BLACK */
		 -0.3f, -0.59f, -0.11f, 0.0f, 255.0f,
		  0.0f,  0.0f,   0.0f,  0.0f,   0.0f,
		  0.0f,  0.0f,   0.0f,  1.0f,   0.0f}, 

		{-0.3f, -0.59f, -0.11f, 0.0f, 255.0f, /* COLOR_MODE_RED_ON_BLACK */
		 0.0f,   0.0f,   0.0f,  0.0f,   0.0f,
		 0.0f,   0.0f,   0.0f,  0.0f,   0.0f,
		 0.0f,   0.0f,   0.0f,  1.0f, 255.0f} 
	};
	
	private Resources resources;
	
	private static final String[] summaryKeys = { PREF_ZOOM_ANIMATION, PREF_ORIENTATION, PREF_PAGE_ANIMATION,
		PREF_FADE_SPEED, PREF_COLOR_MODE, PREF_BOX, PREF_SIDE_MARGINS, PREF_TOP_MARGIN,
		PREF_EXTRA_CACHE, PREF_DOUBLE_TAP, PREF_VOLUME_PAIR, PREF_ZOOM_PAIR,
		PREF_LONG_ZOOM_PAIR, PREF_UP_DOWN_PAIR, PREF_LEFT_RIGHT_PAIR, PREF_RIGHT_UP_DOWN_PAIR,
		PREF_TOP_BOTTOM_TAP_PAIR };
	private static final int[] summaryEntryValues = { R.array.zoom_animations, R.array.orientations, R.array.page_animations,
		R.array.fade_speeds, R.array.color_modes, R.array.boxes, R.array.margins, R.array.margins,
		R.array.extra_caches, R.array.double_tap_actions, R.array.action_pairs, R.array.action_pairs,
		R.array.action_pairs, R.array.action_pairs, R.array.action_pairs, R.array.action_pairs, R.array.action_pairs };
	private static final int[] summaryEntries = { R.array.zoom_animation_labels, R.array.orientation_labels, R.array.page_animation_labels,
		R.array.fade_speed_labels, R.array.color_mode_labels, R.array.box_labels, R.array.margin_labels, R.array.margin_labels,
		R.array.extra_cache_labels, R.array.double_tap_action_labels, R.array.action_pair_labels, R.array.action_pair_labels,
		R.array.action_pair_labels, R.array.action_pair_labels, R.array.action_pair_labels, R.array.action_pair_labels, R.array.action_pair_labels };
	private static final int[] summaryDefaults = { R.string.default_zoom_animation, R.string.default_orientation, R.string.default_page_animation,
		R.string.default_fade_speed, R.string.default_color_mode, R.string.default_box, R.string.default_side_margin, R.string.default_top_margin,
		R.string.default_extra_cache, R.string.default_double_tap_action, R.string.default_volume_pair, R.string.default_zoom_pair,
		R.string.default_long_zoom_pair, R.string.default_up_down_pair, R.string.default_left_right_pair, R.string.default_right_up_down_pair, R.string.default_top_bottom_tap_pair };

	public String getString(SharedPreferences options, String key) {
		return getString(this.resources, options, key);
	}
	
	public static String getString(Resources resources, SharedPreferences options, String key) {
		for (int i=0; i<summaryKeys.length; i++)
			if (summaryKeys[i].equals(key)) 
				return options.getString(key, resources.getString(summaryDefaults[i]));
		return options.getString(key, "");
	}

	public void setSummaries() {
		for (int i=0; i<summaryKeys.length; i++) {
			setSummary(i);
		}
	}

	public void setSummary(String key) {
		for (int i=0; i<summaryKeys.length; i++) {
			if (summaryKeys[i].equals(key)) {
				setSummary(i);
				return;
			}
		}
	}
	
	public void setSummary(int i) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);
		
		Preference pref = findPreference(summaryKeys[i]);
		String value = options.getString(summaryKeys[i], resources.getString(summaryDefaults[i]));
		
		String[] valueArray = resources.getStringArray(summaryEntryValues[i]);
		String[] entryArray = resources.getStringArray(summaryEntries[i]);
		
		for (int j=0; j<valueArray.length; j++) 
			if (valueArray[j].equals(value)) {
				pref.setSummary(entryArray[j]);
				return;
			}
	}
	
	public static int getIntFromString(SharedPreferences pref, String option, int def) {
		return Integer.parseInt(pref.getString(option, ""+def));	
	}

	public static float[] getColorModeMatrix(int colorMode) {
		return colorMatrices[colorMode];
	}
	
	public static int getForeColor(int colorMode) {
		return foreColors[colorMode];
	}
	
	public static int getBackColor(int colorMode) {
		return backColors[colorMode];
	}
	
	public static int getColorMode(SharedPreferences pref) {
		return getIntFromString(pref, PREF_COLOR_MODE, 0);
	}
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		this.resources = getResources();
		
		addPreferencesFromResource(R.xml.options);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		setOrientation(this);

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		setSummaries();
	}
	
	
	
	/* returns true when the calling app is responsible for monitoring */
	public static boolean setOrientation(Activity activity) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(activity);
		int orientation = Integer.parseInt(options.getString(PREF_ORIENTATION, "0"));
		switch(orientation) {
		case 0: 
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			break;
		case 1:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case 2:
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;			
		case 3:
			int prev = options.getInt(Options.PREF_PREV_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			Log.v(TAG, "restoring orientation: "+prev);
			activity.setRequestedOrientation(prev);
			return true;
		default:
			break;
		}
		return false;
	}

	public void onSharedPreferenceChanged(SharedPreferences options, String key) {
		setSummary(key);
	}
}
