package mega.privacy.android.app.lollipop.managerSections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.DividerItemDecorationV2;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MegaMonthPicLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaPhotoSyncGridTitleAdapterLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaPhotoSyncListAdapterLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.MegaPreferences.*;
import static mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop.DEFAULT_CONVENTION_QUEUE_SIZE;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;


public class CameraUploadFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, MegaRequestListenerInterface{

	public static ImageView imageDrag;

	public static int GRID_WIDTH = 154;

	public static int GRID_LARGE = 3;
	public static int GRID_SMALL = 7;
	
	public static int TYPE_CAMERA= 0;
	public static int TYPE_MEDIA = 1;

	private Context context;
	private ActionBar aB;
	private RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	FastScroller fastScroller;

	long[] arrayHandles = null;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;

//	private RelativeLayout contentTextLayout;
//	Button turnOnOff;
	private RelativeLayout transfersOverViewLayout;

	private SwitchCompat switchCellularConnection;
	private SwitchCompat switchUploadVideos;

	private DatabaseHandler dbH;
	private MegaPreferences prefs;

	MegaPhotoSyncListAdapterLollipop adapterList;
	MegaPhotoSyncGridTitleAdapterLollipop adapterGrid;
	private MegaApiAndroid megaApi;

//	long parentHandle = -1;
//	private boolean firstTimeCam = false;

	private int type = 0;

	private ArrayList<MegaNode> nodes;
	private ArrayList<MegaNode> searchNodes;

	private ArrayList<PhotoSyncHolder> nodesArray = new ArrayList<CameraUploadFragmentLollipop.PhotoSyncHolder>();
	private ArrayList<PhotoSyncGridHolder> nodesArrayGrid = new ArrayList<CameraUploadFragmentLollipop.PhotoSyncGridHolder>();
	private ArrayList<MegaMonthPicLollipop> monthPics = new ArrayList<MegaMonthPicLollipop>();
	private long[] searchByDate;

	private ActionMode actionMode;

	String defaultPath;
	String downloadLocationDefaultPath;

	private ProgressDialog statusDialog;
	private long photosyncHandle = -1;

	ScrollView scrollView;

	Handler handler;

	public class PhotoSyncHolder{
		public boolean isNode;
		public long handle;
		public String monthYear;
		public String nodeDate;

		public long getHandle(){
			return handle;
		}
	}
	
	public class PhotoSyncGridHolder{
		public boolean isNode;
		public String monthYear;
		public long handle1;
		public long handle2;
		public long handle3;
	}

	private ImageView initialImageView;
	private TextView bOK;
	private TextView bSkip;	
	private RelativeLayout fragmentContainer;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	private TextView turnOnOff;
	private RelativeLayout relativeLayoutTurnOnOff;

	public void updateScrollPosition(int position) {
		logDebug("Position: " + position);
		if (mLayoutManager != null) {
                mLayoutManager.scrollToPosition(position);
            }
	}

