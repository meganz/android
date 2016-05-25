package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

public class ContactsFragmentLollipop extends Fragment implements RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener {

	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	GestureDetectorCompat detector;
	MegaContactsLollipopAdapter adapter;
	ImageView emptyImageView;
	TextView emptyTextView;
	private ActionMode actionMode;

//	DatabaseHandler dbH = null;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	boolean isList = true;
	
	ContactsFragmentLollipop contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	int orderContacts;

	LayoutManager mLayoutManager;
	MegaUser selectedUser = null;
	
	public class RecyclerViewOnGestureListener extends SimpleOnGestureListener{

	    public void onLongPress(MotionEvent e) {
	        View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
	        int position = recyclerView.getChildPosition(view);

	        // handle long press
			if (!adapter.isMultipleSelect()){
				adapter.setMultipleSelect(true);
			
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
			ArrayList<MegaUser> users = adapter.getSelectedUsers();

			switch(item.getItemId()){
				case R.id.cab_menu_share_folder:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						ContactController cC = new ContactController(context);
						cC.pickFolderToShare(users);
					}										
					break;
				}
				case R.id.cab_menu_send_file:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						ContactController cC = new ContactController(context);
						cC.pickFileToSend(users);
					}										
					break;
				}
				case R.id.cab_menu_delete:{
					clearSelections();
					hideMultipleSelect();
					((ManagerActivityLollipop)context).showConfirmationRemoveContacts(users);

					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
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
			adapter.setMultipleSelect(false);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = adapter.getSelectedUsers();

			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				
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
		adapter.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
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
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaUser> users = adapter.getSelectedUsers();
		
		Resources res = getResources();
		String format = "%d %s";
		
		actionMode.setTitle(String.format(format, users.size(),res.getQuantityString(R.plurals.general_num_contacts, users.size())));

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

//		dbH = DatabaseHandler.getDbHandler(context);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		log("onCreateView");
		contacts = megaApi.getContacts();
		visibleContacts.clear();
		
//		for (int i=0;i<contacts.size();i++){
//
//			MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()));
//			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility()+"__"+contactDB.getName()+" "+contactDB.getLastName());
//			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
//				visibleContacts.add(contacts.get(i));
//			}
//		}

		for (int i=0;i<contacts.size();i++){

//			MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()));
//			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility()+"__"+contactDB.getName()+" "+contactDB.getLastName());
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
	    
	    isList = ((ManagerActivityLollipop)context).isList();
		orderContacts = ((ManagerActivityLollipop)context).getOrderContacts();
		
		if (isList){
			log("isList");
			View v = inflater.inflate(R.layout.fragment_contactslist, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_list_view);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
			recyclerView.setHasFixedSize(true);
		    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
		    recyclerView.setLayoutManager(linearLayoutManager);			
		    recyclerView.addOnItemTouchListener(this);
		    recyclerView.setItemAnimator(new DefaultItemAnimator()); 			
		
			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_list_empty_text);

			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, emptyImageView, emptyTextView, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
		
			adapter.setPositionClicked(-1);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){				
		
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
			detector = new GestureDetectorCompat(getActivity(), new RecyclerViewOnGestureListener());
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_grid_view);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.setHasFixedSize(true);
			final GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
			gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
				@Override
			      public int getSpanSize(int position) {
					return 1;
				}
			});
			
			recyclerView.addOnItemTouchListener(this);
			recyclerView.setItemAnimator(new DefaultItemAnimator());			
				
			emptyImageView = (ImageView) v.findViewById(R.id.contact_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_grid_empty_text);

			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, emptyImageView, emptyTextView, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
		
			adapter.setPositionClicked(-1);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){				
		
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				recyclerView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
			return v;
		}			
	}
	
	public void setContacts(ArrayList<MegaUser> contacts){
		this.contacts = contacts;
		
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		adapter.setContacts(visibleContacts);		
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    public void itemClick(int position) {
					
		if (adapter.isMultipleSelect()){
			adapter.toggleSelection(position);
			List<MegaUser> users = adapter.getSelectedUsers();
			if (users.size() > 0){
				updateActionModeTitle();
				adapter.notifyDataSetChanged();
			}
			else{
				hideMultipleSelect();
			}
		}
		else{
	
			Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
			i.putExtra("name", visibleContacts.get(position).getEmail());
			startActivity(i);
		}
    }
	
	public int onBackPressed(){
		log("onBackPressed");	

		if (adapter.isMultipleSelect()){
			hideMultipleSelect();
			return 2;
		}
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
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
		if (adapter != null){
			adapter.setPositionClicked(positionClicked);
		}
	}
	
	public void notifyDataSetChanged(){
		if (adapter != null){
			adapter.notifyDataSetChanged();
		}
	}
	
	public RecyclerView getRecyclerView(){
		return recyclerView;
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
			recyclerView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
			emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
			emptyTextView.setText(R.string.contacts_list_empty_text);
		}
		else{
			log("CONTACTS SIZE != 0");
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}	
		
	}
	
	public void updateShares(){
		log("updateShares");
		adapter.notifyDataSetChanged();
	}
	
	public void sortByNameDescending(){
		for(int i = 0, j = visibleContacts.size() - 1; i < j; i++) {
			visibleContacts.add(i, visibleContacts.remove(j));
		}
		
		adapter.setContacts(visibleContacts);
	}
	public void sortByNameAscending(){
		updateView();
	}
	
	public boolean showSelectMenuItem(){
		if (adapter != null){
			return adapter.isMultipleSelect();
		}
		
		return false;
	}
	
	public void setOrder(int orderContacts){
		log("setOrder:Contacts");
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

	public void resetAdapter(){
		log("resetAdapter");
		if(adapter!=null){
			adapter.setPositionClicked(-1);
		}
	}
}
