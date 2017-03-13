package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUser;

public class ContactsFragmentLollipop extends Fragment{

	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	RecyclerView recyclerView;
	MegaContactsLollipopAdapter adapter;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	RelativeLayout contentTextLayout;
	private ActionMode actionMode;
	DatabaseHandler dbH = null;

//	DatabaseHandler dbH = null;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	boolean isList = true;
	
	ContactsFragmentLollipop contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<MegaContactAdapter>();
	
	int orderContacts;

	MegaUser selectedUser = null;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapter.isMultipleSelect()){
			adapter.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
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

					if (users.size()>0){
						ContactController cC = new ContactController(context);
						cC.pickFolderToShare(users);
					}										
					break;
				}
				case R.id.cab_menu_send_file:{

					if (users.size()>0){
						ContactController cC = new ContactController(context);
						cC.pickFileToSend(users);
					}										
					break;
				}
				case R.id.cab_menu_delete:{
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
			((ManagerActivityLollipop)context).hideFabButton();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			log("onDestroyActionMode");
			clearSelections();
			adapter.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = adapter.getSelectedUsers();
			MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				
				if(selected.size()==adapter.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else if(selected.size()==1){
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_one));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
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

	public static ContactsFragmentLollipop newInstance() {
		log("newInstance");
		ContactsFragmentLollipop fragment = new ContactsFragmentLollipop();
		return fragment;
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		dbH = DatabaseHandler.getDbHandler(context);
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
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility()+ "_" + contacts.get(i).getTimestamp());
			if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

				MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()+""));
				String fullName = "";
				if(contactDB!=null){
					ContactController cC = new ContactController(context);
					fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contacts.get(i).getEmail());
				}
				else{
					//No name, ask for it and later refresh!!
					fullName = contacts.get(i).getEmail();
				}


				MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
				visibleContacts.add(megaContactAdapter);
			}
		}
		orderContacts = ((ManagerActivityLollipop)context).getOrderContacts();
		sortBy(orderContacts);
		
		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
	    
	    isList = ((ManagerActivityLollipop)context).isList();
		
		if (isList){
			log("isList");
			View v = inflater.inflate(R.layout.fragment_contactslist, container, false);
			
			recyclerView = (RecyclerView) v.findViewById(R.id.contacts_list_view);
			recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
			recyclerView.setClipToPadding(false);
			recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			recyclerView.setHasFixedSize(true);
			MegaLinearLayoutManager linearLayoutManager = new MegaLinearLayoutManager(context);
		    recyclerView.setLayoutManager(linearLayoutManager);
		    recyclerView.setItemAnimator(new DefaultItemAnimator()); 			
		
			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_list_empty_text);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.contact_list_content_text_layout);

			contentText = (TextView) v.findViewById(R.id.contact_list_content_text);

			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, emptyImageView, emptyTextView, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST);
			}

			if (visibleContacts.size() > 0) {
				contentText.setText(visibleContacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
			}
		
			adapter.setPositionClicked(-1);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){				
		
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			if (aB != null){
				aB.setTitle(getString(R.string.section_contacts));
			}
			else{
				if (context != null) {
					aB = ((AppCompatActivity) context).getSupportActionBar();
					if (aB != null) {
						aB.setTitle(getString(R.string.section_contacts));
					}
				}
			}

			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
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

			recyclerView.setItemAnimator(new DefaultItemAnimator());			
				
			emptyImageView = (ImageView) v.findViewById(R.id.contact_grid_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_grid_empty_text);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.contact_content_grid_text_layout);

			contentText = (TextView) v.findViewById(R.id.contact_content_text_grid);

			if (adapter == null){
				adapter = new MegaContactsLollipopAdapter(context, this, visibleContacts, emptyImageView, emptyTextView, recyclerView, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}
			else{
				adapter.setContacts(visibleContacts);
				adapter.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID);
			}

			if (visibleContacts.size() > 0) {
				contentText.setText(visibleContacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
			}

			adapter.setPositionClicked(-1);
			adapter.setMultipleSelect(false);
			recyclerView.setAdapter(adapter);
						
			if (adapter.getItemCount() == 0){				
		
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				recyclerView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				recyclerView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}

			if (aB != null){
				aB.setTitle(getString(R.string.section_contacts));
			}
			else{
				if (context != null) {
					aB = ((AppCompatActivity) context).getSupportActionBar();
					if (aB != null) {
						aB.setTitle(getString(R.string.section_contacts));
					}
				}
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

				MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contacts.get(i).getHandle()+""));
				String fullName = "";
				if(contactDB!=null){
					ContactController cC = new ContactController(context);
					fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contacts.get(i).getEmail());
				}
				else{
					//No name, ask for it and later refresh!!
					fullName = contacts.get(i).getEmail();
				}

				MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
				visibleContacts.add(megaContactAdapter);
			}
		}
		
		adapter.setContacts(visibleContacts);

		if (visibleContacts.size() > 0) {
			contentText.setText(visibleContacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, visibleContacts.size()));
		}
	}

	public void updateOrder(){
		if(isAdded()){
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    public void itemClick(int position) {
		log("itemClick");
					
		if (adapter.isMultipleSelect()){
			log("multiselect ON");
			adapter.toggleSelection(position);

			List<MegaUser> users = adapter.getSelectedUsers();
			if (users.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
	
			Intent i = new Intent(context, ContactInfoActivityLollipop.class);
			i.putExtra("name", visibleContacts.get(position).getMegaUser().getEmail());
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
			log("CONTACTS SIZE != 0 ---> "+visibleContacts.size());
			recyclerView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}
	}
	
	public void updateShares(){
		log("updateShares");
		adapter.notifyDataSetChanged();
	}

	public void sortBy(int orderContacts){
		log("sortByName");

		if(orderContacts == MegaApiJava.ORDER_DEFAULT_DESC){
			Collections.sort(visibleContacts,  Collections.reverseOrder(new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					String name1 = c1.getFullName();
					String name2 = c2.getFullName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				}
			}));
		}
		else if(orderContacts == MegaApiJava.ORDER_CREATION_ASC){
			Collections.sort(visibleContacts,  new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					long timestamp1 = c1.getMegaUser().getTimestamp();
					long timestamp2 = c2.getMegaUser().getTimestamp();

					long result = timestamp2 - timestamp1;
					return (int)result;
				}
			});
		}
		else if(orderContacts == MegaApiJava.ORDER_CREATION_DESC){
			Collections.sort(visibleContacts,  Collections.reverseOrder(new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					long timestamp1 = c1.getMegaUser().getTimestamp();
					long timestamp2 = c2.getMegaUser().getTimestamp();

					long result = timestamp2 - timestamp1;
					return (int)result;
				}
			}));
		}
		else{
			Collections.sort(visibleContacts, new Comparator<MegaContactAdapter>(){

				public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
					String name1 = c1.getFullName();
					String name2 = c2.getFullName();
					int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
					if (res == 0) {
						res = name1.compareTo(name2);
					}
					return res;
				}
			});
		}
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

	public ArrayList<MegaContactAdapter> getVisibleContacts() {
		return visibleContacts;
	}

	public void setVisibleContacts(ArrayList<MegaContactAdapter> visibleContacts) {
		this.visibleContacts = visibleContacts;
	}
}
