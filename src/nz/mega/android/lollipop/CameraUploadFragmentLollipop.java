package nz.mega.android.lollipop;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nz.mega.android.CameraSyncService;
import nz.mega.android.CreateThumbPreviewService;
import nz.mega.android.DatabaseHandler;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.MimeTypeList;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.LoopViewPager;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;


public class CameraUploadFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, MegaRequestListenerInterface{

	
	public static int GRID_WIDTH =300;
	
	Context context;
	ActionBar aB;
	RecyclerView listView;
	GestureDetectorCompat detector;
	private RecyclerView.LayoutManager mLayoutManager;
	ImageButton fabButton;
	SlidingUpPanelLayout slidingOptionsPanel;
	
	ImageView emptyImageView;
	TextView emptyTextView;
	LinearLayout buttonsLayout;
	TextView contentText;
//	Button turnOnOff;
	
	DatabaseHandler dbH;
	MegaPreferences prefs;
	
	MegaPhotoSyncListAdapterLollipop adapterList;
	MegaPhotoSyncGridAdapterLollipop adapterGrid;
	LinearLayout outSpaceLayout=null;
	LinearLayout getProLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;;
	MegaApiAndroid megaApi;
		
//	long parentHandle = -1;
	boolean isList = true;
	boolean firstTimeCam = false;
	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
	
	
	ArrayList<MegaNode> nodes;
	ArrayList<PhotoSyncHolder> nodesArray = new ArrayList<CameraUploadFragmentLollipop.PhotoSyncHolder>();
	ArrayList<PhotoSyncGridHolder> nodesArrayGrid = new ArrayList<CameraUploadFragmentLollipop.PhotoSyncGridHolder>();
	ArrayList<MegaMonthPicLollipop> monthPics = new ArrayList<MegaMonthPicLollipop>();
	
	private ActionMode actionMode;
	
	ProgressDialog statusDialog;
	long photosyncHandle = -1;
	
	public class PhotoSyncHolder{
		boolean isNode;
		long handle;
		String monthYear;
	}
	