	public ImageView getImageDrag(int position) {
		logDebug("Position: " + position);
		if (mLayoutManager != null) {
			if (((ManagerActivityLollipop) context).isListCameraUploads) {
				View v = mLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.photo_sync_list_thumbnail);
				}
			}
			else {
				View v = mLayoutManager.findViewByPosition(position);
				if (v != null) {
					return (ImageView) v.findViewById(R.id.cell_photosync_grid_title_thumbnail);
				}
			}
		}

		return null;
	}

	public void activateActionMode(){
		logDebug("activateActionMode");
		if (!adapterList.isMultipleSelect()){
			adapterList.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	public void onStoragePermissionRefused() {
        showSnackBar(context, SNACKBAR_TYPE, getString(R.string.on_refuse_storage_permission), -1);
        toCloudDrive();
    }

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			List<PhotoSyncHolder> documentsList = null;
			List<MegaNode> documentsGrid = null;

			if(adapterList!=null){
				documentsList = adapterList.getSelectedDocuments();
			}
			else if(adapterGrid != null){
				documentsGrid = adapterGrid.getSelectedDocuments();
			}

			switch(item.getItemId()){
				case R.id.cab_menu_download:{
					ArrayList<Long> handleList = new ArrayList<Long>();

					if(adapterList!=null){
						for (int i=0;i<documentsList.size();i++){
							handleList.add(documentsList.get(i).handle);
						}
					}
					else if(adapterGrid != null){
						for (int i=0;i<documentsGrid.size();i++){
							handleList.add(documentsGrid.get(i).getHandle());
						}
					}

					clearSelections();
					NodeController nC = new NodeController(context);
					nC.prepareForDownload(handleList, false);
					break;
				}
				case R.id.cab_menu_copy:{
					ArrayList<Long> handleList = new ArrayList<Long>();

					if(adapterList!=null){
						for (int i=0;i<documentsList.size();i++){
							handleList.add(documentsList.get(i).handle);
						}
					}
					else if(adapterGrid != null){
						for (int i=0;i<documentsGrid.size();i++){
							handleList.add(documentsGrid.get(i).getHandle());
						}
					}
					clearSelections();
					NodeController nC = new NodeController(context);
					nC.chooseLocationToCopyNodes(handleList);
					break;
				}
				case R.id.cab_menu_move:{
					ArrayList<Long> handleList = new ArrayList<Long>();

					if(adapterList!=null){
						for (int i=0;i<documentsList.size();i++){
							handleList.add(documentsList.get(i).handle);
						}
					}
					else if(adapterGrid != null){
						for (int i=0;i<documentsGrid.size();i++){
							handleList.add(documentsGrid.get(i).getHandle());
						}
					}

					clearSelections();
					NodeController nC = new NodeController(context);
					nC.chooseLocationToMoveNodes(handleList);
					break;
				}
				case R.id.cab_menu_share_link:{
					logDebug("Public link option");
					clearSelections();
					if(adapterList!=null){
						if (documentsList.size()==1){
							//MegaNode n = megaApi.getNodeByHandle(documentsList.get(0).handle);
							//NodeController nC = new NodeController(context);
							//nC.exportLink(n);
							if(documentsList.get(0)==null){
								logWarning("The selected node is NULL");
								break;
							}
							((ManagerActivityLollipop) context).showGetLinkActivity(documentsList.get(0).handle);
						}
					}
					else if(adapterGrid != null){
						if (documentsGrid.size()==1){

							if(documentsGrid.get(0)==null){
								logWarning("The selected node is NULL");
								break;
							}
							((ManagerActivityLollipop) context).showGetLinkActivity(documentsGrid.get(0).getHandle());

							//MegaNode n = megaApi.getNodeByHandle(documentsGrid.get(0).getHandle());
							//NodeController nC = new NodeController(context);
							//nC.exportLink(n);
						}
					}

					break;
				}

				case R.id.cab_menu_share_link_remove:{

					logDebug("Remove public link option");

					clearSelections();
					if(adapterList!=null){
						if (documentsList.size()==1){
							MegaNode n = megaApi.getNodeByHandle(documentsList.get(0).handle);
							//NodeController nC = new NodeController(context);
							//nC.removeLink(n);

							if(documentsList.get(0)==null){
								logWarning("The selected node is NULL");
								break;
							}
							((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(n);
						}
					}
					else if(adapterGrid != null){
						if (documentsGrid.size()==1){
							MegaNode n = megaApi.getNodeByHandle(documentsGrid.get(0).getHandle());
							//NodeController nC = new NodeController(context);
							//nC.removeLink(n);

							if(documentsGrid.get(0)==null){
								logWarning("The selected node is NULL");
								break;
							}
							((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(n);

						}
					}

					break;
				}
				case R.id.cab_menu_trash:{
					ArrayList<Long> handleList = new ArrayList<Long>();

					if(adapterList!=null){
						for (int i=0;i<documentsList.size();i++){
							handleList.add(documentsList.get(i).handle);
						}
					}
					else if(adapterGrid != null){
						for (int i=0;i<documentsGrid.size();i++){
							handleList.add(documentsGrid.get(i).getHandle());
						}
					}
					clearSelections();
					((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
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
			((ManagerActivityLollipop)context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
			if (type == TYPE_CAMERA) {
				((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
			}
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logDebug("onDestroyActionMode");
			clearSelections();
			if (type == TYPE_CAMERA) {
				((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
			}
			if(((ManagerActivityLollipop)context).isListCameraUploads()){
				if(adapterList!=null){
					adapterList.setMultipleSelect(false);
				}
			}
			else{
				if(adapterGrid!=null){
					adapterGrid.setMultipleSelect(false);
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				final Window window = ((ManagerActivityLollipop) context).getWindow();
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						window.setStatusBarColor(0);
					}
				}, 350);
			}
			checkScroll();
			((ManagerActivityLollipop) context).setDrawerLockMode(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			logDebug("onPrepareActionMode");
			boolean showDownload = false;
			boolean showRename = false;
			boolean showCopy = false;
			boolean showMove = false;
			boolean showLink = false;
			boolean showTrash = false;
			boolean showRemoveLink = false;

			if(adapterList!=null) {
				logDebug("LIST onPrepareActionMode");

				List<PhotoSyncHolder> selected = adapterList.getSelectedDocuments();

				// Link
				if ((selected.size() == 1) && (megaApi.checkAccess(megaApi.getNodeByHandle(selected.get(0).handle), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {

					if(megaApi.getNodeByHandle(selected.get(0).handle).isExported()){
						//Node has public link
						showRemoveLink=true;
						showLink=false;

					}
					else{
						showRemoveLink=false;
						showLink=true;
					}

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

					if(selected.size() >= nodes.size()){
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

				if(showCopy){
					menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}

				if(showDownload){
					menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
				if(showLink){
					menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
				if(showRemoveLink){
					menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
				if(showMove){
					if(selected.size()==1){
						menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					}else{
						menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					}
				}
				menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
				menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
				menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
				menu.findItem(R.id.cab_menu_move).setVisible(showMove);
				menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
				menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);

				menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
				menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			}
			else if(adapterGrid!=null){
				logDebug("GRID onPrepareActionMode");
				List<MegaNode> selected = adapterGrid.getSelectedDocuments();

				// Link
				if ((selected.size() == 1) && (megaApi.checkAccess(megaApi.getNodeByHandle(selected.get(0).getHandle()), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
					if(megaApi.getNodeByHandle(selected.get(0).getHandle()).isExported()){
						//Node has public link
						showRemoveLink=true;
						showLink=false;

					}
					else{
						showRemoveLink=false;
						showLink=true;
					}
				}

				if (selected.size() > 0) {
					showDownload = true;
					showTrash = true;
					showMove = true;
					showCopy = true;
					for(int i=0; i<selected.size();i++)	{
						if(megaApi.checkMove(megaApi.getNodeByHandle(selected.get(i).getHandle()), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
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

				if(showCopy){
					menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}

				if(showDownload){
					menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}

				if(showLink){
					menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}
				if(showRemoveLink){
					menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				}

				if(showMove){
					if(selected.size()==1){
						menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
					}else{
						menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					}
				}


			}
			else{
				logWarning("NULL adapters");
			}

			menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);

			menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
			menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);

			menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

			return false;
		}

	}

	//int TYPE_CAMERA= 0;
	//int TYPE_MEDIA = 1;
	public static CameraUploadFragmentLollipop newInstance(int type) {
		logDebug("New instance - Type: "+type);
		CameraUploadFragmentLollipop myFragment = new CameraUploadFragmentLollipop();

	    Bundle args = new Bundle();
	    args.putInt("type", type);
	    myFragment.setArguments(args);

	    return myFragment;
	}	
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		handler = new Handler();
		
		dbH = DatabaseHandler.getDbHandler(context);
		prefs = dbH.getPreferences();
		
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			type= getArguments().getInt("type", TYPE_MEDIA);
		}
		else{
			type=TYPE_CAMERA;
		}

		if (prefs != null) {
			logDebug("prefs != null");
			if (prefs.getStorageAskAlways() != null) {
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())) {
					logDebug("askMe==false");
					if (type == TYPE_CAMERA) {
						if (prefs.getCamSyncEnabled() != null) {
							if (prefs.getCamSyncEnabled().compareTo("") != 0) {
								if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
									if (prefs.getCamSyncLocalPath() != null) {
										if (prefs.getCamSyncLocalPath().compareTo("") != 0) {
											defaultPath = prefs.getCamSyncLocalPath();
										}
									}
								}
							}
						}
					}
					else {
						if (prefs.getSecondaryMediaFolderEnabled() != null) {
							if (prefs.getSecondaryMediaFolderEnabled().compareTo("") != 0) {
								if (Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled())) {
									if (prefs.getLocalPathSecondaryFolder() != null) {
										if (prefs.getLocalPathSecondaryFolder().compareTo("") != 0) {
											defaultPath = prefs.getLocalPathSecondaryFolder();
										}
									}
								}
							}
						}
					}
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}

		logDebug("After recovering bundle type: " + type);
	}

	public void checkScroll () {
		boolean isMultipleSelect = false;
		if ((((ManagerActivityLollipop) context).isListCameraUploads && adapterList != null && adapterList.isMultipleSelect()) || (adapterGrid != null && adapterGrid.isMultipleSelect())) {
			isMultipleSelect = true;
		}
		if (listView != null) {
			if (listView.canScrollVertically(-1) || isMultipleSelect) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		if(!isAdded()){
			return null;
		}
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		if (aB == null){
			aB = ((AppCompatActivity)context).getSupportActionBar();
		}
		
		if (megaApi.getRootNode() == null){
			return null;
		}
		
		prefs = dbH.getPreferences();
		logDebug("Value of isList: " + ((ManagerActivityLollipop)context).isListCameraUploads());
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = getScaleW(outMetrics, density);
	    scaleH = getScaleH(outMetrics, density);

		((ManagerActivityLollipop) context).supportInvalidateOptionsMenu();

		if (type == TYPE_CAMERA) {
			if (((ManagerActivityLollipop) context).getFirstLogin()) {
				((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
				setInitialPreferences();
				View v = inflater.inflate(R.layout.activity_cam_sync_initial, container, false);
				scrollView = (ScrollView) v.findViewById(R.id.cam_sync_scroll_view);
				new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
					@Override
					public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
						if (scrollView.canScrollVertically(-1)){
							((ManagerActivityLollipop) context).changeActionBarElevation(true);
						}
						else {
							((ManagerActivityLollipop) context).changeActionBarElevation(false);
						}
					}
				});

				initialImageView = (ImageView) v.findViewById(R.id.cam_sync_image_view);

				bOK = (TextView) v.findViewById(R.id.cam_sync_button_ok);
				bSkip = (TextView) v.findViewById(R.id.cam_sync_button_skip);
				switchCellularConnection = (SwitchCompat) v.findViewById(R.id.cellular_connection_switch);
				switchUploadVideos = (SwitchCompat) v.findViewById(R.id.upload_videos_switch);

				bSkip.setText(getString(R.string.cam_sync_skip));
				bOK.setText(getString(R.string.cam_sync_ok));
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					bSkip.setBackground(ContextCompat.getDrawable(context, R.drawable.white_rounded_corners_button));
					bOK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
				} else {
					bSkip.setBackgroundResource(R.drawable.black_button_border);
				}

				bOK.setOnClickListener(this);
				bSkip.setOnClickListener(this);

				return v;
			}
		}

		if (((ManagerActivityLollipop) context).isListCameraUploads()) {
			View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);

			listView = (RecyclerView) v.findViewById(R.id.file_list_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			mLayoutManager = new MegaLinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);

			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator());
			listView.addItemDecoration(new DividerItemDecorationV2(context, outMetrics));

			listView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
			listView.setClipToPadding(false);
			listView.setHasFixedSize(true);
			listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			relativeLayoutTurnOnOff = v.findViewById(R.id.relative_layout_file_list_browser_camera_upload_on_off);
			turnOnOff = v.findViewById(R.id.file_list_browser_camera_upload_on_off);
			relativeLayoutTurnOnOff.setVisibility(View.VISIBLE);
			if (type == TYPE_CAMERA) {
				turnOnOff.setText(getString(R.string.settings_camera_upload_turn_on).toUpperCase(Locale.getDefault()));
			} else {
				turnOnOff.setText(getString(R.string.settings_set_up_automatic_uploads).toUpperCase(Locale.getDefault()));
			}

			transfersOverViewLayout = (RelativeLayout) v.findViewById(R.id.transfers_overview_item_layout);
			transfersOverViewLayout.setVisibility(View.GONE);

			boolean camEnabled = false;
			prefs = dbH.getPreferences();
			if (prefs != null) {
				if (prefs.getCamSyncEnabled() != null) {
					if (Boolean.parseBoolean(prefs.getCamSyncEnabled())) {
						logDebug("Hide option Turn on Camera Uploads");
						relativeLayoutTurnOnOff.setVisibility(View.GONE);
						camEnabled = true;
					} else {
						logDebug("SHOW option Turn on Camera Uploads");
						relativeLayoutTurnOnOff.setVisibility(View.VISIBLE);
						camEnabled = false;
					}
				}
			}
			relativeLayoutTurnOnOff.setOnClickListener(this);

//			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_text_layout);
//			contentTextLayout.setVisibility(View.GONE);

			emptyImageView = (ImageView) v.findViewById(R.id.file_list_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.file_list_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_list_empty_text_first);

			if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				emptyImageView.setImageResource(R.drawable.uploads_empty_landscape);
			} else {
				emptyImageView.setImageResource(R.drawable.ic_empty_camera_uploads);
			}

			showEmptyView();

			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);

			if (megaApi.getRootNode() == null) {
				return v;
			}

			if (type == TYPE_CAMERA) {
				if (prefs == null) {
					photosyncHandle = -1;
				} else {
					//The "PhotoSync" folder exists?
					if (prefs.getCamSyncHandle() == null) {
						photosyncHandle = -1;
					} else {
						photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
						if (megaApi.getNodeByHandle(photosyncHandle) == null) {
							photosyncHandle = -1;
						}
					}
				}

				if (photosyncHandle == -1) {
					ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
					for (int i = 0; i < nl.size(); i++) {
						if ((CameraUploadsService.CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())) {
							photosyncHandle = nl.get(i).getHandle();
							dbH.setCamSyncHandle(photosyncHandle);
							listView.setVisibility(View.VISIBLE);
							emptyImageView.setVisibility(View.GONE);
							emptyTextView.setVisibility(View.GONE);
							break;
						}
					}
				}
			} else {
				photosyncHandle = Long.parseLong(prefs.getMegaHandleSecondaryFolder());
				if (megaApi.getNodeByHandle(photosyncHandle) == null) {
					photosyncHandle = -1;
				}
			}


			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);

			if (nodesArray != null) {
				nodesArray.clear();
			}

			if(!((ManagerActivityLollipop)context).getIsSearchEnabled()) {
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
			}
			else{
				searchNodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
				searchByDate = ((ManagerActivityLollipop)context).getTypeOfSearch();
				nodes = searchDate(searchByDate,searchNodes);
			}

			if (megaApi.getNodeByHandle(photosyncHandle) != null) {

				int month = 0;
				int year = 0;
				for (int i = 0; i < nodes.size(); i++) {
					if (nodes.get(i).isFolder()) {
						continue;
					}

					if (!MimeTypeList.typeForName(nodes.get(i).getName()).isImage() && (!MimeTypeList.typeForName(nodes.get(i).getName()).isVideo())) {
						continue;
					}

					PhotoSyncHolder psh = new PhotoSyncHolder();
					Date d = new Date(nodes.get(i).getModificationTime() * 1000);
					if ((month == d.getMonth()) && (year == d.getYear())) {
						psh.isNode = true;
						psh.handle = nodes.get(i).getHandle();
						month = d.getMonth();
						year = d.getYear();
						psh.nodeDate = getImageDateString(month, year);
						nodesArray.add(psh);
					} else {
						month = d.getMonth();
						year = d.getYear();
						psh.isNode = false;
						psh.monthYear = getImageDateString(month, year);
						nodesArray.add(psh);
						psh = new PhotoSyncHolder();
						psh.isNode = true;
						psh.handle = nodes.get(i).getHandle();
						nodesArray.add(psh);
						logDebug("MONTH: " + d.getMonth() + "YEAR: " + d.getYear());
					}
				}

				if (nodesArray.size() == 0) {
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				} else {
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}

			}else{
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}

			if (adapterList == null) {
				adapterList = new MegaPhotoSyncListAdapterLollipop(context, nodesArray, photosyncHandle, listView, emptyImageView, emptyTextView, aB, nodes, this, CAMERA_UPLOAD_ADAPTER);
			} else {
				if (context != adapterList.getContext()) {
					logDebug("Attached activity changed");
					adapterList.setContext(context);
					actionMode = null;
				}
				if (listView != adapterList.getListFragment()) {
					logDebug("Attached ListView changed");
					adapterList.setListFragment(listView);
				}
				adapterList.setNodes(nodesArray, nodes);
			}

			adapterList.setMultipleSelect(false);

			listView.setAdapter(adapterList);
			fastScroller.setRecyclerView(listView);
			visibilityFastScroller();

			return v;
		} else {
			View v = inflater.inflate(R.layout.fragment_filebrowsergrid_camerauploads, container, false);

			listView = (RecyclerView) v.findViewById(R.id.file_grid_view_browser);
			fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

			listView.setDrawingCacheEnabled(true);
			listView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
			listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			relativeLayoutTurnOnOff = v.findViewById(R.id.relative_layout_file_grid_browser_camera_upload_on_off);
			turnOnOff = v.findViewById(R.id.file_grid_browser_camera_upload_on_off);
			relativeLayoutTurnOnOff.setVisibility(View.VISIBLE);
			if (type == TYPE_CAMERA) {
				turnOnOff.setText(getString(R.string.settings_camera_upload_turn_on).toUpperCase(Locale.getDefault()));
			} else {
				turnOnOff.setText(getString(R.string.settings_set_up_automatic_uploads).toUpperCase(Locale.getDefault()));
			}

//			turnOnOff.setGravity(Gravity.CENTER);

			boolean camEnabled = false;
			prefs = dbH.getPreferences();
			if (prefs != null) {
				if (prefs.getCamSyncEnabled() != null) {
					if (Boolean.parseBoolean(prefs.getCamSyncEnabled())) {
						relativeLayoutTurnOnOff.setVisibility(View.GONE);
						camEnabled = true;
					} else {
						camEnabled = false;
						relativeLayoutTurnOnOff.setVisibility(View.VISIBLE);
					}
				}
			}
			relativeLayoutTurnOnOff.setOnClickListener(this);

//			contentTextLayout = (RelativeLayout) v.findViewById(R.id.content_grid_text_layout);
//			contentTextLayout.setVisibility(View.GONE);

			fragmentContainer = (RelativeLayout) v.findViewById(R.id.fragment_container_file_browser_grid);
			fragmentContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.white));

//			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) listView.getLayoutParams();
//			p.addRule(RelativeLayout.ABOVE, R.id.file_grid_browser_camera_upload_on_off);
//			listView.setLayoutParams(p);

			emptyImageView = (ImageView) v.findViewById(R.id.file_grid_empty_image);
			emptyTextView = (LinearLayout) v.findViewById(R.id.file_grid_empty_text);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.file_grid_empty_text_first);

			if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				emptyImageView.setImageResource(R.drawable.uploads_empty_landscape);
			} else {
				emptyImageView.setImageResource(R.drawable.ic_empty_camera_uploads);
			}

			showEmptyView();

			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);

			if (megaApi.getRootNode() == null) {
				return v;
			}

			if (type == TYPE_CAMERA) {
				if (prefs == null) {
					photosyncHandle = -1;
				} else {
					//The "PhotoSync" folder exists?
					if (prefs.getCamSyncHandle() == null) {
						photosyncHandle = -1;
					} else {
						photosyncHandle = Long.parseLong(prefs.getCamSyncHandle());
						if (megaApi.getNodeByHandle(photosyncHandle) == null) {
							photosyncHandle = -1;
						}
					}
				}

				if (photosyncHandle == -1) {
					ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
					for (int i = 0; i < nl.size(); i++) {
						if ((CameraUploadsService.CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())) {
							photosyncHandle = nl.get(i).getHandle();
							dbH.setCamSyncHandle(photosyncHandle);
							listView.setVisibility(View.VISIBLE);
							emptyImageView.setVisibility(View.GONE);
							emptyTextView.setVisibility(View.GONE);
							break;
						}
					}
				}
			} else {
				photosyncHandle = Long.parseLong(prefs.getMegaHandleSecondaryFolder());
				if (megaApi.getNodeByHandle(photosyncHandle) == null) {
					photosyncHandle = -1;
				}
			}

			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);

			int totalWidth = outMetrics.widthPixels;

			int gridWidth = 0;
			int realGridWidth = 0;
			int numberOfCells = 0;
			int padding = 0;
			if (((ManagerActivityLollipop) context).isSmallGridCameraUploads) {
				realGridWidth = totalWidth / GRID_SMALL;
				padding = MegaPhotoSyncGridTitleAdapterLollipop.PADDING_GRID_SMALL;
				gridWidth = realGridWidth - (padding * 2);
				numberOfCells = GRID_SMALL;
			} else {
				realGridWidth = totalWidth / GRID_LARGE;
				padding = MegaPhotoSyncGridTitleAdapterLollipop.PADDING_GRID_LARGE;
				gridWidth = realGridWidth - (padding * 2);
				numberOfCells = GRID_LARGE;
			}

//		    int numberOfCells = totalWidth / GRID_WIDTH;
//		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//		    	if (numberOfCells < 4){
//					numberOfCells = 4;
//				}	
//		    }
//		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//		    	if (numberOfCells < 3){
//					numberOfCells = 3;
//				}	
//		    }


			if (monthPics != null) {
				monthPics.clear();
			}


			List<MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation> itemInformationList = new ArrayList<>();
			int countTitles = 0;

			if(!((ManagerActivityLollipop)context).getIsSearchEnabled()) {
				nodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
			}
			else{
				searchNodes = megaApi.getChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
				searchByDate = ((ManagerActivityLollipop)context).getTypeOfSearch();
				nodes = searchDate(searchByDate,searchNodes);
			}
			if (megaApi.getNodeByHandle(photosyncHandle) != null) {

//				MegaChildren children = megaApi.getFileFolderChildren(megaApi.getNodeByHandle(photosyncHandle), MegaApiJava.ORDER_MODIFICATION_DESC);
//				nodes = children.getFileList();
				itemInformationList = new ArrayList<>(this.nodes.size());
				int month = 0;
				int year = 0;
				MegaMonthPicLollipop monthPic = new MegaMonthPicLollipop();
				boolean thereAreImages = false;
				for (int i = 0; i < nodes.size(); i++) {
					MegaNode n = nodes.get(i);
					if (n.isFolder()) {
						continue;
					}

					if (!MimeTypeList.typeForName(n.getName()).isImage() && (!MimeTypeList.typeForName(n.getName()).isVideo())) {
						continue;
					}
					thereAreImages = true;

					Date d = new Date(n.getModificationTime() * 1000);
					if ((month == 0) && (year == 0)) {
						month = d.getMonth();
						year = d.getYear();
						monthPic.monthYearString = getImageDateString(month, year);
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE, monthPic.monthYearString, monthPic));
						countTitles++;
						monthPic.nodeHandles.add(n.getHandle());
						monthPic.setPosition(n, i);
						if (!isVideoFile(n.getName())) {
							itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE, n, monthPic));
						} else {
							itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO, n, monthPic));
						}

					} else if ((month == d.getMonth()) && (year == d.getYear())) {

						monthPic.nodeHandles.add(n.getHandle());
						monthPic.setPosition(n, i);
//						month = d.getMonth();
//						year = d.getYear();
						monthPic.monthYearString = getImageDateString(month, year);

						if (!isVideoFile(n.getName())) {
							itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE, n, monthPic));
						} else {
							itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO, n, monthPic));
						}
					} else {
						month = d.getMonth();
						year = d.getYear();
						monthPics.add(monthPic);
						monthPic = new MegaMonthPicLollipop();
						monthPic.monthYearString = getImageDateString(month, year);
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE, monthPic.monthYearString, monthPic));
						countTitles++;
						monthPic.nodeHandles.add(n.getHandle());
						monthPic.setPosition(n, i);
						if (!isVideoFile(n.getName())) {
							itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE, n, monthPic));
						} else {
							itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO, n, monthPic));
						}
