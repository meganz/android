package com.mega.android;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaUser;

public class ContactsFragment extends Fragment implements OnClickListener, OnItemClickListener{

	MegaApiAndroid megaApi;
	
	Context context;
	ActionBar aB;
	ListView listView;
	MegaContactsListAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	
	boolean isList = true;
	
	ArrayList<MegaUser> contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_contacts));
		
		if (((ManagerActivity)context).getmDrawerToggle() != null){
			((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(true);
			((ManagerActivity)context).supportInvalidateOptionsMenu();
		}
		else{
			return null;
		}
		
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
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			listView.setItemsCanFocus(false);
			if (adapterList == null){
				adapterList = new MegaContactsListAdapter(context, visibleContacts);
			}
			else{
				adapterList.setContacts(visibleContacts);
			}
			
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
			
			return v;
		}
		else{
			View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
			
			listView = (ListView) v.findViewById(R.id.contact_grid_view_browser);
	        listView.setOnItemClickListener(null);
	        listView.setItemsCanFocus(false);
	        
	        if (adapterGrid == null){
	        	adapterGrid = new MegaContactsGridAdapter(context, visibleContacts);
	        }
	        else{
	        	adapterGrid.setContacts(visibleContacts);
	        }
	        
	        adapterGrid.setPositionClicked(-1);
			listView.setAdapter(adapterGrid);
			
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

		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		if (isList){
			Intent i = new Intent(context, ContactPropertiesMainActivity.class);
			i.putExtra("name", visibleContacts.get(position).getEmail());
			startActivity(i);
		}
    }
	
	public int onBackPressed(){
		
		if (isList){
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
	

}
