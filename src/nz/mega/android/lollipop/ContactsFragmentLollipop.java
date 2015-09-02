package nz.mega.android.lollipop;

import java.util.ArrayList;
import java.util.List;

import nz.mega.android.ContactPropertiesMainActivity;
import nz.mega.android.MegaApplication;
import nz.mega.android.MegaContactsGridAdapter;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.SimpleDividerItemDecoration;
import nz.mega.components.SlidingUpPanelLayout;
import nz.mega.components.SlidingUpPanelLayout.PanelState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
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
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContactsFragmentLollipop extends Fragment implements OnClickListener, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener {

	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	RecyclerView listView;
	GestureDetectorCompat detector;
	MegaContactsLollipopAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	ImageView emptyImageView;
	TextView emptyTextView;
    ImageButton fabButton;
	LinearLayout outSpaceLayout=null;
	TextView outSpaceText;
	Button outSpaceButton;
	int usedSpacePerc;
	private ActionMode actionMode;
	
	boolean isList = true;
	
	ContactsFragmentLollipop contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;

	LayoutManager mLayoutManager;
	MegaUser selectedUser = null;
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionProperties;
	public LinearLayout optionSendFile;
	public LinearLayout optionShare;
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
			if (adapterList.getPositionClicked() == -1){
				adapterList.setMultipleSelect(true);
			
				actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());			

		        itemClick(position);
			}  
	        super.onLongPress(e);
	    }
	}
	
	/////Multiselect/////
	private class ActionBarCallBack implements ActionMode.Callback {
//		
//		boolean selectAll = true;
//		boolean unselectAll = false;
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaUser> users = adapterList.getSelectedUsers();
			final List<MegaUser> multipleContacts = users;;
			
			switch(item.getItemId()){
				case R.id.cab_menu_share_folder:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						((ManagerActivityLollipop) context).pickFolderToShare(users);
					}										
					break;
				}
				case R.id.cab_menu_delete:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
						        switch (which){
						        case DialogInterface.BUTTON_POSITIVE:						        	
						        	((ManagerActivityLollipop) context).removeMultipleContacts(multipleContacts);		        	
						            break;
			
						        case DialogInterface.BUTTON_NEGATIVE:
						            //No button clicked
						            break;
						        }
						    }
						};
			
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setMessage(getResources().getString(R.string.confirmation_remove_multiple_contacts)).setPositiveButton(R.string.general_yes, dialogClickListener)
						    .setNegativeButton(R.string.general_no, dialogClickListener).show();
						
					}	
					//TODO remove contact
					
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					actionMode.invalidate();
					break;
				}				
			}
			return false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.contact_fragment_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			adapterList.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = adapterList.getSelectedUsers();

			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				
				if(selected.size()==adapterList.getItemCount()){
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
			
			menu.findItem(R.id.cab_menu_help).setVisible(false);
			menu.findItem(R.id.cab_menu_upgrade_account).setVisible(false);
			menu.findItem(R.id.cab_menu_settings).setVisible(false);
//			menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);
			return false;
		}		
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
			if (adapterGrid != null){
				adapterGrid.selectAll();
			}
		}
	}
	
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
		List<MegaUser> users = adapterList.getSelectedUsers();
		
		Resources res = getResources();
		String format = "%d %s";
		
		actionMode.setTitle(String.format(format, users.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())+ " selected"));

		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}
		
	//End Multiselect/////
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}		
		
		if (isList){
			View v = inflater.inflate(R.layout.fragment_contactslist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			listView = (RecyclerView) v.findViewById(R.id.contacts_list_view);
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			listView.setHasFixedSize(true);
		    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
		    listView.setLayoutManager(linearLayoutManager);			
			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator()); 
			
			outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_contacts);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text_contacts);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn_contacts);
			outSpaceButton.setOnClickListener(this);
			
			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
			
			if(usedSpacePerc>95){
				//Change below of RecyclerView
				log("usedSpacePerc>95");
//				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//				p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//				listView.setLayoutParams(p);
				outSpaceLayout.setVisibility(View.VISIBLE);
				outSpaceLayout.bringToFront();
				
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {					
					
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
			}
			
			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_list_empty_text);
			
			fabButton = (ImageButton) v.findViewById(R.id.invite_contact_button);
			fabButton.setOnClickListener(this);
			
			if (adapterList == null){
				adapterList = new MegaContactsLollipopAdapter(context, this, visibleContacts, emptyImageView, emptyTextView, listView);
			}
			else{
				adapterList.setContacts(visibleContacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){				
		
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			optionsLayout = (LinearLayout) v.findViewById(R.id.contact_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.contact_list_out_options);
			optionProperties = (LinearLayout) v.findViewById(R.id.contact_list_option_properties_layout);
			optionShare = (LinearLayout) v.findViewById(R.id.contact_list_option_share_layout);
			optionSendFile = (LinearLayout) v.findViewById(R.id.contact_list_option_send_file_layout);
			optionRemove = (LinearLayout) v.findViewById(R.id.contact_list_option_remove_layout);		
			
			optionRemove.setOnClickListener(this);
			optionShare.setOnClickListener(this);
			optionProperties.setOnClickListener(this);
			optionSendFile.setOnClickListener(this);
			
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
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			/*
			Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
			DisplayMetrics outMetrics = new DisplayMetrics ();
		    display.getMetrics(outMetrics);
		    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
			
		    float scaleW = Util.getScaleW(outMetrics, density);
		    float scaleH = Util.getScaleH(outMetrics, density);
		    
		    int totalWidth = outMetrics.widthPixels;
		    int totalHeight = outMetrics.heightPixels;
		    
		    int numberOfCells = totalWidth / GRID_WIDTH;
		    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
		    	if (numberOfCells < 3){
					numberOfCells = 3;
				}	
		    }
		    else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
		    	if (numberOfCells < 2){
					numberOfCells = 2;
				}	
		    }
			
			listView = (RecyclerView) v.findViewById(R.id.contact_grid_view_browser);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);
	        
	        outSpaceLayout = (LinearLayout) v.findViewById(R.id.out_space_grid_contacts);
			outSpaceText =  (TextView) v.findViewById(R.id.out_space_text_grid_contacts);
			outSpaceButton = (Button) v.findViewById(R.id.out_space_btn_grid_contacts);
			outSpaceButton.setOnClickListener(this);
			
			usedSpacePerc=((ManagerActivityLollipop)context).getUsedPerc();
			
			if(usedSpacePerc>95){
				//Change below of RecyclerView
				log("usedSpacePerc>95");
//				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
//				p.addRule(RelativeLayout.ABOVE, R.id.out_space);
//				listView.setLayoutParams(p);
				outSpaceLayout.setVisibility(View.VISIBLE);
				outSpaceLayout.bringToFront();
				
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {					
					
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
			}
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.contact_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_grid_empty_text);
	        
	        if (adapterGrid == null){
	        	adapterGrid = new MegaContactsGridAdapter(context, visibleContacts, listView, numberOfCells);
	        }
	        else{
	        	adapterGrid.setContacts(visibleContacts);
	        }
	        
	        adapterGrid.setPositionClicked(-1);   
			listView.setAdapter(adapterGrid);
			addContactButton = (Button) v.findViewById(R.id.add_contact_button);
			addContactButton.setOnClickListener(this);			
			
			if (adapterGrid.getCount() == 0){
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				addContactButton.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
				addContactButton.setVisibility(View.GONE);
			}		*/	
			
			return v;
		}			
	}
	
	public void showOptionsPanel(MegaUser user){		
		log("showOptionsPanel");	
		
		this.selectedUser = user;
		fabButton.setVisibility(View.GONE);					
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
				
		adapterList.setPositionClicked(-1);
		fabButton.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
	
	public void setContacts(ArrayList<MegaUser> contacts){
		this.contacts = contacts;
		
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		if (isList){
			adapterList.setContacts(visibleContacts);
		}
		else{
			adapterGrid.setContacts(visibleContacts);
		}
		
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
			case R.id.invite_contact_button:{				
				((ManagerActivityLollipop)context).showNewContactDialog(null);				
				break;
			}
			case R.id.out_space_btn_contacts:
			case R.id.out_space_btn_grid_contacts:{
				((ManagerActivityLollipop)getActivity()).upgradeAccountButton();
				break;
			}
			case R.id.contact_list_option_send_file_layout:{
				log("optionSendFile");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				List<MegaUser> user = new ArrayList<MegaUser>();
				user.add(selectedUser);
				((ManagerActivityLollipop) context).pickContacToSendFile(user);
				break;
			}
			case R.id.contact_list_option_properties_layout:{
				log("optionProperties");
//				Intent i = new Intent(context, ContactPropertiesMainActivity.class);
				log("optionSendFile");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
				i.putExtra("name", selectedUser.getEmail());
				context.startActivity(i);			
				break;
			}
			case R.id.contact_list_option_share_layout:{
				log("optionShare");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				List<MegaUser> user = new ArrayList<MegaUser>();
				user.add(selectedUser);
				((ManagerActivityLollipop) context).pickFolderToShare(user);
				break;
			}
			case R.id.contact_list_option_remove_layout:{
				log("Remove contact");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).removeContact(selectedUser);
				break;
			}
		}
	}
			
    public void itemClick(int position) {
		
		if (isList){
			
			if (adapterList.isMultipleSelect()){
				adapterList.toggleSelection(position);
				updateActionModeTitle();
				adapterList.notifyDataSetChanged();
			}
			else{
		
				Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
				i.putExtra("name", visibleContacts.get(position).getEmail());
				startActivity(i);
			}
		}
    }
	
	public int onBackPressed(){
		
		log("onBackPressed");	
		
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
		
		if (isList){
			if (adapterList.isMultipleSelect()){
				hideMultipleSelect();
				return 2;
			}
			
			if (adapterList.getPositionClicked() != -1){
				adapterList.setPositionClicked(-1);
				adapterList.notifyDataSetChanged();
				return 1;
			}
			else{
				return 0;
			}
		}
		else{
//			if (adapterGrid.isMultipleSelect()){
//				adapterGrid.hideMultipleSelect();
//				return 2;
//			}
//			
//			if (adapterGrid.getPositionClicked() != -1){
//				adapterGrid.setPositionClicked(-1);
//				adapterGrid.notifyDataSetChanged();
//				return 1;
//			}
//			else{
//				return 0;
//			}
			return 0;
		}
	}
	
	public void setIsList(boolean isList){
		this.isList = isList;
	}
	
	public boolean getIsList(){
		return isList;
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
			if (adapterGrid != null){
				adapterGrid.setPositionClicked(positionClicked);
			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
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
	
	public RecyclerView getListView(){
		return listView;
	}
	
	private static void log(String log) {
		Util.log("ContactsFragmentLollipop", log);
	}

	public void updateView () {
		log("updateView");
		ArrayList<MegaUser> contacts = megaApi.getContacts();
		this.setContacts(contacts);
		
		if (visibleContacts.size() == 0){
			log("CONTACTS SIZE == 0");
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
			emptyTextView.setText(R.string.contacts_list_empty_text);
		}
		else{
			log("CONTACTS SIZE != 0");
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}	
		
	}
	
	public void sortByNameDescending(){
		for(int i = 0, j = visibleContacts.size() - 1; i < j; i++) {
			visibleContacts.add(i, visibleContacts.remove(j));
		}
		
		if (isList){
			adapterList.setContacts(visibleContacts);
		}
		else{
			adapterGrid.setContacts(visibleContacts);
		}
	}
	public void sortByNameAscending(){
		updateView();
	}
	
	public boolean showSelectMenuItem(){
		if (isList){
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
	
	public void setOrder(int orderContacts){
		this.orderContacts = orderContacts;
	}

	public ArrayList<MegaUser> getVisibleContacts() {
		return visibleContacts;
	}

	public void setVisibleContacts(ArrayList<MegaUser> visibleContacts) {
		this.visibleContacts = visibleContacts;
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