//						monthPics.add(monthPic);
//						monthPic = new MegaMonthPicLollipop();
//						i--;
					}
				}
				if (nodes.size() > 0) {
					monthPics.add(monthPic);
				}

				if (!thereAreImages) {
					monthPics.clear();
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					listView.setVisibility(View.GONE);
				} else {

					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
			} else {
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}

//			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && numberOfCells == GRID_SMALL){
//				log("the device is portrait and the grid is small");
//				listView.setItemViewCacheSize(numberOfCells * 20);
//			}

			if (adapterGrid == null) {
				logDebug("ADAPTERGRID.MONTHPICS(NEW) = " + monthPics.size());
				adapterGrid = new MegaPhotoSyncGridTitleAdapterLollipop(context, monthPics, photosyncHandle, listView, emptyImageView, emptyTextView, aB, nodes, numberOfCells, gridWidth, this, CAMERA_UPLOAD_ADAPTER, itemInformationList.size(), countTitles, itemInformationList, defaultPath);
				adapterGrid.setHasStableIds(true);
			} else {
				logDebug("ADAPTERGRID.MONTHPICS = " + monthPics.size());

				if (adapterGrid.getContext() != context) {
					logDebug("Attached activity changed");
					adapterGrid.setContext(context);
				}

				adapterGrid.setNumberOfCells(numberOfCells, gridWidth);
				adapterGrid.setNodes(monthPics, nodes, itemInformationList.size(), countTitles, itemInformationList);
			}

