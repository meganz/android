package nz.mega.android;

import java.util.ArrayList;
import java.util.List;

import nz.mega.android.FileStorageActivity.Mode;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.AdapterView.OnItemLongClickListener;
import android.support.v7.view.ActionMode;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ListView;

public class ContactsFragment extends Fragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener{

	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	ListView listView;
	MegaContactsListAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	ImageView emptyImageView;
	TextView emptyTextView;
	
	private Button addContactButton;
	private ActionMode actionMode;
	
	boolean isList = true;
	
	ContactsFragment contactsFragment = this;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;
	
	/////Multiselect/////
	private class ActionBarCallBack implements ActionMode.Callback {
//		
//		boolean selectAll = true;
//		boolean unselectAll = false;
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaUser> users = getSelectedUsers();
			final List<MegaUser> multipleContacts = users;;
			
			switch(item.getItemId()){
				case R.id.cab_menu_share_folder:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						((ManagerActivity) context).pickFolderToShare(users);
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
						        	((ManagerActivity) context).removeMultipleContacts(multipleContacts);		        	
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
			listView.setOnItemLongClickListener(contactsFragment);
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = getSelectedUsers();

			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				menu.findItem(R.id.cab_menu_help).setVisible(true);
				menu.findItem(R.id.cab_menu_upgrade_account).setVisible(true);
				menu.findItem(R.id.cab_menu_settings).setVisible(true);
				
				if(selected.size()==adapterList.getCount()){
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
	 * Get list of all selected contacts
	 */
	private List<MegaUser> getSelectedUsers() {
		ArrayList<MegaUser> usersSelected = new ArrayList<MegaUser>();
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaUser user = adapterList.getContactAt(checkedItems.keyAt(i));
				if (user != null){
					log("User "+user.getEmail());
					usersSelected.add(user);
				}
			}
		}
		return usersSelected;
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		adapterList.setMultipleSelect(false);
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public void selectAll(){
		if (isList){
			actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
	
			adapterList.setMultipleSelect(true);
			for ( int i=0; i< adapterList.getCount(); i++ ) {
				listView.setItemChecked(i, true);
			}
			updateActionModeTitle();
			listView.setOnItemLongClickListener(null);
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
		SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				listView.setItemChecked(checkedPosition, false);
			}
		}
		updateActionModeTitle();
	}
	
	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaUser> users = getSelectedUsers();
		
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
			
			listView = (ListView) v.findViewById(R.id.contacts_list_view);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setItemsCanFocus(false);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			
			
			emptyImageView = (ImageView) v.findViewById(R.id.contact_list_empty_image);
			emptyTextView = (TextView) v.findViewById(R.id.contact_list_empty_text);
			
			listView.setItemsCanFocus(false);
			if (adapterList == null){
				adapterList = new MegaContactsListAdapter(context, visibleContacts, emptyImageView, emptyTextView, listView);
			}
			else{
				adapterList.setContacts(visibleContacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
			addContactButton = (Button) v.findViewById(R.id.add_contact_button);
			addContactButton.setOnClickListener(this);
						
			if (adapterList.getCount() == 0){				
		
				emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
				emptyTextView.setText(R.string.contacts_list_empty_text);
				listView.setVisibility(View.GONE);
				addContactButton.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				listView.setVisibility(View.VISIBLE);
				addContactButton.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
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
			
			listView = (ListView) v.findViewById(R.id.contact_grid_view_browser);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);
	        
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
			}			
			
			return v;
		}			
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
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.add_contact_button:				
				((ManagerActivity)context).showNewContactDialog(null);				
				break;
		}
	}
			
	@Override
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
		
				Intent i = new Intent(context, ContactPropertiesMainActivity.class);
				i.putExtra("name", visibleContacts.get(position).getEmail());
				startActivity(i);
			}
		}
    }
	
	public int onBackPressed(){
		
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
			if (adapterGrid.isMultipleSelect()){
				adapterGrid.hideMultipleSelect();
				return 2;
			}
			
			if (adapterGrid.getPositionClicked() != -1){
				adapterGrid.setPositionClicked(-1);
				adapterGrid.notifyDataSetChanged();
				return 1;
			}
			else{
				return 0;
			}
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
	
	public ListView getListView(){
		return listView;
	}
	
	private static void log(String log) {
		Util.log("ContactsFragment", log);
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
			addContactButton.setVisibility(View.VISIBLE);
		}
		else{
			log("CONTACTS SIZE != 0");
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
			addContactButton.setVisibility(View.GONE);
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

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
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
	
	public void setOrder(int orderContacts){
		this.orderContacts = orderContacts;
	}
}