	public class PhotoSyncGridHolder{
		boolean isNode;
		String monthYear;
		long handle1;
		long handle2;
		long handle3;
	}
	
//	private TourImageAdapter adapter;
//	private LoopViewPager viewPager;
	private ImageView initialImageView;
//	private ImageView bar;
	private TextView bOK;
	private TextView bSkip;	
	private RadioGroup camSyncRadioGroup;
	private RadioButton camSyncData;
	private RadioButton camSyncWifi;
	private RelativeLayout layoutRadioGroup;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{
		public void onLongPress(MotionEvent e) {
			log("onLongPress");
			if (isList){
		        View view = listView.findChildViewUnder(e.getX(), e.getY());
		        int position = listView.getChildPosition(view);
	
		        // handle long press
		        if (!adapterList.isMultipleSelect()){
					adapterList.setMultipleSelect(true);
				
					actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());			
	
			        itemClick(position);
			        super.onLongPress(e);
		        }
			}
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<PhotoSyncHolder> documents = adapterList.getSelectedDocuments();
			
			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).handle);
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).onFileClick(handleList);
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).handle);
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).showCopyLollipop(handleList);
					break;
				}	
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).handle);
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).showMoveLollipop(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					clearSelections();
					hideMultipleSelect();
					if (documents.size()==1){
						MegaNode n = megaApi.getNodeByHandle(documents.get(0).handle);
						((ManagerActivityLollipop) context).getPublicLinkAndShareIt(n);
					}
					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).handle);
					}
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop) context).moveToTrash(handleList);
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					break;
				}				
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_browser_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			adapterList.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<PhotoSyncHolder> selected = adapterList.getSelectedDocuments();
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			
			// Link
			if ((selected.size() == 1) && (megaApi.checkAccess(megaApi.getNodeByHandle(selected.get(0).handle), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
				showLink = true;
			}
			
			if (selected.size() > 0) {
				showDownload = true;
				showTrash = true;
				showMove = true;
				showCopy = true;
				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(megaApi.getNodeByHandle(selected.get(i).handle), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showTrash = false;
						showMove = false;
						break;
					}
				}
				
				if(selected.size() == nodes.size()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
				}
			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}
			
			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			
			return false;
		}
		
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
		super.onCreate(savedInstanceState);
		log("onCreate");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		log("onCreateView");		
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		
		if (megaApi.getRootNode() == null){
			return null;
		}
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
	    float scaleText;
	    if (scaleH < scaleW){
	    	scaleText = scaleH;
	    }
	    else{
	    	scaleText = scaleW;
	    }
		
		aB.setTitle(getString(R.string.section_photo_sync));	
		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
		((ManagerActivityLollipop)context).setFirstNavigationLevel(true);
		((ManagerActivityLollipop)context).supportInvalidateOptionsMenu();

		if (firstTimeCam){
			setInitialPreferences();
			View v = inflater.inflate(R.layout.activity_cam_sync_initial, container, false);
			
			initialImageView = (ImageView) v.findViewById(R.id.cam_sync_image_view);
			initialImageView.getLayoutParams().height = outMetrics.widthPixels;
			bOK = (TextView) v.findViewById(R.id.cam_sync_button_ok);
			bSkip = (TextView) v.findViewById(R.id.cam_sync_button_skip);
			camSyncRadioGroup = (RadioGroup) v.findViewById(R.id.cam_sync_radio_group);
			camSyncData = (RadioButton) v.findViewById(R.id.cam_sync_data);
			camSyncWifi = (RadioButton) v.findViewById(R.id.cam_sync_wifi);
			layoutRadioGroup = (RelativeLayout) v.findViewById(R.id.cam_sync_relative_radio);
			buttonsLayout = (LinearLayout) v.findViewById(R.id.cam_buttons_layout);
			
			bSkip.setText(getString(R.string.cam_sync_skip).toUpperCase(Locale.getDefault()));
			android.view.ViewGroup.LayoutParams paramsb2 = bSkip.getLayoutParams();		
			paramsb2.height = Util.scaleHeightPx(48, outMetrics);
			paramsb2.width = Util.scaleWidthPx(63, outMetrics);
			bSkip.setLayoutParams(paramsb2);
			//Left and Right margin
			LinearLayout.LayoutParams textParamsLogin = (LinearLayout.LayoutParams)bSkip.getLayoutParams();
			textParamsLogin.setMargins(Util.scaleWidthPx(6, outMetrics), 0, Util.scaleWidthPx(8, outMetrics), 0); 
			bSkip.setLayoutParams(textParamsLogin);
			
			bOK.setText(getString(R.string.cam_sync_ok).toUpperCase(Locale.getDefault()));
			android.view.ViewGroup.LayoutParams paramsb1 = bOK.getLayoutParams();		
			paramsb1.height = Util.scaleHeightPx(48, outMetrics);
			paramsb1.width = Util.scaleWidthPx(144, outMetrics);
			bOK.setLayoutParams(paramsb1);
			
			bSkip.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));
			bOK.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));
			
			bOK.setOnClickListener(this);
			bSkip.setOnClickListener(this);
			
			return v;
		}
				
		if (isList){
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button);
			fabButton.setVisibility(View.GONE);
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			slidingOptionsPanel.setVisibility(View.GONE);
			
			final Button turnOnOff = (Button) v.findViewById(R.id.file_list_browser_camera_upload_on_off);
			turnOnOff.setVisibility(View.VISIBLE);
			turnOnOff.setText(context.getResources().getString(R.string.settings_camera_upload_on));
			boolean camEnabled = false;
			if (prefs != null){
				if (prefs.getCamSyncEnabled() != null){
					if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
						turnOnOff.setVisibility(View.GONE);
						camEnabled = true;
					}
					else{
						camEnabled = false;
					}
				}
			}
			turnOnOff.setOnClickListener(this);
	
			contentText = (TextView) v.findViewById(R.id.content_text);
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_layout);
			
			contentText.setVisibility(View.GONE);
			buttonsLayout.setVisibility(View.GONE);
			getProLayout=(LinearLayout) v.findViewById(R.id.get_pro_account);
			getProLayout.setVisibility(View.GONE);
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn);
			outSpaceButton.setVisibility(View.VISIBLE);
			outSpaceButton.setOnClickListener(this);
			
			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
			
			if(usedSpacePerc>95){
				//Change below of ListView
				log("usedSpacePerc>95");
				buttonsLayout.setVisibility(View.GONE);				
//				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//				p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//				listView.setLayoutParams(p);
				outSpaceLayout.setVisibility(View.VISIBLE);
				outSpaceLayout.bringToFront();
				
				Handler handler2 = new Handler();
				handler2.postDelayed(new Runnable() {					
					
					@Override
					public void run() {
						log("BUTTON DISAPPEAR");
						log("altura: "+outSpaceLayout.getHeight());
						
						TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, outSpaceLayout.getHeight());
						animTop.setDuration(2000);
						animTop.setFillAfter(true);
						outSpaceLayout.setAnimation(animTop);
					
						outSpaceLayout.setVisibility(View.GONE);
						outSpaceLayout.invalidate();
//						RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//						p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
//						listView.setLayoutParams(p);
					}
				}, 15 * 1000);
				
			}	
			else{
				outSpaceLayout.setVisibility(View.GONE);
				
				if(!camEnabled){
					turnOnOff.setOnClickListener(this);
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						
											@Override
											public void run() {
												log("BUTTON DISAPPEAR");
												TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
												animTop.setDuration(1000);
												animTop.setFillAfter( true );
												turnOnOff.setAnimation(animTop);
												turnOnOff.setVisibility(View.GONE);
											}
										}, 30 * 1000);
				}
				
			}
			
			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) listView.getLayoutParams();
			p.addRule(RelativeLayout.ABOVE, R.id.file_list_browser_camera_upload_on_off);
			listView.setLayoutParams(p);
			
			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_list_empty_text);
			
			emptyImageView.setImageResource(R.drawable.media_empty_image);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
			
			if (megaApi.getRootNode() == null){
				return v;
			}
			
			if (prefs == null){
				photosyncHandle = -1;
			}
			else{
				//The "PhotoSync" folder exists?
				if (prefs.getCamSyncHandle() == null){
					photosyncHandle = -1;
				}
				else{
					photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
					if (megaApi.getNodeByHandle(photosyncHandle) == null){
						photosyncHandle = -1;
					}
				}
			}
			
			if (photosyncHandle == -1){
				ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
				for (int i=0;i<nl.size();i++){
					if ((CameraSyncService.CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						photosyncHandle = nl.get(i).getHandle();
						dbH.setCamSyncHandle(photosyncHandle);
						listView.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
						break;
					}
				}
			}
			
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			
			if (nodesArray != null){
				nodesArray.clear();
			}
			
			if (megaApi.getNodeByHandle(photosyncHandle) != null){
				
				if (Util.CREATE_THUMB_PREVIEW_SERVICE){
					if (context != null){
						Intent intent = new Intent(context, CreateThumbPreviewService.class);
						intent.putExtra(CreateThumbPreviewService.EXTRA_PARENT_HASH, photosyncHandle);
						context.startService(intent);
					}
				}
				
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
				int month = 0;
				int year = 0;
				for (int i=0;i<nodes.size();i++){
					if (nodes.get(i).isFolder()){
						continue;
					}
					
					if (!MimeTypeList.typeForName(nodes.get(i).getName()).isImage() && (!MimeTypeList.typeForName(nodes.get(i).getName()).isVideo())){
						continue;
					}
					
					PhotoSyncHolder psh = new PhotoSyncHolder();
					Date d = new Date(nodes.get(i).getModificationTime()*1000);
					if ((month == d.getMonth()) && (year == d.getYear())){
						psh.isNode = true;
						psh.handle = nodes.get(i).getHandle();
						nodesArray.add(psh);
					}
					else{
						month = d.getMonth();
						year = d.getYear();
						psh.isNode = false;
						psh.monthYear = getImageDateString(month, year);
						nodesArray.add(psh);
						psh = new PhotoSyncHolder();
						psh.isNode = true;
						psh.handle = nodes.get(i).getHandle();
						nodesArray.add(psh);
						log("MONTH: " + d.getMonth() + "YEAR: " + d.getYear());
					}
				}
				
				if (nodesArray.size() == 0){
					emptyImageView.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);	
				}
				else{
					emptyImageView.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
			}
			else{
				emptyImageView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}
			
			if (adapterList == null){
				adapterList = new MegaPhotoSyncListAdapterLollipop(context, nodesArray, photosyncHandle, listView, emptyImageView, emptyTextView, aB, nodes, this, ManagerActivityLollipop.CAMERA_UPLOAD_ADAPTER);
			}
			else{
				adapterList.setNodes(nodesArray, nodes);
			}
			
			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.file_grid_view_browser);