//			mLayoutManager = new StaggeredGridLayoutManager(numberOfCells, StaggeredGridLayoutManager.HORIZONTAL | StaggeredGridLayoutManager.VERTICAL);
//			listView.setLayoutManager(mLayoutManager);

			mLayoutManager = new GridLayoutManager(context, numberOfCells);
			((GridLayoutManager) mLayoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
				@Override
				public int getSpanSize(int position) {
					return adapterGrid.getSpanSizeOfPosition(position);
				}
			});


			listView.setLayoutManager(mLayoutManager);

			listView.setAdapter(adapterGrid);
			fastScroller.setRecyclerView(listView);
			visibilityFastScroller();
			return v;
		}
	}

	private void showEmptyView() {
		if (((ManagerActivityLollipop) context).getIsSearchEnabled()) {
			showEmptySearchResults();
		} else {
			showEmptyResults();
		}
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		reDoTheSelectionAfterRotation();
	}

	private void reDoTheSelectionAfterRotation() {
		if (((ManagerActivityLollipop) context).isListCameraUploads()) {
			if (adapterList != null && adapterList.getSelectedDocuments().size() > 0) {
				logDebug("There is previous selected items, we need to redo the selection");
				activateActionMode();
				updateActionModeTitle();
			}
		} else {
			if (adapterGrid != null && adapterGrid.getSelectedDocuments().size() > 0) {
				logDebug("There is previous selected items, we need to redo the selection");
				adapterGrid.refreshActionModeTitle();
			}
		}

	}

	public void selectAll(){
		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			if (adapterList != null){
				if(adapterList.isMultipleSelect()){
					adapterList.selectAll();
				}
				else{
					adapterList.setMultipleSelect(true);
					adapterList.selectAll();
					
					actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
				}
				
				updateActionModeTitle();

			}
		}
		else{
			if (adapterGrid != null){
				if(adapterGrid.isMultipleSelect()){
					adapterGrid.selectAll();
				}
				else{
					adapterGrid.setMultipleSelect(true);
					adapterGrid.selectAll();
				}
			}
		}
	}
	
	public void setInitialPreferences(){
		logDebug("setInitialPreferences");
//		DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		dbH.setFirstTime(false);
//		dbH.setCamSyncEnabled(false);
		dbH.setStorageAskAlways(false);
		File defaultDownloadLocation = buildDefaultDownloadDir(context);
		defaultDownloadLocation.mkdirs();
		
		dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
		dbH.setPinLockEnabled(false);
		dbH.setPinLockCode("");

		ArrayList<MegaNode> nodeLinks = megaApi.getPublicLinks();
		if(nodeLinks == null || nodeLinks.size() == 0){
			logDebug("No public links: showCopyright set true");
			dbH.setShowCopyright(true);
		} else {
			logDebug("Already public links: showCopyright set false");
			dbH.setShowCopyright(false);
		}
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
	public void onAttach(Context context) {
		logDebug("onAttach");

		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)context).getSupportActionBar();
	}
	
	@SuppressLint("NewApi")
	public void cameraOnOffFirstTime(){
		((ManagerActivityLollipop) context).setFirstLogin(false);
//		firstTimeCam = false;
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		dbH.setCamSyncEnabled(true);
		File localFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		String localPath = localFile.getAbsolutePath();
		dbH.setCamSyncLocalPath(localPath);
		dbH.setCameraFolderExternalSDCard(false);
		if (switchCellularConnection.isChecked()){
			dbH.setCamSyncWifi(false);
		}
		else{
			dbH.setCamSyncWifi(true);
		}
		if(switchUploadVideos.isChecked()){
			dbH.setCamSyncFileUpload(MegaPreferences.PHOTOS_AND_VIDEOS);
		}
		else{
			dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
		}
  
		saveCompressionSettings();
        startCU();
		
		((ManagerActivityLollipop)context).refreshCameraUpload();
	}
	
	@SuppressLint("NewApi")
	public void cameraOnOff(){
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
			resetCUTimeStampsAndCache();
            dbH.setCamSyncEnabled(false);
			dbH.deleteAllSyncRecords(SyncRecord.TYPE_ANY);
            stopRunningCameraUploadService(context);
			((ManagerActivityLollipop)context).refreshCameraUpload();
		}
		else{
            prefs = dbH.getPreferences();
            if (prefs != null &&
                    !TextUtils.isEmpty(prefs.getCamSyncLocalPath()) &&
                    !TextUtils.isEmpty(prefs.getCamSyncFileUpload()) &&
                    !TextUtils.isEmpty(prefs.getCamSyncWifi())
            ) {
                resetCUTimeStampsAndCache();
                dbH.setCamSyncEnabled(true);
                dbH.deleteAllSyncRecords(SyncRecord.TYPE_ANY);
                
                //video quality
                saveCompressionSettings();
                startCU();
                ((ManagerActivityLollipop)context).refreshCameraUpload();
                
                return;
            }
			
			AlertDialog wifiDialog;
			
			final ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.select_dialog_singlechoice, android.R.id.text1, new String[] {getResources().getString(R.string.cam_sync_wifi), getResources().getString(R.string.cam_sync_data)});
			AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
								
			
			builder.setTitle(getString(R.string.section_photo_sync));
			builder.setSingleChoiceItems(adapter,  0,  new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					logDebug("AlertDialog");
					resetCUTimeStampsAndCache();
                    dbH.setCamSyncEnabled(true);
					dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
                    File localFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
					String localPath = localFile.getAbsolutePath();
					dbH.setCamSyncLocalPath(localPath);
					dbH.setCameraFolderExternalSDCard(false);
                    startCU();
				
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
		}
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_ON_OFF:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    cameraOnOff();
                }
        
                break;
            }
    
            case REQUEST_CAMERA_ON_OFF_FIRST_TIME:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    cameraOnOffFirstTime();
                }
        
                break;
            }
        }
    }
	
	@SuppressLint("NewApi")
	@Override
    public void onClick(View v) {
        ((MegaApplication)((Activity)context).getApplication()).sendSignalPresenceActivity();
        String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE};
        
        switch (v.getId()) {
            case R.id.relative_layout_file_grid_browser_camera_upload_on_off:
            case R.id.relative_layout_file_list_browser_camera_upload_on_off: {
                if (type == TYPE_CAMERA) {
                    if (hasPermissions(context,permissions)) {
                        cameraOnOff();
                    } else {
                        requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF);
                    }
                } else {
                    ((ManagerActivityLollipop)context).moveToSettingsSection();
                }
                break;
            }
            case R.id.cam_sync_button_ok: {
                if (hasPermissions(context,permissions)) {
                    cameraOnOffFirstTime();
                }else{
                    requestCameraUploadPermission(permissions, REQUEST_CAMERA_ON_OFF_FIRST_TIME);
                }
                ((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
                break;
            }
            case R.id.cam_sync_button_skip: {
                toCloudDrive();
                break;
            }
        }
    }

    private void toCloudDrive() {
        ((ManagerActivityLollipop)context).setFirstLogin(false);
        dbH.setCamSyncEnabled(false);
        ((ManagerActivityLollipop)context).setInitialCloudDrive();
    }

    
    private void requestCameraUploadPermission(String[] permissions, int requestCode){
        ActivityCompat.requestPermissions((ManagerActivityLollipop)context,
                permissions,
                requestCode);
    }
	
	public void itemClick(int position, ImageView imageView, int[] screenPosition) {
		
		PhotoSyncHolder psHPosition = nodesArray.get(position);

		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			logDebug("isList");
			if (adapterList.isMultipleSelect()){
				adapterList.toggleSelection(position);
				List<PhotoSyncHolder> documents = adapterList.getSelectedDocuments();
				if (documents.size() > 0){
					updateActionModeTitle();
				}
				else{
					clearSelections();
				}
			}
			else{
				if (psHPosition.isNode){
					MegaNode psHMegaNode = megaApi.getNodeByHandle(psHPosition.handle);
					if (psHMegaNode != null){
						int positionInNodes = 0;
						for (int i=0;i<nodes.size();i++){
							if(nodes.get(i).getHandle() == psHMegaNode.getHandle()){
								positionInNodes = i;
							}
						}
						if (MimeTypeList.typeForName(psHMegaNode.getName()).isImage()){
							Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
							intent.putExtra("position", positionInNodes);
							if(((ManagerActivityLollipop)context).isFirstNavigationLevel() == true){
								intent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);
								arrayHandles = null;

							}else{
								intent.putExtra("adapterType", SEARCH_BY_ADAPTER);
								arrayHandles = new long[nodes.size()];
								for(int i = 0; i < nodes.size(); i++) {
									arrayHandles[i] = nodes.get(i).getHandle();
								}
								intent.putExtra("handlesNodesSearch",arrayHandles);

							}

							intent.putExtra("isFolderLink", false);
							if (megaApi.getParentNode(psHMegaNode).getType() == MegaNode.TYPE_ROOT){
								intent.putExtra("parentNodeHandle", -1L);
							}
							else{
								intent.putExtra("parentNodeHandle", megaApi.getParentNode(psHMegaNode).getHandle());
							}

							intent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCamera);
							intent.putExtra("screenPosition", screenPosition);
							startActivity(intent);
							((ManagerActivityLollipop) context).overridePendingTransition(0,0);
							imageDrag = imageView;
						}
						else if (MimeTypeList.typeForName(psHMegaNode.getName()).isVideoReproducible()){

							String mimeType = MimeTypeList.typeForName(psHMegaNode.getName()).getType();
							logDebug("FILE HANDLE: " + psHMegaNode.getHandle());

							Intent mediaIntent;
							boolean internalIntent;
							if (MimeTypeList.typeForName(psHMegaNode.getName()).isVideoNotSupported()){
								mediaIntent = new Intent(Intent.ACTION_VIEW);
								internalIntent = false;
							}
							else {
								mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
								internalIntent = true;
							}
							mediaIntent.putExtra("position", positionInNodes);
							if (megaApi.getParentNode(psHMegaNode).getType() == MegaNode.TYPE_ROOT){
								mediaIntent.putExtra("parentNodeHandle", -1L);
							}
							else{
								mediaIntent.putExtra("parentNodeHandle", megaApi.getParentNode(psHMegaNode).getHandle());
							}
							mediaIntent.putExtra("orderGetChildren", ((ManagerActivityLollipop)context).orderCamera);
							mediaIntent.putExtra("adapterType", FILE_BROWSER_ADAPTER);
							mediaIntent.putExtra("HANDLE", psHMegaNode.getHandle());
							mediaIntent.putExtra("FILENAME", psHMegaNode.getName());
							mediaIntent.putExtra("screenPosition", screenPosition);
							if(((ManagerActivityLollipop)context).isFirstNavigationLevel() == true){
								mediaIntent.putExtra("adapterType", PHOTO_SYNC_ADAPTER);
								arrayHandles = null;

							}else{
								mediaIntent.putExtra("adapterType", SEARCH_BY_ADAPTER);
								arrayHandles = new long[nodes.size()];
								for(int i = 0; i < nodes.size(); i++) {
									arrayHandles[i] = nodes.get(i).getHandle();
								}
								mediaIntent.putExtra("handlesNodesSearch",arrayHandles);

							}
							String localPath = findVideoLocalPath(context, psHMegaNode);
                            if (localPath != null && checkFingerprint(megaApi,psHMegaNode,localPath)) {
								File mediaFile = new File(localPath);

								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
									mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(psHMegaNode.getName()).getType());
								}
								else{
									mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(psHMegaNode.getName()).getType());
								}
								mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							}
							else {
								if (megaApi.httpServerIsRunning() == 0) {
									megaApi.httpServerStart();
								}

								ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
								ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
								activityManager.getMemoryInfo(mi);

								if(mi.totalMem>BUFFER_COMP){
									logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
									megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
								}
								else{
									logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
									megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
								}

								String url = megaApi.httpServerGetLocalLink(psHMegaNode);
								mediaIntent.setDataAndType(Uri.parse(url), mimeType);
							}
							if (internalIntent) {
								context.startActivity(mediaIntent);
							}
							else {
								if (isIntentAvailable(context, mediaIntent)) {
									context.startActivity(mediaIntent);
								} else {
									((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.intent_not_available), -1);
									adapterList.notifyDataSetChanged();
									ArrayList<Long> handleList = new ArrayList<Long>();
									handleList.add(psHMegaNode.getHandle());
									NodeController nC = new NodeController(context);
									nC.prepareForDownload(handleList, true);
								}
							}
							((ManagerActivityLollipop) context).overridePendingTransition(0,0);
							imageDrag = imageView;
						}
						else{
							adapterList.notifyDataSetChanged();
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(psHMegaNode.getHandle());
							NodeController nC = new NodeController(context);
							nC.prepareForDownload(handleList, true);
						}
					}
				}
			}
		}
		else{
			logDebug("isGrid");
		}
	}

	public String getPath (String fileName, long fileSize, String destDir, MegaNode file) {
		logDebug("getPath");
		String path = null;
		if (destDir != null) {
			File dir = new File(destDir);
			File[] listFiles = dir.listFiles();

			if (listFiles != null) {
				for (int i = 0; i < listFiles.length; i++) {
					if (listFiles[i].isDirectory()) {
						path = getPath(fileName, fileSize, listFiles[i].getAbsolutePath(), file);
						if (path != null) {
							return path;
						}
					} else {
						boolean isOnMegaDownloads = false;
						path = getLocalFile(context, fileName, fileSize, downloadLocationDefaultPath);
						File f = new File(downloadLocationDefaultPath, file.getName());
						if (f.exists() && (f.length() == file.getSize())) {
							isOnMegaDownloads = true;
						}
						if (path != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(path))))) {
							return path;
						}
					}
				}
			}
		}

		return null;
	}

	private void clearSelections() {
		logDebug("clearSelections");
		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			if (adapterList != null){
				if(adapterList.isMultipleSelect()){
					adapterList.clearSelections();
					hideMultipleSelect();
				}
				hideMultipleSelect();
				updateActionModeTitle();
			}
		}
		else{
			if (adapterGrid != null){
				if(adapterGrid.isMultipleSelect()){
					adapterGrid.clearSelections();
				}
				hideMultipleSelect();
				updateActionModeTitle();
			}
		}
	}

	private void updateActionModeTitle() {

		logDebug("updateActionModeTitle");
		if (actionMode == null || getActivity() == null) {
			return;
		}

		int files = 0;
		int folders = 0;

		if(adapterList!=null){
			List<PhotoSyncHolder> documents = adapterList.getSelectedDocuments();

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

		}else if(adapterGrid!=null){
			List<MegaNode> documents = adapterGrid.getSelectedDocuments();

			for (MegaNode document : documents) {
				MegaNode n = megaApi.getNodeByHandle(document.getHandle());
				if (n != null){
					if (n.isFile()) {
						files++;
					} else if (n.isFolder()) {
						folders++;
					}
				}
			}
		}

		Resources res = getActivity().getResources();

		String title;
		int sum=files+folders;

		if (files == 0 && folders == 0) {
			title = Integer.toString(sum);
		} else if (files == 0) {
			title = Integer.toString(folders);
		} else if (folders == 0) {
			title = Integer.toString(files);
		} else {
			title = Integer.toString(sum);
		}
		actionMode.setTitle(title);
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			logError("Invalidate error", e);
		}
		// actionMode.
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		logDebug("hideMultipleSelect");
		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			if (adapterList != null){
				adapterList.setMultipleSelect(false);

			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setMultipleSelect(false);

			}
		}

		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public int onBackPressed(){
		logDebug("onBackPressed");

		if(((ManagerActivityLollipop)context).getFirstLogin()){
			((ManagerActivityLollipop) context).setFirstLogin(false);
			dbH.setCamSyncEnabled(false);
			((ManagerActivityLollipop) context).refreshMenu();
		}


		if(((ManagerActivityLollipop)context).isFirstNavigationLevel() == true){
			return 0;
		}else{
			long cameraUploadHandle = getPhotoSyncHandle();
			MegaNode nps = megaApi.getNodeByHandle(cameraUploadHandle);
			if (nps != null) {
				ArrayList<MegaNode> nodes = megaApi.getChildren(nps, MegaApiJava.ORDER_MODIFICATION_DESC);
				setNodes(nodes);

				((ManagerActivityLollipop)context).invalidateOptionsMenu();
				((ManagerActivityLollipop)context).setIsSearchEnabled(false);
				((ManagerActivityLollipop)context).setToolbarTitle();
				return 1;
			}
			return 0;
		}
	}

	public long getPhotoSyncHandle(){

		if (type == TYPE_CAMERA){
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
					if ((CameraUploadsService.CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
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
			
			if (((ManagerActivityLollipop)context).isListCameraUploads()){
				if (adapterList != null){
					adapterList.setPhotoSyncHandle(photosyncHandle);
				}
			}
			else{
				if (adapterGrid != null){
					adapterGrid.setPhotoSyncHandle(photosyncHandle);
				}
			}
			
			return photosyncHandle;
		}
		else if (type == TYPE_MEDIA){
			
			if (prefs == null){
				photosyncHandle = -1;
			}
			else{
				//The "PhotoSync" folder exists?
				if (prefs.getCamSyncHandle() == null){
					photosyncHandle = -1;
				}
				else{
					photosyncHandle = Long.parseLong(prefs.getMegaHandleSecondaryFolder());
					if (megaApi.getNodeByHandle(photosyncHandle) == null){
						photosyncHandle = -1;
					}
				}
			}
		
			if (((ManagerActivityLollipop)context).isListCameraUploads()){
				if (adapterList != null){
					adapterList.setPhotoSyncHandle(photosyncHandle);
				}
			}
			else{
				if (adapterGrid != null){
					adapterGrid.setPhotoSyncHandle(photosyncHandle);
				}
			}
		}
		
		return photosyncHandle;
	}

	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;

		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			this.nodesArray.clear();
			int month = 0;
			int year = 0;
			for (int i=0;i<nodes.size();i++){
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
					logDebug("MONTH: " + d.getMonth() + ", YEAR: " + d.getYear());
				}
			}
			if (adapterList != null){
				adapterList.setNodes(nodesArray, nodes);

				visibilityFastScroller();

				if (adapterList.getItemCount() == 0){
					if (listView != null){
						listView.setVisibility(View.GONE);
						emptyImageView.setVisibility(View.VISIBLE);
						emptyTextView.setVisibility(View.VISIBLE);
					}
				}
				else{
					if (listView != null){
						listView.setVisibility(View.VISIBLE);
						emptyImageView.setVisibility(View.GONE);
						emptyTextView.setVisibility(View.GONE);
					}					

				}			
			}	
		}
		else{
			
			if (outMetrics == null){
				outMetrics = new DisplayMetrics ();
			}

			if (listView == null){
				return;
			}

			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
 
		    int totalWidth = outMetrics.widthPixels;
		    		    
		    int gridWidth = 0;
		    int numberOfCells = 0;
		    if (((ManagerActivityLollipop)context).isSmallGridCameraUploads){
				gridWidth = totalWidth / GRID_SMALL;
				numberOfCells = GRID_SMALL;
		    }else{
				gridWidth = totalWidth / GRID_LARGE;
				numberOfCells = GRID_LARGE;
		    }
		    
			if (monthPics != null){
				monthPics.clear();
			}
			
			int month = 0;
			int year = 0;
			MegaMonthPicLollipop monthPic = new MegaMonthPicLollipop();
			boolean thereAreImages = false;
//			for (int i=0;i<nodes.size();i++){
//				if (nodes.get(i).isFolder()){
//					continue;
//				}
//
//				if (!MimeTypeList.typeForName(nodes.get(i).getName()).isImage() && (!MimeTypeList.typeForName(nodes.get(i).getName()).isVideo())){
//					continue;
//				}
//
//				Date d = new Date(nodes.get(i).getModificationTime()*1000);
//				if ((month == 0) && (year == 0)){
//					month = d.getMonth();
//					year = d.getYear();
//					monthPic.monthYearString = getImageDateString(month, year);
//					monthPics.add(monthPic);
//					monthPic = new MegaMonthPicLollipop();
//					i--;
//				}
//				else if ((month == d.getMonth()) && (year == d.getYear())){
//					thereAreImages = true;
//					if (monthPic.nodeHandles.size() == numberOfCells){
//						monthPics.add(monthPic);
//						monthPic = new MegaMonthPicLollipop();
//						monthPic.nodeHandles.add(nodes.get(i).getHandle());
//					}
//					else{
//						monthPic.nodeHandles.add(nodes.get(i).getHandle());
//					}
//				}
//				else{
//					month = d.getMonth();
//					year = d.getYear();
//					monthPics.add(monthPic);
//					monthPic = new MegaMonthPicLollipop();
//					monthPic.monthYearString = getImageDateString(month, year);
//					monthPics.add(monthPic);
//					monthPic = new MegaMonthPicLollipop();
//					i--;
//				}
//			}
			List<MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation> itemInformationList = new ArrayList<>(nodes.size());
			int countTitles = 0;
			for (int i=0;i<nodes.size();i++){
				MegaNode n = nodes.get(i);
				if (n.isFolder()){
					continue;
				}

				if (!MimeTypeList.typeForName(n.getName()).isImage() && (!MimeTypeList.typeForName(n.getName()).isVideo())){
					continue;
				}

				thereAreImages = true;

				Date d = new Date(n.getModificationTime()*1000);
				if ((month == 0) && (year == 0)){
					month = d.getMonth();
					year = d.getYear();
					monthPic.monthYearString = getImageDateString(month, year);
					itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE, monthPic.monthYearString, monthPic));
					countTitles++;
					monthPic.nodeHandles.add(n.getHandle());
					monthPic.setPosition(n, i);
					if(!isVideoFile(n.getName())){
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE, n, monthPic));
					}
					else{
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO, n, monthPic));
					}
