package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.SlidingUpPanelLayout;
import mega.privacy.android.app.components.SlidingUpPanelLayout.PanelState;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;


public class ContactFileListFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, MegaRequestListenerInterface {

	MegaApiAndroid megaApi;
	ActionBar aB;
	Context context;
	Object contactFileListFragment = this;

	String userEmail;

	RelativeLayout mainLayout;
	TextView nameView;
	RoundedImageView imageView;
	TextView contactInitialLetter;
	TextView textViewContent;

	RelativeLayout contactLayout;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImageView;
	TextView emptyTextView;
	
	ImageView toolbarBack;

	MegaUser contact;
	ArrayList<MegaNode> contactNodes;

	MegaBrowserLollipopAdapter adapter;

	long parentHandle = -1;
	MegaNode selectedNode = null;

	Stack<Long> parentHandleStack = new Stack<Long>();

	private ActionMode actionMode;
	
	private boolean name = false;
	private boolean firstName = false;
	String nameText;
	String firstNameText;

	ProgressDialog statusDialog;

	public static int REQUEST_CODE_GET = 1000;
	public static int REQUEST_CODE_GET_LOCAL = 1003;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;

	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;

	ArrayList<MegaTransfer> tL;
	HashMap<Long, MegaTransfer> mTHash = null;
	long lastTimeOnTransferUpdate = -1;
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionDownload;
	public LinearLayout optionProperties;
	public LinearLayout optionRename;
//	public LinearLayout optionPublicLink;
//	public LinearLayout optionShare;
//	public LinearLayout optionPermissions;
	public LinearLayout optionDelete;
	public LinearLayout optionRemoveTotal;
//	public LinearLayout optionClearShares;
	public LinearLayout optionLeaveShare;
//	public LinearLayout optionMoveTo;
	public LinearLayout optionCopyTo;	
	public TextView propertiesText;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

//		@Override
//	    public boolean onSingleTapConfirmed(MotionEvent e) {
//	        View view = listView.findChildViewUnder(e.getX(), e.getY());
//	        int position = listView.getChildPosition(view);
//
//	        // handle single tap
//	        itemClick(view, position);
//
//	        return super.onSingleTapConfirmed(e);
//	    }

	    public void onLongPress(MotionEvent e) {
	        View view = listView.findChildViewUnder(e.getX(), e.getY());
	        int position = listView.getChildPosition(view);

	        // handle long press
			if (adapter.getPositionClicked() == -1){
				adapter.setMultipleSelect(true);
			
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
	    }
	}

	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaNode> documents = adapter.getSelectedNodes();