//			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 
			
			fabButton = (ImageButton) v.findViewById(R.id.file_upload_button_grid);
			fabButton.setVisibility(View.GONE);
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_grid);
			slidingOptionsPanel.setVisibility(View.GONE);
			
			final Button turnOnOff = (Button) v.findViewById(R.id.file_grid_browser_camera_upload_on_off);
			turnOnOff.setVisibility(View.VISIBLE);
			turnOnOff.setText(context.getResources().getString(R.string.settings_camera_upload_on));
			boolean camEnabled = false;
			if (prefs != null){
				if (prefs.getCamSyncEnabled() != null){
					if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
						turnOnOff.setVisibility(View.GONE);
						camEnabled = true;
					}
					else{
						camEnabled = false;
					}
				}
			}
			turnOnOff.setOnClickListener(this);
	
			contentText = (TextView) v.findViewById(R.id.content_grid_text);
			buttonsLayout = (LinearLayout) v.findViewById(R.id.buttons_grid_layout);
			
			contentText.setVisibility(View.GONE);
			buttonsLayout.setVisibility(View.GONE);
			getProLayout=(LinearLayout) v.findViewById(R.id.get_pro_account_grid);
			getProLayout.setVisibility(View.GONE);
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_grid);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text_grid);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn_grid);
			outSpaceButton.setVisibility(View.VISIBLE);
			outSpaceButton.setOnClickListener(this);
			
			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
			
			if(usedSpacePerc>95){
				//Change below of ListView
				log("usedSpacePerc>95");
				buttonsLayout.setVisibility(View.GONE);				
//				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//				p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//				listView.setLayoutParams(p);
				outSpaceLayout.setVisibility(View.VISIBLE);
				outSpaceLayout.bringToFront();
				
				Handler handler2 = new Handler();
				handler2.postDelayed(new Runnable() {					
					
					@Override
					public void run() {
						log("BUTTON DISAPPEAR");
						log("altura: "+outSpaceLayout.getHeight());
						
						TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, outSpaceLayout.getHeight());
						animTop.setDuration(2000);
						animTop.setFillAfter(true);
						outSpaceLayout.setAnimation(animTop);
					
						outSpaceLayout.setVisibility(View.GONE);
						outSpaceLayout.invalidate();
//						RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//						p.addRule(RelativeLayout.ABOVE, R.id.buttons_layout);
//						listView.setLayoutParams(p);
					}
				}, 15 * 1000);
				
			}	
			else{
				outSpaceLayout.setVisibility(View.GONE);
				
				if(!camEnabled){
					turnOnOff.setOnClickListener(this);
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						
											@Override
											public void run() {
												log("BUTTON DISAPPEAR");
												TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
												animTop.setDuration(1000);
												animTop.setFillAfter( true );
												turnOnOff.setAnimation(animTop);
												turnOnOff.setVisibility(View.GONE);
											}
										}, 30 * 1000);
				}
				
			}
			
			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) listView.getLayoutParams();
			p.addRule(RelativeLayout.ABOVE, R.id.file_grid_browser_camera_upload_on_off);
			listView.setLayoutParams(p);
			
			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.file_grid_empty_text);
			
			emptyImageView.setImageResource(R.drawable.media_empty_image);
			emptyTextView.setText(R.string.file_browser_empty_folder);
			
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
			
			if (megaApi.getRootNode() == null){
				return v;
			}
			
			if (prefs == null){
				photosyncHandle = -1;
			}
			else{
				//The "PhotoSync" folder exists?
				if (prefs.getCamSyncHandle() == null){
					photosyncHandle = -1;
				}
				else{
					photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
					if (megaApi.getNodeByHandle(photosyncHandle) == null){
						photosyncHandle = -1;
					}
				}
			}
			
			if (photosyncHandle == -1){
				ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
				for (int i=0;i<nl.size();i++){
					if ((CameraSyncService.CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
						photosyncHandle = nl.get(i).getHandle();
						dbH.setCamSyncHandle(photosyncHandle);
						listView.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
						break;
					}
				}
			}
			
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			
			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
			DisplayMetrics outMetrics = new DisplayMetrics ();
		    display.getMetrics(outMetrics);
		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
			
		    float scaleW = Util.getScaleW(outMetrics, density);
		    float scaleH = Util.getScaleH(outMetrics, density);
		    
		    int totalWidth = outMetrics.widthPixels;
		    int totalHeight = outMetrics.heightPixels;
		    float dpWidth  = outMetrics.widthPixels / density;
		    		    
		    int numberOfCells = totalWidth / GRID_WIDTH;
		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
		    	if (numberOfCells < 4){
					numberOfCells = 4;
				}	
		    }
		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
		    	if (numberOfCells < 3){
					numberOfCells = 3;
				}	
		    }
		    
			
			if (monthPics != null){
				monthPics.clear();
			}
			
			if (megaApi.getNodeByHandle(photosyncHandle) != null){
				
				if (Util.CREATE_THUMB_PREVIEW_SERVICE){
					if (context != null){
						Intent intent = new Intent(context, CreateThumbPreviewService.class);
						intent.putExtra(CreateThumbPreviewService.EXTRA_PARENT_HASH, photosyncHandle);
						context.startService(intent);
					}
				}
				
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
				int month = 0;
				int year = 0;
				MegaMonthPicLollipop monthPic = new MegaMonthPicLollipop();
				boolean thereAreImages = false;
				for (int i=0;i<nodes.size();i++){
					if (nodes.get(i).isFolder()){
						continue;
					}
					
					if (!MimeTypeList.typeForName(nodes.get(i).getName()).isImage() && (!MimeTypeList.typeForName(nodes.get(i).getName()).isVideo())){
						continue;
					}
					
					Date d = new Date(nodes.get(i).getModificationTime()*1000);
					if ((month == 0) && (year == 0)){
						month = d.getMonth();
						year = d.getYear();
						monthPic.monthYearString = getImageDateString(month, year);
						monthPics.add(monthPic);
						monthPic = new MegaMonthPicLollipop();
						i--;
					}
					else if ((month == d.getMonth()) && (year == d.getYear())){
						thereAreImages = true;
						if (monthPic.nodeHandles.size() == numberOfCells){
							monthPics.add(monthPic);
							monthPic = new MegaMonthPicLollipop();
							monthPic.nodeHandles.add(nodes.get(i).getHandle());
						}
						else{
							monthPic.nodeHandles.add(nodes.get(i).getHandle());
						}
					}
					else{
						month = d.getMonth();
						year = d.getYear();
						monthPics.add(monthPic);
						monthPic = new MegaMonthPicLollipop();
						monthPic.monthYearString = getImageDateString(month, year);
						monthPics.add(monthPic);
						monthPic = new MegaMonthPicLollipop();
						i--;						
					}
				}
				if (nodes.size() > 0){
					monthPics.add(monthPic);
				}
				
				if (!thereAreImages){
					monthPics.clear();
					emptyImageView.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				}
				else{
					emptyImageView.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
			}
			else{
				emptyImageView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}
			
			if (adapterGrid == null){
				log("ADAPTERGRID.MONTHPICS(NEW) = " + monthPics.size());
				adapterGrid = new MegaPhotoSyncGridAdapterLollipop(context, monthPics, photosyncHandle, listView, emptyImageView, emptyTextView, aB, nodes, numberOfCells, this, ManagerActivityLollipop.CAMERA_UPLOAD_ADAPTER);
			}
			else{
				log("ADAPTERGRID.MONTHPICS = " + monthPics.size());
				adapterGrid.setNodes(monthPics, nodes);
			}
			
			listView.setAdapter(adapterGrid);
			
			return v;
		}
	}
	
	public void selectAll(){
		if (isList){
			if(adapterList.isMultipleSelect()){
				adapterList.selectAll();
			}
			else{
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
				
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();
			}
			
			updateActionModeTitle();
		}
		else{
			/*if (adapterGrid != null){
				adapterGrid.selectAll();
			}*/
		}
	}
	
	public void setInitialPreferences(){
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		dbH.setFirstTime(false);
		dbH.setCamSyncEnabled(false);
		dbH.setStorageAskAlways(false);
		File defaultDownloadLocation = null;
		if (Environment.getExternalStorageDirectory() != null){
			defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR + "/");
		}
		else{
			defaultDownloadLocation = context.getFilesDir();
		}
		
		defaultDownloadLocation.mkdirs();
		
		dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
		dbH.setPinLockEnabled(false);
		dbH.setPinLockCode("");
	}
	
	public String getImageDateString(int month, int year){
		String ret = "";
		year = year + 1900;
	
		switch(month){
			case 0:{
				ret = context.getString(R.string.january) + " " + year;
				break;
			}
			case 1:{
				ret = context.getString(R.string.february) + " " + year;
				break;
			}
			case 2:{
				ret = context.getString(R.string.march) + " " + year;
				break;
			}
			case 3:{
				ret = context.getString(R.string.april) + " " + year;
				break;
			}
			case 4:{
				ret = context.getString(R.string.may) + " " + year;
				break;
			}
			case 5:{
				ret = context.getString(R.string.june) + " " + year;
				break;
			}
			case 6:{
				ret = context.getString(R.string.july) + " " + year;
				break;
			}
			case 7:{
				ret = context.getString(R.string.august) + " " + year;
				break;
			}
			case 8:{
				ret = context.getString(R.string.september) + " " + year;
				break;
			}
			case 9:{
				ret = context.getString(R.string.october) + " " + year;
				break;
			}
			case 10:{
				ret = context.getString(R.string.november) + " " + year;
				break;
			}
			case 11:{
				ret = context.getString(R.string.december) + " " + year;
				break;
			}
		}
		return ret;
	}
		
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.file_grid_browser_camera_upload_on_off:
			case R.id.file_list_browser_camera_upload_on_off:{
				final DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
				MegaPreferences prefs = dbH.getPreferences();
				boolean isEnabled = false;
				if (prefs != null){
					if (prefs.getCamSyncEnabled() != null){
						if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
							isEnabled = true;
						}
					}
				}

				if (isEnabled){
					dbH.setCamSyncTimeStamp(0);
					dbH.setCamSyncEnabled(false);
					
					Intent stopIntent = null;
					stopIntent = new Intent(context, CameraSyncService.class);
					stopIntent.setAction(CameraSyncService.ACTION_STOP);
					context.startService(stopIntent);
					
					((ManagerActivityLollipop)context).refreshCameraUpload();
				}
				else{					
					AlertDialog wifiDialog;
					
					final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice, android.R.id.text1, new String[] {getResources().getString(R.string.cam_sync_wifi), getResources().getString(R.string.cam_sync_data)});
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle(getString(R.string.section_photo_sync));
					builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dbH.setCamSyncTimeStamp(0);
							dbH.setCamSyncEnabled(true);
							dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
							dbH.setCamSyncEnabled(true);
							File localFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
							String localPath = localFile.getAbsolutePath();
							dbH.setCamSyncLocalPath(localPath);
							
							Handler handler = new Handler();
							handler.postDelayed(new Runnable() {
								
													@Override
													public void run() {
														log("Now I start the service");
														context.startService(new Intent(context, CameraSyncService.class));		
													}
												}, 5 * 1000);
						
							((ManagerActivityLollipop)context).refreshCameraUpload();
							switch (which){
							case 0:{
								dbH.setCamSyncWifi(true);
								break;
							}
							case 1:{
								dbH.setCamSyncWifi(false);
								break;
							}
						}
							dialog.dismiss();
						}
					});
					
					builder.setPositiveButton(context.getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

					wifiDialog = builder.create();
					wifiDialog.show();
					Util.brandAlertDialog(wifiDialog);
				}
				break;
			}
			case R.id.out_space_btn_grid:
			case R.id.out_space_btn:{
				((ManagerActivityLollipop)getActivity()).upgradeAccountButton();
				break;
			}
			case R.id.cam_sync_button_ok:{
				firstTimeCam = false;
				DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
				dbH.setCamSyncEnabled(true);
				File localFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
				String localPath = localFile.getAbsolutePath();
				dbH.setCamSyncLocalPath(localPath);
				if (camSyncData.isChecked()){
					dbH.setCamSyncWifi(false);
				}
				else{
					dbH.setCamSyncWifi(true);
				}
				dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
				
				context.startService(new Intent(context, CameraSyncService.class));
				
				((ManagerActivityLollipop)context).refreshCameraUpload();
				break;
			}
			case R.id.cam_sync_button_skip:{
				firstTimeCam = false;
				((ManagerActivityLollipop)context).setInitialCloudDrive();
				break;
			}
		}
	}
	
	public void itemClick(int position) {
		log("itemClick");
		
		if (isList){
			if (adapterList.isMultipleSelect()){
				adapterList.toggleSelection(position);
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
		}
	}
	
	/*@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		if (isList){
			if (adapterList.isMultipleSelect()){
				SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
				if (checkedItems.get(position, false) == true){
					listView.setItemChecked(position, true);
				}
				else{
					listView.setItemChecked(position, false);
				}				
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{
				PhotoSyncHolder psH = nodesArray.get(position);
				if (psH.isNode){
					MegaNode n = megaApi.getNodeByHandle(psH.handle);
					if (n.isFolder()){
						
					}
					else{
						if (MimeTypeList.typeForName(n.getName()).isImage()){
							int positionInNodes = 0;
							for (int i=0;i<nodes.size();i++){
								if(nodes.get(i).getHandle() == n.getHandle()){
									positionInNodes = i;
								}
							}
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", positionInNodes);
							intent.putExtra("adapterType", ManagerActivity.PHOTO_SYNC_ADAPTER);
							intent.putExtra("parentNodeHandle", photosyncHandle);
							intent.putExtra("orderGetChildren", orderGetChildren);
							startActivity(intent);
						}
						else if (MimeTypeList.typeForName(n.getName()).isVideo() || MimeTypeList.typeForName(n.getName()).isAudio() ){
							Intent service = new Intent(context, MegaStreamingService.class);
					  		context.startService(service);
					  		String fileName = n.getName();
							try {
								fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
							} 
							catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							
					  		String url = "http://127.0.0.1:4443/" + n.getBase64Handle() + "/" + fileName;
					  		String mimeType = MimeTypeList.typeForName(n.getName()).getType();
					  		System.out.println("FILENAME: " + fileName);
					  		
					  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					  		if (ManagerActivity.isIntentAvailable(context, mediaIntent)){
					  			startActivity(mediaIntent);
					  		}
					  		else{
					  			Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
					  			adapterList.setPositionClicked(-1);
								adapterList.notifyDataSetChanged();
								ArrayList<Long> handleList = new ArrayList<Long>();
								handleList.add(n.getHandle());
								((ManagerActivity) context).onFileClick(handleList);
					  		}
						}
						else{
							adapterList.setPositionClicked(-1);
							adapterList.notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							((ManagerActivity) context).onFileClick(handleList);
						}
					}
				}
			}
		}
    }*/
	
	/*@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		return false;
		log("onItemLongClick");
		PhotoSyncHolder psH = nodesArray.get(position);
		if (psH.isNode){
			if (adapterList.getPositionClicked() == -1){
				clearSelections();
				actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
				listView.setItemChecked(position, true);
				adapterList.setMultipleSelect(true);
				updateActionModeTitle();
				listView.setOnItemLongClickListener(null);
			}
			return true;
		}
		return false;
	}*/
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapterList.isMultipleSelect()){
			adapterList.clearSelections();
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<PhotoSyncHolder> documents = adapterList.getSelectedDocuments();
		int files = 0;
		int folders = 0;
		for (PhotoSyncHolder document : documents) {
			MegaNode n = megaApi.getNodeByHandle(document.handle);
			if (n != null){
				if (n.isFile()) {
					files++;
				} else if (n.isFolder()) {
					folders++;
				}
			}
		}
		Resources res = getActivity().getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = "";
		} else if (files == 0) {
			title = foldersStr;
		} else if (folders == 0) {
			title = filesStr;
		} else {
			title = foldersStr + ", " + filesStr;
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
		// actionMode.
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public int onBackPressed(){
		if (isList){
			if (adapterList != null){
				if (adapterList.isMultipleSelect()){
					hideMultipleSelect();
					return 2;
				}
			}
		}
		else{
			/*if (adapterGrid != null){
				if (adapterGrid.getPositionClicked() != -1){
					adapterGrid.setPositionClicked(-1);
					adapterGrid.notifyDataSetChanged();
					return 1;
				}
				else if (adapterGrid.isMultipleSelect()){
					adapterGrid.hideMultipleSelect();
					return 2;
				}
			}*/
		}
		
		return 0;

//		if (isList){
//			parentHandle = adapterList.getParentHandle();
//			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//			
//			if (adapterList.getPositionClicked() != -1){
//				adapterList.setPositionClicked(-1);
//				adapterList.notifyDataSetChanged();
//				return 1;
//			}
//			else{
//				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
//				if (parentNode != null){
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//						aB.setTitle(getString(R.string.section_cloud_drive));	
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//					}
//					else{
//						aB.setTitle(parentNode.getName());					
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					}
//					
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = parentNode.getHandle();
//					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//					nodes = megaApi.getChildren(parentNode, orderGetChildren);
//					adapterList.setNodes(nodes);
//					listView.setSelection(0);
//					adapterList.setParentHandle(parentHandle);
//					return 2;
//				}
//				else{
//					return 0;
//				}
//			}
//		}
//		else{
//			parentHandle = adapterGrid.getParentHandle();
//			((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//			
//			if (adapterGrid.getPositionClicked() != -1){
//				adapterGrid.setPositionClicked(-1);
//				adapterGrid.notifyDataSetChanged();
//				return 1;
//			}
//			else{
//				MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(parentHandle));
//				if (parentNode != null){
//					listView.setVisibility(View.VISIBLE);
//					emptyImageView.setVisibility(View.GONE);
//					emptyTextView.setVisibility(View.GONE);
//					if (parentNode.getHandle() == megaApi.getRootNode().getHandle()){
//						aB.setTitle(getString(R.string.section_cloud_drive));	
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
//					}
//					else{
//						aB.setTitle(parentNode.getName());					
//						((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					}
//					
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = parentNode.getHandle();
//					((ManagerActivity)context).setParentHandleBrowser(parentHandle);
//					nodes = megaApi.getChildren(parentNode, orderGetChildren);
//					adapterGrid.setNodes(nodes);
//					listView.setSelection(0);
//					adapterGrid.setParentHandle(parentHandle);
//					return 2;
//				}
//				else{
//					return 0;
//				}
//			}
//		}
	}
	
	public long getPhotoSyncHandle(){

		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		MegaPreferences prefs = dbH.getPreferences();
		if (prefs == null){
			photosyncHandle = -1;
		}
		else{
			//The "PhotoSync" folder exists?
			if (prefs.getCamSyncHandle() == null){
				photosyncHandle = -1;
			}
			else{
				photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
				if (megaApi.getNodeByHandle(photosyncHandle) == null){
					photosyncHandle = -1;
				}
			}
		}
		
		if (photosyncHandle == -1){
			ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
			for (int i=0;i<nl.size();i++){
				if ((CameraSyncService.CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
					photosyncHandle = nl.get(i).getHandle();
					dbH.setCamSyncHandle(photosyncHandle);
					if (listView != null){
						listView.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
					}
					break;
				}
			}
		}
		
		if (isList){
			if (adapterList != null){
				adapterList.setPhotoSyncHandle(photosyncHandle);
			}
		}
		else{
			/*if (adapterGrid != null){
				adapterGrid.setPhotoSyncHandle(photosyncHandle);
			}*/
		}
		
		return photosyncHandle;
	}
	
	
	public RecyclerView getListView(){
		return listView;
	}
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		
		if (isList){
			this.nodesArray.clear();
			
			int month = 0;
			int year = 0;
			for (int i=0;i<nodes.size();i++){
				if (nodes.get(i).isFolder()){
					continue;
				}
				PhotoSyncHolder psh = new PhotoSyncHolder();
				Date d = new Date(nodes.get(i).getModificationTime()*1000);
				if ((month == d.getMonth()) && (year == d.getYear())){
					psh.isNode = true;
					psh.handle = nodes.get(i).getHandle();
					nodesArray.add(psh);
				}
				else{
					month = d.getMonth();
					year = d.getYear();
					psh.isNode = false;
					psh.monthYear = getImageDateString(month, year);
					nodesArray.add(psh);
					psh = new PhotoSyncHolder();
					psh.isNode = true;
					psh.handle = nodes.get(i).getHandle();
					nodesArray.add(psh);
					log("MONTH: " + d.getMonth() + "YEAR: " + d.getYear());
				}
			}
			if (adapterList != null){
				adapterList.setNodes(nodesArray, nodes);
				if (adapterList.getItemCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.GONE);
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}	
		}
		else{
			/*this.monthPics.clear();
			
			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
			DisplayMetrics outMetrics = new DisplayMetrics ();
		    display.getMetrics(outMetrics);
		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
			
		    float scaleW = Util.getScaleW(outMetrics, density);
		    float scaleH = Util.getScaleH(outMetrics, density);
		    
		    int totalWidth = outMetrics.widthPixels;
		    int totalHeight = outMetrics.heightPixels;
		    float dpWidth  = outMetrics.widthPixels / density;
		    		    
		    int numberOfCells = totalWidth / GRID_WIDTH;
		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
		    	if (numberOfCells < 4){
					numberOfCells = 4;
				}	
		    }
		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
		    	if (numberOfCells < 3){
					numberOfCells = 3;
				}	
		    }
		    
			
			if (monthPics != null){
				monthPics.clear();
			}
			
			if (megaApi.getNodeByHandle(photosyncHandle) != null){
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
				int month = 0;
				int year = 0;
				MegaMonthPic monthPic = new MegaMonthPic();
				boolean thereAreImages = false;
				for (int i=0;i<nodes.size();i++){
					if (nodes.get(i).isFolder()){
						continue;
					}
					
					if (!MimeTypeList.typeForName(nodes.get(i).getName()).isImage()){
						continue;
					}
					
					Date d = new Date(nodes.get(i).getModificationTime()*1000);
					if ((month == 0) && (year == 0)){
						month = d.getMonth();
						year = d.getYear();
						monthPic.monthYearString = getImageDateString(month, year);
						monthPics.add(monthPic);
						monthPic = new MegaMonthPic();
						i--;
					}
					else if ((month == d.getMonth()) && (year == d.getYear())){
						thereAreImages = true;
						if (monthPic.nodeHandles.size() == numberOfCells){
							monthPics.add(monthPic);
							monthPic = new MegaMonthPic();
							monthPic.nodeHandles.add(nodes.get(i).getHandle());
						}
						else{
							monthPic.nodeHandles.add(nodes.get(i).getHandle());
						}
					}
					else{
						month = d.getMonth();
						year = d.getYear();
						monthPics.add(monthPic);
						monthPic = new MegaMonthPic();
						monthPic.monthYearString = getImageDateString(month, year);
						monthPics.add(monthPic);
						monthPic = new MegaMonthPic();
						i--;						
					}
				}
				if (nodes.size() > 0){
					monthPics.add(monthPic);
				}
				
				if (!thereAreImages){
					monthPics.clear();
					if (emptyImageView != null){
						emptyImageView.setVisibility(View.VISIBLE);
						listView.setVisibility(View.GONE);
					}
				}
				else{
					if (emptyImageView != null){
						emptyImageView.setVisibility(View.GONE);
						listView.setVisibility(View.VISIBLE);
					}
				}
			}
			else{
				if (emptyImageView != null){
					emptyImageView.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				}
			}
			
			if (adapterGrid != null){
				adapterGrid.setNodes(monthPics, nodes);
				if (adapterGrid.getCount() == 0){
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.GONE);
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
				else{
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}			
			}*/
		}
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			/*if (adapterGrid != null){
				adapterGrid.notifyDataSetChanged();
			}*/
		}
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public void setFirstTimeCam(boolean firstTimeCam){
		this.firstTimeCam = firstTimeCam;
	}
	
	public boolean getFirstTimeCam(){
		return firstTimeCam;
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
		if (isList){
			if (adapterList != null){
				adapterList.setOrder(orderGetChildren);
			}
		}
		else{
			/*if (adapterGrid != null){
				adapterGrid.setOrder(orderGetChildren);
			}*/
		}
	}
	
	public boolean showSelectMenuItem(){
		if (isList){
			if (adapterList != null){
				return adapterList.isMultipleSelect();
			}
		}
		else{
			/*if (adapterGrid != null){
				return adapterGrid.isMultipleSelect();
			}*/
		}
		
		return false;
	}
	
	private static void log(String log) {
		Util.log("CameraUploadFragment", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			log("create folder start");
		}		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			log("create folder finished");
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, context.getString(R.string.camera_uploads_created), Toast.LENGTH_LONG).show();
				emptyImageView.setVisibility(View.VISIBLE);
				emptyImageView.setOnClickListener(this);
				emptyTextView.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		log("onInterceptTouchEvent");
		detector.onTouchEvent(e);
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		
	}
}