//						monthPics.add(monthPic);
//						monthPic = new MegaMonthPicLollipop();
//						i--;
				}
				else if ((month == d.getMonth()) && (year == d.getYear())){
//						if (monthPic.nodeHandles.size() == numberOfCells){
//							monthPics.add(monthPic);
//							monthPic = new MegaMonthPicLollipop();
//							monthPic.nodeHandles.add(nodes.get(i).getHandle());
//						}
//						else{
					monthPic.nodeHandles.add(n.getHandle());
					monthPic.setPosition(n, i);
					if(!isVideoFile(n.getName())){
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE, n, monthPic));
					}
					else{
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO, n, monthPic));
					}
//						}
				}
				else{
					month = d.getMonth();
					year = d.getYear();
					monthPics.add(monthPic);
					monthPic = new MegaMonthPicLollipop();
					monthPic.monthYearString = getImageDateString(month, year);
					itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE, monthPic.monthYearString, monthPic));
					countTitles++;
					monthPic.nodeHandles.add(n.getHandle());
					monthPic.setPosition(n, i);
					if(!isVideoFile(n.getName())){
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE, n, monthPic));
					}
					else{
						itemInformationList.add(new MegaPhotoSyncGridTitleAdapterLollipop.ItemInformation(MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO, n, monthPic));
					}