			switch (item.getItemId()) {
				case R.id.cab_menu_download: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i = 0; i < documents.size(); i++) {
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ContactPropertiesActivityLollipop)context).onFileClick(handleList);
					break;
				}
				case R.id.cab_menu_copy: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i = 0; i < documents.size(); i++) {
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
	
					((ContactPropertiesActivityLollipop)context).showCopyLollipop(handleList);
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
				case R.id.cab_menu_leave_multiple_share: {
					ArrayList<Long> handleList = new ArrayList<Long>();
					for (int i=0;i<documents.size();i++){
						handleList.add(documents.get(i).getHandle());
					}
					clearSelections();
					hideMultipleSelect();
					((ContactPropertiesActivityLollipop) context).leaveMultipleShares(handleList);					
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
		public void onDestroyActionMode(ActionMode arg0) {
			adapter.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaNode> selected = adapter.getSelectedNodes();
			boolean showRename = false;
			boolean showMove = false;
			
			// Rename
			if(selected.size() == 1){
				if ((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
					showRename = true;
				}
			}

			if (selected.size() > 0) {
				if ((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
					showMove = true;	
				}				
			}
			
			if (selected.size() != 0) {
				showMove = false;
				// Rename
				if(selected.size() == 1) {
					
					if((megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK)){
						showMove = true;
						showRename = true;
					}
					else if(megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK){
						showMove = false;
						showRename = true;
					}		
				}
				else{
					showRename = false;
					showMove = false;
				}
				
				for(int i=0; i<selected.size();i++)	{
					if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
						showMove = false;
						break;
					}
				}
				
				if(selected.size()==adapter.getItemCount()){
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
			
			menu.findItem(R.id.cab_menu_download).setVisible(true);
			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(true);
			menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
			menu.findItem(R.id.cab_menu_copy).setVisible(true);
			menu.findItem(R.id.cab_menu_move).setVisible(showMove);
			menu.findItem(R.id.cab_menu_share_link).setVisible(false);
			menu.findItem(R.id.cab_menu_trash).setVisible(false);

			return false;
		}

	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
		log("onCreate");
	}
	
	public int getStatusBarHeight() { 
	      int result = 0;
	      int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	      if (resourceId > 0) {
	          result = getResources().getDimensionPixelSize(resourceId);
	      } 
	      return result;
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

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = null;

		if (userEmail != null){
			v = inflater.inflate(R.layout.fragment_contact_file_list, container, false);

			if (aB != null){
				aB.setTitle(R.string.contact_shared_files);
				aB.setLogo(R.drawable.ic_action_navigation_accept_white);
			}
			
			mainLayout = (RelativeLayout) v.findViewById(R.id.contact_file_list);
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mainLayout.getLayoutParams();
			params.setMargins(0, getStatusBarHeight(), 0, 0);
			
			nameView = (TextView) v.findViewById(R.id.contact_file_list_name);
			imageView = (RoundedImageView) v.findViewById(R.id.contact_file_list_thumbnail);
			contactInitialLetter = (TextView) v.findViewById(R.id.contact_file_list_initial_letter);
			textViewContent = (TextView) v.findViewById(R.id.contact_file_list_content);
			contactLayout = (RelativeLayout) v.findViewById(R.id.contact_file_list_contact_layout);
			contactLayout.setOnClickListener(this);
			
			toolbarBack = (ImageView) v.findViewById(R.id.contact_file_list_toolbar_back);
			RelativeLayout.LayoutParams paramsBack = (RelativeLayout.LayoutParams) toolbarBack.getLayoutParams();
			int leftMarginBack = getResources().getDimensionPixelSize(R.dimen.left_margin_back_arrow);
			paramsBack.setMargins(leftMarginBack, 0, 0, 0);
			toolbarBack.setLayoutParams(paramsBack);
			toolbarBack.setOnClickListener(this);

			nameView.setText(userEmail);
			contact = megaApi.getContact(userEmail);
			if(contact == null)
			{
				return null;
			}
			name=false;
			firstName=false;
			megaApi.getUserAttribute(contact, 1, this);
			megaApi.getUserAttribute(contact, 2, this);
			
			createDefaultAvatar();

			File avatar = null;
			if (context.getExternalCacheDir() != null) {
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(),
						contact.getEmail() + ".jpg");
			} else {
				avatar = new File(context.getCacheDir().getAbsolutePath(),
						contact.getEmail() + ".jpg");
			}
			Bitmap imBitmap = null;
			if (avatar.exists()) {
				if (avatar.length() > 0) {
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					imBitmap = BitmapFactory.decodeFile(
							avatar.getAbsolutePath(), bOpts);
					if (imBitmap == null) {
						avatar.delete();
						if (context.getExternalCacheDir() != null) {
							megaApi.getUserAvatar(contact,
									context.getExternalCacheDir().getAbsolutePath()
									+ "/" + contact.getEmail(), this);
						} else {
							megaApi.getUserAvatar(contact,
									context.getCacheDir().getAbsolutePath() + "/"
											+ contact.getEmail(), this);
						}
					} else {
						imageView.setImageBitmap(imBitmap);
						contactInitialLetter.setVisibility(View.GONE);
					}
				}
			}

			contactNodes = megaApi.getInShares(contact);
			textViewContent.setText(getDescription(contactNodes));
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.contact_file_list_view_browser);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new MegaLinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 

			emptyImageView = (ImageView) v.findViewById(R.id.contact_file_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_file_list_empty_text);
			if (contactNodes.size() != 0) {
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				listView.setVisibility(View.VISIBLE);
			} else {
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				listView.setVisibility(View.GONE);
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.file_browser_empty_folder);
			}

			if (adapter == null) {
				adapter = new MegaBrowserLollipopAdapter(context, this, contactNodes, -1,listView, aB,Constants.CONTACT_FILE_ADAPTER, MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST);
				if (mTHash != null){
					adapter.setTransfers(mTHash);
				}
			} else {
				adapter.setNodes(contactNodes);
				adapter.setParentHandle(-1);
			}

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);

			listView.setAdapter(adapter);
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout_contact_file);

			optionsLayout = (LinearLayout) v.findViewById(R.id.contact_file_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.contact_file_list_out_options);
			optionRename = (LinearLayout) v.findViewById(R.id.contact_file_list_option_rename_layout);
			optionRename.setVisibility(View.GONE);
			optionLeaveShare = (LinearLayout) v.findViewById(R.id.contact_file_list_option_leave_share_layout);
			optionLeaveShare.setVisibility(View.GONE);
			
			optionDownload = (LinearLayout) v.findViewById(R.id.contact_file_list_option_download_layout);
			optionProperties = (LinearLayout) v.findViewById(R.id.contact_file_list_option_properties_layout);
			propertiesText = (TextView) v.findViewById(R.id.contact_file_list_option_properties_text);			
			
			optionDelete = (LinearLayout) v.findViewById(R.id.contact_file_list_option_delete_layout);			
			optionRemoveTotal = (LinearLayout) v.findViewById(R.id.contact_file_list_option_remove_layout);
	
			optionCopyTo = (LinearLayout) v.findViewById(R.id.contact_file_list_option_copy_layout);			
			
			optionDownload.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionRename.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			optionRemoveTotal.setOnClickListener(this);			
			optionCopyTo.setOnClickListener(this);
			
			optionsOutLayout.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			log("SliddingPanel invisible!");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			log("SliddingPanel hidden!");
			
			slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
	            @Override
	            public void onPanelSlide(View panel, float slideOffset) {
	            	log("onPanelSlide, offset " + slideOffset);
	            }

	            @Override
	            public void onPanelExpanded(View panel) {
	            	log("onPanelExpanded");

	            }

	            @Override
	            public void onPanelCollapsed(View panel) {
	            	log("onPanelCollapsed");
	            	

	            }

	            @Override
	            public void onPanelAnchored(View panel) {
	            	log("onPanelAnchored");
	            }

	            @Override
	            public void onPanelHidden(View panel) {
	                log("onPanelHidden");                
	            }
	        });			
		}

		return v;
	}
	
	public void showOptionsPanel(MegaNode sNode){
		log("showNodeOptionsPanel");
		
//		fabButton.setVisibility(View.GONE);
		
		this.selectedNode = sNode;	
		
		if (selectedNode.isFolder()) {
			propertiesText.setText(R.string.general_folder_info);
		}else{
			propertiesText.setText(R.string.general_file_info);
		}		
		
		int accessLevel = megaApi.getAccess(selectedNode);
		log("Node: "+selectedNode.getName());
		log("ManagerActivity.CONTACT_FILE_ADAPTER: "+accessLevel);					
		
		switch (accessLevel) {			
			case MegaShare.ACCESS_FULL: {
				
				optionDownload.setVisibility(View.VISIBLE);
				optionProperties.setVisibility(View.VISIBLE);
				
				if(selectedNode.isFile()){
					optionLeaveShare.setVisibility(View.GONE);
				}
				else {
					optionLeaveShare.setVisibility(View.VISIBLE);
				}
				optionRemoveTotal.setVisibility(View.GONE);
				optionRename.setVisibility(View.VISIBLE);
				optionDelete.setVisibility(View.GONE);
	
				break;
			}
			case MegaShare.ACCESS_READ: {
				log("read");
				optionDownload.setVisibility(View.VISIBLE);
				optionProperties.setVisibility(View.VISIBLE);	
				optionRename.setVisibility(View.GONE);
				optionDelete.setVisibility(View.GONE);
				optionRemoveTotal.setVisibility(View.GONE);
				
				if(selectedNode.isFile()){
					optionLeaveShare.setVisibility(View.GONE);
				}
				else {
					optionLeaveShare.setVisibility(View.VISIBLE);
				}						
				break;
			}
			case MegaShare.ACCESS_READWRITE: {
				log("readwrite");
				optionDownload.setVisibility(View.VISIBLE);
				optionProperties.setVisibility(View.VISIBLE);
				//						holder.shareDisabled.setVisibility(View.VISIBLE);
				optionRename.setVisibility(View.VISIBLE);
				optionDelete.setVisibility(View.GONE);
				optionRemoveTotal.setVisibility(View.GONE);
				if(selectedNode.isFile()){
					optionLeaveShare.setVisibility(View.GONE);
				}
				else {
					optionLeaveShare.setVisibility(View.VISIBLE);
				}
				break;
			}
		}
					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
		log("Show the slidingPanel");
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
				
		adapter.setPositionClicked(-1);
//		fabButton.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public PanelState getPanelState ()
	{
		log("getPanelState: "+slidingOptionsPanel.getPanelState());
		return slidingOptionsPanel.getPanelState();
	}
	
	public void createDefaultAvatar(){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
		
		int radius; 
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
        	radius = defaultAvatar.getWidth()/2;
        else
        	radius = defaultAvatar.getHeight()/2;
        
		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		imageView.setImageBitmap(defaultAvatar);
		
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = context.getResources().getDisplayMetrics().density;
	    
	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (userEmail != null){
		    if (userEmail.length() > 0){
		    	String firstLetter = userEmail.charAt(0) + "";
		    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		    	contactInitialLetter.setVisibility(View.VISIBLE);
		    	contactInitialLetter.setText(firstLetter);
		    	contactInitialLetter.setTextSize(32);
		    	contactInitialLetter.setTextColor(Color.WHITE);
		    }
	    }
	}
		
	private int getAvatarTextSize (float density){
		float textSize = 0.0f;
		
		if (density > 3.0){
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		}
		else if (density > 2.0){
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		}
		else if (density > 1.5){
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		}
		else if (density > 1.0){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		}
		else if (density > 0.75){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		}
		else{
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f); 
		}
		
		return (int)textSize;
	}

	public boolean showUpload(){
		if (!parentHandleStack.isEmpty()){
			if ((megaApi.checkAccess(megaApi.getNodeByHandle(parentHandle), MegaShare.ACCESS_FULL).getErrorCode() == MegaError.API_OK) || (megaApi.checkAccess(megaApi.getNodeByHandle(parentHandle), MegaShare.ACCESS_READWRITE).getErrorCode() == MegaError.API_OK)) {
				return true;
			}
		}

		return false;
	}

	public void setNodes(long parentHandle){
		if (megaApi.getNodeByHandle(parentHandle) == null){
			parentHandle = -1;
			this.parentHandle = -1;
			((ContactPropertiesActivityLollipop)context).setParentHandle(parentHandle);
			adapter.setParentHandle(parentHandle);
			if (aB != null){
				aB.setTitle(R.string.contact_shared_files);
				aB.setLogo(R.drawable.ic_action_navigation_accept_white);
			}
			setNodes(megaApi.getInShares(contact));
		}
		else{
			this.parentHandle = parentHandle;
			((ContactPropertiesActivityLollipop)context).setParentHandle(parentHandle);
			adapter.setParentHandle(parentHandle);
			setNodes(megaApi.getChildren(megaApi.getNodeByHandle(parentHandle), orderGetChildren));
		}
	}

	public void setNodes(ArrayList<MegaNode> nodes){
		this.contactNodes = nodes;
		if (adapter != null){
			adapter.setNodes(contactNodes);
			if (adapter.getItemCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				if (megaApi.getRootNode().getHandle()==parentHandle) {
					emptyImageView.setImageResource(R.drawable.ic_empty_cloud_drive);
					emptyTextView.setText(R.string.file_browser_empty_cloud_drive);
				} else {
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				}
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}

	public void setUserEmail(String userEmail){
		this.userEmail = userEmail;
	}

	public String getUserEmail(){
		return this.userEmail;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
		if (aB != null){
			aB.show();
		}
	}

	public String getDescription(ArrayList<MegaNode> nodes) {
		int numFolders = 0;
		int numFiles = 0;

		for (int i = 0; i < nodes.size(); i++) {
			MegaNode c = nodes.get(i);
			if (c.isFolder()) {
				numFolders++;
			} else {
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0) {
			info = numFolders
					+ " "
					+ getResources().getQuantityString(
							R.plurals.general_num_folders, numFolders);
			if (numFiles > 0) {
				info = info
						+ ", "
						+ numFiles
						+ " "
						+ getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		} else {
			if (numFiles == 0) {
				info = numFiles
						+ " "
						+ getResources().getQuantityString(
								R.plurals.general_num_folders, numFolders);
			} else {
				info = numFiles
						+ " "
						+ getResources().getQuantityString(
								R.plurals.general_num_files, numFiles);
			}
		}

		return info;
	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
		if (adapter != null){
			adapter.setParentHandle(parentHandle);
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_MOVE) {
			log("move request start");
		} else if (request.getType() == MegaRequest.TYPE_REMOVE) {
			log("remove request start");
		} else if (request.getType() == MegaRequest.TYPE_EXPORT) {
			log("export request start");
		} else if (request.getType() == MegaRequest.TYPE_RENAME) {
			log("rename request start");
		} else if (request.getType() == MegaRequest.TYPE_COPY) {
			log("copy request start");
		}

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: "+request.getType());
		if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
			if (e.getErrorCode() == MegaError.API_OK) {
				File avatar = null;				
				if (context.getExternalCacheDir() != null) {
					avatar = new File(context.getExternalCacheDir().getAbsolutePath(),
							request.getEmail() + ".jpg");
				} else {
					avatar = new File(context.getCacheDir().getAbsolutePath(),
							request.getEmail() + ".jpg");
				}
				Bitmap imBitmap = null;
				if (avatar.exists()) {
					if (avatar.length() > 0) {
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						imBitmap = BitmapFactory.decodeFile(
								avatar.getAbsolutePath(), bOpts);
						if (imBitmap == null) {
							avatar.delete();
						} else {
							imageView.setImageBitmap(imBitmap);
							contactInitialLetter.setVisibility(View.GONE);
						}
					}
				}
				if(request.getParamType()==1){
					log("(1)request.getText(): "+request.getText());
					nameText=request.getText();
					name=true;
				}
				else if(request.getParamType()==2){
					log("(2)request.getText(): "+request.getText());
					firstNameText = request.getText();
					firstName = true;
				}
				if(name&&firstName){
					nameView.setText(nameText+" "+firstNameText);
					name= false;
					firstName = false;
				}				
			}
		} 
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");
	}

	public static void log(String log) {
		Util.log("ContactFileListFragmentLollipop", log);
	}

	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
	
	public void itemClick(int position) {
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			updateActionModeTitle();
			adapter.notifyDataSetChanged();
//			adapterList.notifyDataSetChanged();
		}
		else{
			if (contactNodes.get(position).isFolder()) {
				MegaNode n = contactNodes.get(position);

				if (aB != null){
					aB.setTitle(n.getName());
					aB.setLogo(R.drawable.ic_action_navigation_previous_item);
				}
				((ContactPropertiesActivityLollipop)context).supportInvalidateOptionsMenu();

				parentHandleStack.push(parentHandle);
				parentHandle = contactNodes.get(position).getHandle();
				adapter.setParentHandle(parentHandle);
				((ContactPropertiesActivityLollipop)context).setParentHandle(parentHandle);

				contactNodes = megaApi.getChildren(contactNodes.get(position));
				adapter.setNodes(contactNodes);
				listView.scrollToPosition(0);
				
				// If folder has no files
				if (adapter.getItemCount() == 0) {
					listView.setVisibility(View.GONE);
					emptyImageView.setVisibility(View.VISIBLE);
					emptyTextView.setVisibility(View.VISIBLE);
					emptyImageView.setImageResource(R.drawable.ic_empty_folder);
					emptyTextView.setText(R.string.file_browser_empty_folder);
				} else {
					listView.setVisibility(View.VISIBLE);
					emptyImageView.setVisibility(View.GONE);
					emptyTextView.setVisibility(View.GONE);
				}
			} 
			else {
				if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isImage()) {
					Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
					intent.putExtra("position", position);
					if (megaApi.getParentNode(contactNodes.get(position)).getType() == MegaNode.TYPE_ROOT) {
						intent.putExtra("parentNodeHandle", -1L);
					} else {
						intent.putExtra("parentNodeHandle", megaApi.getParentNode(contactNodes.get(position)).getHandle());
					}
					((ContactPropertiesActivityLollipop)context).startActivity(intent);
				} 
				else if (MimeTypeList.typeForName(contactNodes.get(position).getName()).isVideo()	|| MimeTypeList.typeForName(contactNodes.get(position).getName()).isAudio()) {
					MegaNode file = contactNodes.get(position);
					Intent service = new Intent(context, MegaStreamingService.class);
					((ContactPropertiesActivityLollipop)context).startService(service);
					String fileName = file.getName();
					try {
						fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
					String mimeType = MimeTypeList.typeForName(file.getName()).getType();
					System.out.println("FILENAME: " + fileName);

					Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
			  			startActivity(mediaIntent);
			  		}
			  		else{
						((ManagerActivityLollipop) context).showSnackbar(context.getResources().getString(R.string.intent_not_available));
			  			adapter.setPositionClicked(-1);
						adapter.notifyDataSetChanged();
						ArrayList<Long> handleList = new ArrayList<Long>();
						handleList.add(contactNodes.get(position).getHandle());
						((ContactPropertiesActivityLollipop)context).onFileClick(handleList);
			  		}
				} else {
					adapter.setPositionClicked(-1);
					adapter.notifyDataSetChanged();
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(contactNodes.get(position).getHandle());
					((ContactPropertiesActivityLollipop)context).onFileClick(handleList);
				}
			}
		}
	}

	public int onBackPressed() {
		
		PanelState pS=slidingOptionsPanel.getPanelState();
		
		if(pS==null){
			log("NULLL");
		}
		else{
			if(pS==PanelState.HIDDEN){
				log("Hidden");
			}
			else if(pS==PanelState.COLLAPSED){
				log("Collapsed");
			}
			else{
				log("ps: "+pS);
			}
		}		
		
		if(slidingOptionsPanel.getPanelState()!=PanelState.HIDDEN){
			log("getPanelState()!=PanelState.HIDDEN");
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
			slidingOptionsPanel.setVisibility(View.GONE);
			setPositionClicked(-1);
			notifyDataSetChanged();
			return 4;
		}
		
		log("Sliding not shown");

		parentHandle = adapter.getParentHandle();
		((ContactPropertiesActivityLollipop)context).setParentHandle(parentHandle);

		if (adapter.getPositionClicked() != -1) {
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		} else {
			if (parentHandleStack.isEmpty()) {
				return 0;
			} else {
				parentHandle = parentHandleStack.pop();
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				if (parentHandle == -1) {
					contactNodes = megaApi.getInShares(contact);
					if (aB != null){
						aB.setTitle(R.string.contact_shared_files);
						aB.setLogo(R.drawable.ic_action_navigation_accept_white);
					}
					((ContactPropertiesActivityLollipop)context).supportInvalidateOptionsMenu();
					adapter.setNodes(contactNodes);
					listView.scrollToPosition(0);
					((ContactPropertiesActivityLollipop)context).setParentHandle(parentHandle);
					adapter.setParentHandle(parentHandle);
					return 2;
				} else {
					contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
					if (aB != null){
						aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
						aB.setLogo(R.drawable.ic_action_navigation_previous_item);
					}
					((ContactPropertiesActivityLollipop)context).supportInvalidateOptionsMenu();
					adapter.setNodes(contactNodes);
					listView.scrollToPosition(0);
					((ContactPropertiesActivityLollipop)context).setParentHandle(parentHandle);
					adapter.setParentHandle(parentHandle);
					return 3;
				}
			}
		}
	}

	public void selectAll(){
		if (adapter != null){
			if(adapter.isMultipleSelect()){
				adapter.selectAll();
			}
			else{
				adapter.setMultipleSelect(true);
				adapter.selectAll();
				
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
			}
			
			updateActionModeTitle();
		}
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		if(adapter.isMultipleSelect()){
			adapter.clearSelections();
		}
//		adapterList.startMultiselection();
//		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//		for (int i = 0; i < checkedItems.size(); i++) {
//			if (checkedItems.valueAt(i) == true) {
//				int checkedPosition = checkedItems.keyAt(i);
//				listView.setItemChecked(checkedPosition, false);
//			}
//		}
		updateActionModeTitle();
	}	

	private void updateActionModeTitle() {
		if (actionMode == null) {
			return;
		}
		List<MegaNode> documents = adapter.getSelectedNodes();
		int files = 0;
		int folders = 0;
		for (MegaNode document : documents) {
			if (document.isFile()) {
				files++;
			} else if (document.isFolder()) {
				folders++;
			}
		}
		Resources res = getResources();
		String format = "%d %s";
		String filesStr = String.format(format, files,
				res.getQuantityString(R.plurals.general_num_files, files));
		String foldersStr = String.format(format, folders,
				res.getQuantityString(R.plurals.general_num_folders, folders));
		String title;
		if (files == 0 && folders == 0) {
			title = foldersStr + ", " + filesStr;
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
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public void setPositionClicked(int positionClicked){
		if (adapter != null){
			adapter.setPositionClicked(positionClicked);
		}	
	}
	
	public void notifyDataSetChanged(){		
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}		
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch (v.getId()) {
		//		case R.id.contact_file_list_contact_layout: {
		//			Intent i = new Intent(this, ContactPropertiesActivityLollipop.class);
		//			i.putExtra("name", contact.getEmail());
		//			startActivity(i);
		//			finish();
		//			break;
		//		}
			case R.id.contact_file_list_toolbar_back:{
				((ContactPropertiesActivityLollipop)context).onBackPressed();
				break;
			}
			case R.id.contact_file_list_option_download_layout: {
				log("Download option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());
				((ContactPropertiesActivityLollipop) context).onFileClick(handleList);
				break;
			}
			case R.id.contact_file_list_option_leave_share_layout: {
				log("Leave share option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ContactPropertiesActivityLollipop) context).leaveIncomingShare(selectedNode);
				break;
			}
			case R.id.contact_file_list_option_move_layout:{
				log("Move option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());									
				((ContactPropertiesActivityLollipop) context).showMoveLollipop(handleList);
	
				break;
			}
			case R.id.contact_file_list_option_properties_layout: {
				log("Properties option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
				i.putExtra("handle", selectedNode.getHandle());
				
				if (selectedNode.isFolder()) {
					if (megaApi.isShared(selectedNode)){
						i.putExtra("imageId", R.drawable.folder_shared_mime);	
					}
					else{
						i.putExtra("imageId", R.drawable.folder_mime);
					}
				} 
				else {
					i.putExtra("imageId", MimeTypeMime.typeForName(selectedNode.getName()).getIconResourceId());
				}
				i.putExtra("name", selectedNode.getName());
				context.startActivity(i);
	
				break;
			}
			case R.id.contact_file_list_option_rename_layout: {
				log("Rename option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ContactPropertiesActivityLollipop) context).showRenameDialog(selectedNode, selectedNode.getName());
				break;
			}	
			case R.id.contact_file_list_option_copy_layout: {
				log("Copy option");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(selectedNode.getHandle());									
				((ContactPropertiesActivityLollipop) context).showCopyLollipop(handleList);
				break;
			}	
		}
	}

	public void setTransfers(HashMap<Long, MegaTransfer> _mTHash){
		this.mTHash = _mTHash;

		if (adapter != null){
			adapter.setTransfers(mTHash);
		}
	}

	public void setCurrentTransfer(MegaTransfer mT){
		if (adapter != null){
			adapter.setCurrentTransfer(mT);
		}
	}

	@Override
	public boolean onDown(MotionEvent e) {
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
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(RecyclerView rV, MotionEvent e) {
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
