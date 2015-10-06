package nz.mega.android.lollipop;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import nz.mega.android.MegaApplication;
import nz.mega.android.MegaPreferences;
import nz.mega.android.R;
import nz.mega.android.ShareInfo;
import nz.mega.android.UploadHereDialog;
import nz.mega.android.UploadService;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FileContactListActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, OnClickListener, MegaGlobalListenerInterface {

	MegaApiAndroid megaApi;
	ActionBar aB;
	Toolbar tB;
	FileContactListActivityLollipop fileContactListActivityLollipop = this;
	MegaShare selectedNode;
	
	TextView nameView;
	ImageView imageView;
	ImageView statusDot;
	TextView createdView;
	
	RelativeLayout contactLayout;
	RelativeLayout fileContactLayout;
	RecyclerView listView;
	RecyclerView.LayoutManager mLayoutManager;
	GestureDetectorCompat detector;
	ImageView emptyImage;
	TextView emptyText;
	
	ArrayList<MegaShare> listContacts;	
	ArrayList<MegaShare> tempListContacts;	
	
//	ArrayList<MegaUser> listContactsArray = new ArrayList<MegaUser>();
	
	long nodeHandle;
	MegaNode node;
	ArrayList<MegaNode> contactNodes;
	
	MegaSharedFolderLollipopAdapter adapter;
	
	long parentHandle = -1;
	
	Stack<Long> parentHandleStack = new Stack<Long>();
	
	private ActionMode actionMode;
	
	boolean removeShare = false;
	boolean changeShare = false;
	
	ProgressDialog statusDialog;
	AlertDialog permissionsDialog;
	
	public static int REQUEST_CODE_SELECT_CONTACT = 1000;
	
	private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	public UploadHereDialog uploadDialog;
	
	private List<ShareInfo> filePreparedInfos;
	
	MegaPreferences prefs = null;
	
	MenuItem permissionButton;
	MenuItem deleteShareButton;
	MenuItem addSharingContact;
	MenuItem selectMenuItem;
	MenuItem unSelectMenuItem;
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionPermissions;
	public LinearLayout optionRemove;
	
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
			
				actionMode = startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
	    }
	}
	
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			log("onActionItemClicked");
			final List<MegaShare> contacts = adapter.getSelectedShares();		
						
			switch(item.getItemId()){
				case R.id.action_file_contact_list_permissions:{
					
					//Change permissions
	
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(fileContactListActivityLollipop);
	
					dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
					
					
					final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
					dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							removeShare = false;
							changeShare = true;
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(fileContactListActivityLollipop);
								temp.setMessage(getString(R.string.context_sharing_folder));
								temp.show();
							}
							catch(Exception e){
								return;
							}
							statusDialog = temp;
							switch(item) {
								case 0:{
									if(contacts!=null){
		
										if(contacts.size()!=0){
											log("Tamaño array----- "+contacts.size());	
											for(int j=0;j<contacts.size();j++){
												log("Numero: "+j);	
												if(contacts.get(j).getUser()!=null){
													MegaUser u = megaApi.getContact(contacts.get(j).getUser());													
													megaApi.share(node, u, MegaShare.ACCESS_READ, fileContactListActivityLollipop);	
												}
											}
										}
									}		                        	
									break;
								}
								case 1:{
									if(contacts!=null){
										if(contacts.size()!=0){
											log("Tamaño array----- "+contacts.size());		
											for(int j=0;j<contacts.size();j++){										
												log("Numero: "+j);
												if(contacts.get(j).getUser()!=null){
													MegaUser u = megaApi.getContact(contacts.get(j).getUser());
													megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);		
												}
											}
										}
									}	
		
									break;
								}
								case 2:{
									if(contacts!=null){
		
										if(contacts.size()!=0){
											log("Tamaño array----- "+contacts.size());		
											for(int j=0;j<contacts.size();j++){										
												log("Numero: "+j);	
												if(contacts.get(j).getUser()!=null){
													MegaUser u = megaApi.getContact(contacts.get(j).getUser());
													megaApi.share(node, u, MegaShare.ACCESS_FULL, fileContactListActivityLollipop);								
												}
											}
										}
									}
									break;
								}
							}
						}
					});
					
					permissionsDialog = dialogBuilder.create();
					permissionsDialog.show();
					
					adapter.setMultipleSelect(false);
	
					log("Cambio permisos");
					
					adapter.clearSelections();
					hideMultipleSelect();
					
					break;
				}
				case R.id.action_file_contact_list_delete:{
					
	
					removeShare = true;
					changeShare = false;
					ProgressDialog temp = null;
	
					try{
						temp = new ProgressDialog(fileContactListActivityLollipop);					
	
						temp.setMessage((getString(R.string.context_sharing_folder))); 
						temp.show();
					}
					catch(Exception e){
						return false;
					}
	
					statusDialog = temp;
	
					if(contacts!=null){

						if(contacts.size()!=0){

							for(int j=0;j<contacts.size();j++){	
								if(contacts.get(j).getUser()!=null){
									MegaUser u = megaApi.getContact(contacts.get(j).getUser());
									megaApi.share(node, u, MegaShare.ACCESS_UNKNOWN, fileContactListActivityLollipop);	
								}
								else{
									megaApi.disableExport(node, fileContactListActivityLollipop);
								}
							}

						}
					}				
					adapter.setMultipleSelect(false);
					adapter.clearSelections();
					hideMultipleSelect();
					break;
				}
				case R.id.cab_menu_select_all:{
					adapter.selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					adapter.clearSelections();
					actionMode.invalidate();
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			log("onCreateActionMode");
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.file_contact_shared_browser_action, menu);
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			adapter.setMultipleSelect(false);
			adapter.clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			log("onPrepareActionMode");
			List<MegaShare> selected = adapter.getSelectedShares();
			boolean deleteShare = false;
			boolean permissions = false;
						
			if (selected.size() != 0) {
				permissions = true;
				deleteShare = true;
				
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
			
			menu.findItem(R.id.action_file_contact_list_permissions).setVisible(permissions);
			menu.findItem(R.id.action_file_contact_list_delete).setVisible(deleteShare);
			
			return false;
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);
		
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}		
		
		megaApi.addGlobalListener(this);
		
		listContacts = new ArrayList<MegaShare>();
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);	    
	    
	    Bundle extras = getIntent().getExtras();
		if (extras != null){
			nodeHandle = extras.getLong("name");
			node=megaApi.getNodeByHandle(nodeHandle);				
			
			setContentView(R.layout.activity_file_contact_list);
			
			//Set toolbar
			tB = (Toolbar) findViewById(R.id.toolbar_file_contact_list);
			setSupportActionBar(tB);
			aB = getSupportActionBar();
//			aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
			aB.setDisplayHomeAsUpEnabled(true);
			aB.setDisplayShowHomeEnabled(true);
			aB.setTitle(getString(R.string.file_properties_shared_folder_list_shares));
			
			imageView = (ImageView) findViewById(R.id.file_properties_icon);
			nameView = (TextView) findViewById(R.id.node_name);
			createdView = (TextView) findViewById(R.id.node_last_update);
			contactLayout = (RelativeLayout) findViewById(R.id.file_contact_list_layout);
			contactLayout.setOnClickListener(this);
			fileContactLayout = (RelativeLayout) findViewById(R.id.file_contact_list_browser_layout);
						
			nameView.setText(node.getName());		
			
			imageView.setImageResource(R.drawable.ic_folder_shared_list);	
			
			tempListContacts = megaApi.getOutShares(node);		
			for(int i=0; i<tempListContacts.size();i++){
				if(tempListContacts.get(i).getUser()!=null){
					listContacts.add(tempListContacts.get(i));
				}
			}
			
			detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) findViewById(R.id.file_contact_list_view_browser);
			listView.addItemDecoration(new SimpleDividerItemDecoration(this));
			mLayoutManager = new LinearLayoutManager(this);
			listView.setLayoutManager(mLayoutManager);
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 			
			
			emptyImage = (ImageView) findViewById(R.id.file_contact_list_empty_image);
			emptyText = (TextView) findViewById(R.id.file_contact_list_empty_text);
			if (listContacts.size() != 0){
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				fileContactLayout.setVisibility(View.VISIBLE);
				listView.setVisibility(View.VISIBLE);
			}			
			else{
				fileContactLayout.setVisibility(View.VISIBLE);
				emptyImage.setVisibility(View.VISIBLE);
				emptyText.setVisibility(View.VISIBLE);
				
				listView.setVisibility(View.GONE);
				emptyImage.setImageResource(R.drawable.ic_empty_folder);
				emptyText.setText(R.string.file_browser_empty_folder);
			}
			
			if (node.getCreationTime() != 0){
				try {createdView.setText(DateUtils.getRelativeTimeSpanString(node.getCreationTime() * 1000));
				}catch(Exception ex){
					createdView.setText("");
				}				
				
			}
			else{
				createdView.setText("");				
			}
						
			if (adapter == null){
				
				adapter = new MegaSharedFolderLollipopAdapter(this, node, listContacts, listView);
				listView.setAdapter(adapter);
				adapter.setShareList(listContacts);		
			}
			else{
				adapter.setShareList(listContacts);
				//adapter.setParentHandle(-1);
			}
						
			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			
			listView.setAdapter(adapter);
			
			slidingOptionsPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout_file_contact_list);
			optionsLayout = (LinearLayout) findViewById(R.id.file_contact_list_options);
			optionsOutLayout = (FrameLayout) findViewById(R.id.file_contact_list_out_options);
			optionPermissions = (LinearLayout) findViewById(R.id.file_contact_list_option_share_layout);					
			optionRemove = (LinearLayout) findViewById(R.id.file_contact_list_option_remove_layout);
			
			optionRemove.setOnClickListener(this);			
			optionPermissions.setOnClickListener(this);
			
			optionsOutLayout.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			
			slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
	            @Override
	            public void onPanelSlide(View panel, float slideOffset) {
	            	log("onPanelSlide, offset " + slideOffset);
	            	if(slideOffset==0){
	            		hideOptionsPanel();
	            	}
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
	}
	
	
	public void showOptionsPanel(MegaShare sShare){
		log("showOptionsPanel");
		
//		fabButton.setVisibility(View.GONE);
		
		this.selectedNode = sShare;	
		
		if (selectedNode.getUser() != null){
			optionPermissions.setVisibility(View.VISIBLE);
			optionPermissions.setOnClickListener(this);
		}
		else{
			optionPermissions.setVisibility(View.GONE);			
		}		
					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
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
	
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(megaApi != null)
    	{
    		megaApi.removeGlobalListener(this);
    		megaApi.removeRequestListener(this);
    	}
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_folder_contact_list, menu);
	    