//						monthPics.add(monthPic);
//						monthPic = new MegaMonthPicLollipop();
//						i--;
				}
			}
			if (nodes.size() > 0){
				monthPics.add(monthPic);
			}
			visibilityFastScroller();

			if (!thereAreImages){
				monthPics.clear();
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				showEmptyResults();
				listView.setVisibility(View.GONE);
			}
			else{
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			}
			
			if (adapterGrid != null){
				logDebug("ADAPTERGRID.MONTHPICS = " + monthPics.size());
				adapterGrid.setNumberOfCells(numberOfCells, gridWidth);
				adapterGrid.setNodes(monthPics, nodes, itemInformationList.size(), countTitles, itemInformationList);
			}
		}
	}

	public void showEmptySearchResults() {
		emptyTextView.setVisibility(View.VISIBLE);
		emptyTextViewFirst.setText(getText(R.string.no_results_found));
	}

	private void showEmptyResults() {
		String textToShow = String.format(context.getString(R.string.context_empty_camera_uploads));

		try{
			textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
			textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
			textToShow = textToShow.replace("[/B]", "</font>");
		}
		catch (Exception e){}
		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textToShow);
		}
		emptyTextViewFirst.setText(result);
	}

	public void notifyDataSetChanged(){
		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.notifyDataSetChanged();
			}
		}
	}