//	    permissionButton = menu.findItem(R.id.action_file_contact_list_permissions);
//	    deleteShareButton = menu.findItem(R.id.action_file_contact_list_delete);
	    addSharingContact = menu.findItem(R.id.action_folder_contacts_list_share_folder);
	    	    
//	    permissionButton.setVisible(false);
//	    deleteShareButton.setVisible(false);
	    addSharingContact.setVisible(true);
	    
	    selectMenuItem = menu.findItem(R.id.action_select);
		unSelectMenuItem = menu.findItem(R.id.action_unselect);
		
		selectMenuItem.setVisible(true);
		unSelectMenuItem.setVisible(false);
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		// Handle presses on the action bar items
	    switch (item.getItemId()) {
		    case android.R.id.home:{
		    	onBackPressed();
		    	return true;
		    }
		    case R.id.action_folder_contacts_list_share_folder:{
		    	//Option add new contact to share
		    	
		    	Intent intent = new Intent(ContactsExplorerActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
		    	intent.setClass(this, ContactsExplorerActivityLollipop.class);
		    	intent.putExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
		    	startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
		    	
	        	return true;
	        }
		    case R.id.action_select:{
		    	
		    	selectAll();
		    	return true;
		    }
		    default:{
	            return super.onOptionsItemSelected(item);
	        }
	    }
	}
	
	public void selectAll(){

		if(adapter.isMultipleSelect()){
			adapter.selectAll();
		}
		else{						
			adapter.setMultipleSelect(true);
			actionMode = startSupportActionMode(new ActionBarCallBack());
			adapter.selectAll();
		}		
		updateActionModeTitle();		
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		if (request.getType() == MegaRequest.TYPE_SHARE) {
			log("onRequestStart - Share");
		}
	}	

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
		log("onRequestFinish: " + request.getType());
		log("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_SHARE){		
			log(" MegaRequest.TYPE_SHARE");
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {
				log("Error dismiss status dialog");
			}

			if (e.getErrorCode() == MegaError.API_OK){
				if(removeShare){
					log("OK onRequestFinish remove");
					Toast.makeText(this, getString(R.string.context_contact_removed), Toast.LENGTH_SHORT).show();
					removeShare=false;
					adapter.setShareList(listContacts);
					listView.invalidate();

				}
				if(changeShare){
					log("OK onRequestFinish change");
					permissionsDialog.dismiss();
					Toast.makeText(this, getString(R.string.context_permissions_changed), Toast.LENGTH_SHORT).show();
					changeShare=false;
					adapter.setShareList(listContacts);
					listView.invalidate();
				
				}				
			}
			else{
				if(removeShare){
					log("ERROR onRequestFinish remove");
					Toast.makeText(this, getString(R.string.context_contact_not_removed), Toast.LENGTH_SHORT).show();
					removeShare=false;	
				}
				if(changeShare){
					log("ERROR onRequestFinish change");
					Toast.makeText(this, getString(R.string.context_permissions_not_changed), Toast.LENGTH_SHORT).show();
				}
			}
			log("Finish onRequestFinish");
		}
		else if (request.getType() == MegaRequest.TYPE_EXPORT){
			
			if (e.getErrorCode() == MegaError.API_OK){
				try { 
					statusDialog.dismiss();	
					adapter.setShareList(listContacts);
					listView.invalidate();
				} 
				catch (Exception ex) {
					log("Error dismiss status dialog");
				}
				Toast.makeText(this, getString(R.string.context_node_private), Toast.LENGTH_LONG).show();
			}
			else{
				Util.showErrorAlertDialog(e, this);
			}
		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError");
	}
	
	public static void log(String log) {
		Util.log("FileContactListActivityLollipop", log);
	}

	public void itemClick(int position) {
		
		log("itemClick");
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			updateActionModeTitle();
			adapter.notifyDataSetChanged();
//			adapterList.notifyDataSetChanged();
		}
	}

	@Override
	public void onBackPressed() {
					
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
		}
		else{
			if (parentHandleStack.isEmpty()){
				super.onBackPressed();
			}
			else{
				parentHandle = parentHandleStack.pop();
				listView.setVisibility(View.VISIBLE);
				emptyImage.setVisibility(View.GONE);
				emptyText.setVisibility(View.GONE);
				if (parentHandle == -1){
					aB.setTitle(getString(R.string.file_properties_shared_folder_list_shares));
					aB.setLogo(R.drawable.ic_action_navigation_accept_white);
					supportInvalidateOptionsMenu();
					adapter.setShareList(listContacts);
					listView.scrollToPosition(0);
				}
				else{
					contactNodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
					aB.setTitle(megaApi.getNodeByHandle(parentHandle).getName());
					aB.setLogo(R.drawable.ic_action_navigation_previous_item);
					supportInvalidateOptionsMenu();
					adapter.setShareList(listContacts);
					listView.scrollToPosition(0);
				}
			}
		}
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null) {
			return;
		}
		List<MegaShare> contacts = adapter.getSelectedShares();
		
		Resources res = getResources();
		String format = "%d %s";
	
		actionMode.setTitle(String.format(format, contacts.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())));
		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}		
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()){		
			case R.id.file_contact_list_layout:{
				Intent i = new Intent(this, ManagerActivityLollipop.class);
				i.setAction(ManagerActivityLollipop.ACTION_REFRESH_PARENTHANDLE_BROWSER);
				i.putExtra("parentHandle", node.getHandle());
				startActivity(i);
				finish();
				break;
			}
			case R.id.file_contact_list_option_share_layout:{
				log("En el adapter - change");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
				dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, selectedNode.getAccess(), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						removeShare = false;
						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(fileContactListActivityLollipop);
							temp.setMessage(getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						permissionsDialog.dismiss();
						
						switch(item) {
	                        case 0:{
	                        	MegaUser u = megaApi.getContact(selectedNode.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_READ, fileContactListActivityLollipop);
	                        	break;
	                        }
	                        case 1:{
	                        	MegaUser u = megaApi.getContact(selectedNode.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);
                                break;
	                        }
	                        case 2:{
	                        	MegaUser u = megaApi.getContact(selectedNode.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_FULL, fileContactListActivityLollipop);
                                break;
	                        }
	                    }
					}
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
//				Resources resources = permissionsDialog.getContext().getResources();
//				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
//				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
//		        alertTitle.setTextColor(resources.getColor(R.color.mega));
//				int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
//				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
//				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));				
				setPositionClicked(-1);
//				((FileContactListActivityLollipop)context).refreshView();
				break;
			}
			case R.id.file_contact_list_option_remove_layout:{
				log("En el adapter - remove");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				MegaUser c = null;
				if (selectedNode.getUser() != null){
					c = megaApi.getContact(selectedNode.getUser());
				}
				removeShare(c);
				setPositionClicked(-1);
//				((FileContactListActivityLollipop)context).refreshView();
				break;
			}
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

	/*
	 * Handle processed upload intent
	 */
	public void onIntentProcessed() {
		List<ShareInfo> infos = filePreparedInfos;
		if (statusDialog != null) {
			try { 
				statusDialog.dismiss(); 
			} 
			catch(Exception ex){}
		}
		
		MegaNode parentNode = megaApi.getNodeByHandle(parentHandle); 
		if(parentNode == null){
			Util.showErrorAlertDialog(getString(R.string.error_temporary_unavaible), false, this);
			return;
		}
			
		if (infos == null) {
			Util.showErrorAlertDialog(getString(R.string.upload_can_not_open),
					false, this);
		} 
		else {
			Toast.makeText(getApplicationContext(), getString(R.string.upload_began),
					Toast.LENGTH_SHORT).show();
			for (ShareInfo info : infos) {
				Intent intent = new Intent(this, UploadService.class);
				intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
				intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
				intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
				intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
				startService(intent);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_CONTACT && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
				return;
			}
			
			final ArrayList<String> emails = intent.getStringArrayListExtra(ContactsExplorerActivityLollipop.EXTRA_CONTACTS);
			final long nodeHandle = intent.getLongExtra(ContactsExplorerActivityLollipop.EXTRA_NODE_HANDLE, -1);
			
			if(nodeHandle!=-1){
				node=megaApi.getNodeByHandle(nodeHandle);
			}			
			if(node!=null)
			{
				if (node.isFolder()){
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
					dialogBuilder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
					final CharSequence[] items = {getString(R.string.file_properties_shared_folder_read_only), getString(R.string.file_properties_shared_folder_read_write), getString(R.string.file_properties_shared_folder_full_access)};
					dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							ProgressDialog temp = null;
							try{
								temp = new ProgressDialog(fileContactListActivityLollipop);
								temp.setMessage(getString(R.string.context_sharing_folder));
								temp.show();
							}
							catch(Exception e){
								return;
							}
							statusDialog = temp;
							permissionsDialog.dismiss();
							
							switch(item) {
			                    case 0:{
			                    	for (int i=0;i<emails.size();i++){
			                    		MegaUser u = megaApi.getContact(emails.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_READ, fileContactListActivityLollipop);
			                    	}
			                    	break;
			                    }
			                    case 1:{
			                    	for (int i=0;i<emails.size();i++){
			                    		MegaUser u = megaApi.getContact(emails.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_READWRITE, fileContactListActivityLollipop);
			                    	}
			                        break;
			                    }
			                    case 2:{
			                    	for (int i=0;i<emails.size();i++){
			                    		MegaUser u = megaApi.getContact(emails.get(i));
			                    		megaApi.share(node, u, MegaShare.ACCESS_FULL, fileContactListActivityLollipop);
			                    	}		                    	
			                        break;
			                    }
			                }
						}
					});
					permissionsDialog = dialogBuilder.create();
					permissionsDialog.show();
				}
				else{ 
					for (int i=0;i<emails.size();i++){
						MegaUser u = megaApi.getContact(emails.get(i));
						megaApi.sendFileToUser(node, u, fileContactListActivityLollipop);
					}
				}
			}
		}			
	}
	
	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUserupdate");

	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodes) {

		log("onNodesUpdate");

		if (node.isFolder()){
			listContacts.clear();
			
			tempListContacts = megaApi.getOutShares(node);		
			for(int i=0; i<tempListContacts.size();i++){
				if(tempListContacts.get(i).getUser()!=null){
					listContacts.add(tempListContacts.get(i));
				}
			}

//			listContacts = megaApi.getOutShares(node);
			if (listContacts != null){
				if (listContacts.size() > 0){
					fileContactLayout.setVisibility(View.VISIBLE);

					if (adapter != null){
						adapter.setNode(node);
						adapter.setContext(this);
						adapter.setShareList(listContacts);
						adapter.setListFragment(listView);
					}
					else{
						adapter = new MegaSharedFolderLollipopAdapter(this, node, listContacts, listView);
					}

				}
				else{
					fileContactLayout.setVisibility(View.GONE);
					//((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.file_properties_image);
				}
			}			
		}

		listView.invalidate();
	}
	
	public void removeShare (MegaUser c)
	{
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_sharing_folder)); 
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		if (c != null){
			removeShare = true;			
			megaApi.share(node, c, MegaShare.ACCESS_UNKNOWN, this);
		}
		else{
			megaApi.disableExport(node, this);
		}
		
	}
//	public void refreshView (){
//		log("refreshView");
//		
//		if (node.isFolder()){
//
//			listContacts = megaApi.getOutShares(node);
//			if (listContacts != null){
//				if (listContacts.size() > 0){
//					fileContactLayout.setVisibility(View.VISIBLE);
//
//					if (adapter != null){
//						adapter.setNode(node);
//						adapter.setContext(this);
//						adapter.setShareList(listContacts);
//						adapter.setListViewActivity(listView);
//					}
//					else{
//						adapter = new MegaSharedFolderAdapter(this, node, listContacts, listView);
//					}
//
//				}
//				else{
//					fileContactLayout.setVisibility(View.GONE);
//					//((RelativeLayout.LayoutParams)infoTable.getLayoutParams()).addRule(RelativeLayout.BELOW, R.id.file_properties_image);
//				}
//			}			
//		}
//
//		listView.invalidateViews();
//		
//	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api,
			ArrayList<MegaContactRequest> requests) {
		// TODO Auto-generated method stub
		
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