//	public void setFirstLogin(boolean firstTimeCam){
//		this.firstTimeCam = firstTimeCam;
//	}
//
//	public boolean getFirstLogin(){
//		return firstTimeCam;
//	}

	public boolean showSelectMenuItem(){
		if (((ManagerActivityLollipop)context).isListCameraUploads()){
			if (adapterList != null){
				return adapterList.isMultipleSelect();
			}
		}
		else{
			if (adapterGrid != null){
				return adapterGrid.isMultipleSelect();
			}
		}
		
		return false;
	}

	public int getItemCountList(){
		if(adapterList != null){
			return adapterList.getItemCount();
		}
		return 0;
	}

	public int getItemCountGrid(){
		if(adapterGrid != null){
			return adapterGrid.getItemCount();
		}
		return 0;
	}

	public RecyclerView getRecyclerView(){
		return listView;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			logDebug("Create folder start");
		}		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
			logDebug("Create folder finished");
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, context.getString(R.string.camera_uploads_created), Toast.LENGTH_LONG).show();
				emptyImageView.setVisibility(View.VISIBLE);
				emptyImageView.setOnClickListener(this);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {

	}

	public boolean getIsLargeGrid() {
		boolean isSmall = ((ManagerActivityLollipop)context).isSmallGridCameraUploads;
		boolean isLarge = !isSmall;
		return isLarge;
		//		return ((ManagerActivityLollipop)context).isLargeGridCameraUploads;

	}
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
		}
		
		super.onDestroy();
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
		logDebug("onInterceptTouchEvent");
		return false;
	}

	@Override
	public void onRequestDisallowInterceptTouchEvent(boolean arg0) {

	}

	@Override
	public void onTouchEvent(RecyclerView arg0, MotionEvent arg1) {

	}

	public void visibilityFastScroller(){
		if(nodes == null){
			fastScroller.setVisibility(View.GONE);
		}else{
			if(((ManagerActivityLollipop)context).isSmallGridCameraUploads){
				if (nodes.size() < MIN_ITEMS_SCROLLBAR_GRID) {
					fastScroller.setVisibility(View.GONE);
				} else {
					fastScroller.setVisibility(View.VISIBLE);
				}
			}else {
				if (nodes.size() < MIN_ITEMS_SCROLLBAR) {
					fastScroller.setVisibility(View.GONE);
				} else {
					fastScroller.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	public ArrayList<MegaNode> searchDate(long[] searchByDate, ArrayList<MegaNode> nodes ){

		((ManagerActivityLollipop)context).setIsSearchEnabled(true);
		((ManagerActivityLollipop)context).setToolbarTitle();

		ArrayList<MegaNode> nodesResult = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		Calendar calTo = Calendar.getInstance();

		if(searchByDate[0] == 1){
			logDebug("Option day");

			cal.setTimeInMillis(searchByDate[1]);
			int selectedYear = cal.get(Calendar.YEAR);
			int selectedMonth = (cal.get(Calendar.MONTH));
			int selectedDay = cal.get(Calendar.DAY_OF_MONTH);

			//Title
			SimpleDateFormat titleFormat = new SimpleDateFormat("d MMM");
			Calendar calTitle = Calendar.getInstance();
			calTitle.set(selectedYear, selectedMonth, selectedDay);
			Date date = calTitle.getTime();
			String formattedDate = titleFormat.format(date);
			aB.setTitle(formattedDate);

			int nodeDay, nodeMonth, nodeYear;
			for (MegaNode node : nodes){
				Date d = new Date(node.getModificationTime()*1000);
				Calendar calNode = Calendar.getInstance();
				calNode.setTime(d);
				nodeDay = calNode.get(Calendar.DAY_OF_MONTH);
				nodeMonth = calNode.get(Calendar.MONTH);
				nodeYear = calNode.get(Calendar.YEAR);

				if((selectedYear == nodeYear) && (selectedMonth == nodeMonth) && (selectedDay == nodeDay)){
					nodesResult.add(node);
				}
			}

		}else if(searchByDate[0] == 2){

			if(searchByDate[2] == 1){
				logDebug("Option last month");
				int selectedDay = cal.get(Calendar.DAY_OF_MONTH);
				int selectedMonth = cal.get(Calendar.MONTH);
				int selectedYear = cal.get(Calendar.YEAR);

				if(selectedMonth == 0){
					selectedMonth = 11;
					selectedYear = selectedYear - 1;
				}else{
					selectedMonth = selectedMonth - 1;
				}

				//Title
				SimpleDateFormat titleFormat = new SimpleDateFormat("MMMM");
				Calendar calTitle = Calendar.getInstance();
				calTitle.set(selectedYear, selectedMonth, selectedDay);
				Date date = calTitle.getTime();
				String formattedDate = titleFormat.format(date);
				aB.setTitle(formattedDate);

				int nodeMonth, nodeYear;

				for (MegaNode node : nodes){
					Date d = new Date(node.getModificationTime()*1000);
					Calendar calNode = Calendar.getInstance();
					calNode.setTime(d);
					nodeMonth = calNode.get(Calendar.MONTH);
					nodeYear = calNode.get(Calendar.YEAR);

					if((selectedYear == nodeYear) && (selectedMonth == nodeMonth)){
						nodesResult.add(node);
					}
				}



			}else if(searchByDate[2] == 2){
				logDebug("Option last year");
				int selectedYear = (cal.get(Calendar.YEAR) - 1);

				//Title
				String formattedDate = String.valueOf(selectedYear);
				aB.setTitle(formattedDate);

				int nodeYear;
				for (MegaNode node : nodes){
					Date d = new Date(node.getModificationTime()*1000);
					Calendar calNode = Calendar.getInstance();
					calNode.setTime(d);
					nodeYear = calNode.get(Calendar.YEAR);

					if(selectedYear == nodeYear){
						nodesResult.add(node);
					}
				}
			}

		}else if(searchByDate[0] == 3){
			logDebug("Option period");

			cal.setTimeInMillis(searchByDate[3]);
			int selectedYearFrom = cal.get(Calendar.YEAR);
			int selectedMonthFrom = cal.get(Calendar.MONTH);
			int selectedDayFrom = cal.get(Calendar.DAY_OF_MONTH);

			calTo.setTimeInMillis(searchByDate[4]);
			int selectedYearTo = calTo.get(Calendar.YEAR);
			int selectedMonthTo = calTo.get(Calendar.MONTH);
			int selectedDayTo = calTo.get(Calendar.DAY_OF_MONTH);

			//Title
			SimpleDateFormat titleFormat = new SimpleDateFormat("d MMM");
			Calendar calTitleFrom = Calendar.getInstance();
			Calendar calTitleTo = Calendar.getInstance();

			calTitleFrom.set(selectedYearFrom, selectedMonthFrom, selectedDayFrom);
			calTitleTo.set(selectedYearTo, selectedMonthTo, selectedDayTo);
			Date dateFrom = calTitleFrom.getTime();
			Date dateTo = calTitleTo.getTime();

			String formattedDateFrom = titleFormat.format(dateFrom);
			String formattedDateTo = titleFormat.format(dateTo);

			String formattedDate = formattedDateFrom +" - "+ formattedDateTo;
			aB.setTitle(formattedDate);

			int nodeDay, nodeMonth, nodeYear;

			for (MegaNode node : nodes){
				int period = 0;
				Date d = new Date(node.getModificationTime()*1000);
				Calendar calNode = Calendar.getInstance();
				calNode.setTime(d);
				nodeDay = calNode.get(Calendar.DAY_OF_MONTH);
				nodeMonth = calNode.get(Calendar.MONTH);
				nodeYear = calNode.get(Calendar.YEAR);

				//Period From
				if(selectedYearFrom < nodeYear){
					period ++;
				}else if(selectedYearFrom == nodeYear){
					if(selectedMonthFrom < nodeMonth){
						period ++;
					}else if(selectedMonthFrom == nodeMonth){

						if(selectedDayFrom <= nodeDay){
							period ++;
						}
					}
				}

				//Period To
				if(selectedYearTo > nodeYear){
					period ++;
				}else if(selectedYearTo == nodeYear){
					if(selectedMonthTo > nodeMonth){
						period ++;
					}else if(selectedMonthTo == nodeMonth){

						if(selectedDayTo >= nodeDay){
							period ++;
						}
					}
				}

				if(period == 2){
					nodesResult.add(node);
				}
			}
		}
		return nodesResult;
		//setNodes(nodesResult);
	}

	public MegaPhotoSyncListAdapterLollipop getAdapterList() {
		return adapterList;
	}

	public MegaPhotoSyncGridTitleAdapterLollipop getAdapterGrid() {
		return adapterGrid;
	}

	public ArrayList<MegaMonthPicLollipop> getMonthPics() {
		return monthPics;
	}

	public ArrayList<PhotoSyncHolder> getNodesArray() {
		return nodesArray;
	}
    
    private void resetCUTimeStampsAndCache(){
        dbH.setCamSyncTimeStamp(0);
        dbH.setCamVideoSyncTimeStamp(0);
        dbH.setSecSyncTimeStamp(0);
        dbH.setSecVideoSyncTimeStamp(0);
        dbH.saveShouldClearCamsyncRecords(true);
        purgeDirectory(new File(context.getCacheDir().toString() + File.separator));
    }
    
    private void saveCompressionSettings(){
        dbH.setCameraUploadVideoQuality(MEDIUM);
        dbH.setConversionOnCharging(true);

        dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
    }
    
    private void startCU() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            
            @Override
            public void run() {
				logDebug("Starting CU");
                startCameraUploadService(context);
            }
        },1000);
    }

    public void resetSwitchButtonLabel(){
		relativeLayoutTurnOnOff.setVisibility(View.VISIBLE);
		turnOnOff.setText(getString(R.string.settings_camera_upload_turn_on).toUpperCase(Locale.getDefault()));
	}
}
