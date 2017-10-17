package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
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
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaContactRequestLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

public class SentRequestsFragmentLollipop extends Fragment {
	
	public static int GRID_WIDTH =400;

	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	RecyclerView listView;
	MegaContactRequestLollipopAdapter adapterList;
	ImageView emptyImageView;
	TextView emptyTextView;
	TextView contentText;
	RelativeLayout contentTextLayout;
	LinearLayoutManager mLayoutManager;
	MegaContactRequest selectedRequest = null;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	private ActionMode actionMode;
	
	boolean isList = true;
	
	SentRequestsFragmentLollipop sentRequestsFragment = this;
	
	ArrayList<MegaContactRequest> contacts;
//
	public void activateActionMode(){
		log("activateActionMode");
		if (!adapterList.isMultipleSelect()){
			adapterList.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	/////Multiselect/////
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
			List<MegaContactRequest> requests = adapterList.getSelectedRequest();

			switch(item.getItemId()){
				case R.id.cab_menu_select_all:{
					((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
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
				case R.id.cab_menu_reinvite:{
					ContactController cC = new ContactController(context);
					cC.reinviteMultipleContacts(requests);
					break;
				}
				case R.id.cab_menu_delete:{
					((ManagerActivityLollipop)context).showConfirmationRemoveContactRequests(requests);
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.sent_request_action, menu);
			((ManagerActivityLollipop)context).hideFabButton();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			clearSelections();
			adapterList.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaContactRequest> selected = adapterList.getSelectedRequest();

			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_reinvite).setVisible(true);
				menu.findItem(R.id.cab_menu_delete).setVisible(true);

				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				if(selected.size()==adapterList.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}

			}
			else{
				log("selected is = 0");
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);

				menu.findItem(R.id.cab_menu_reinvite).setVisible(false);
				menu.findItem(R.id.cab_menu_delete).setVisible(false);
			}

			return false;
		}

	}

	public boolean showSelectMenuItem(){
		if (adapterList != null){
			return adapterList.isMultipleSelect();
		}

		return false;
	}

	/*
	 * Clear all selected items
	 */
	public void clearSelections() {
        if(adapterList!=null){
            if(adapterList.isMultipleSelect()){
                adapterList.clearSelections();
            }
        }
	}

	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaContactRequest> users = adapterList.getSelectedRequest();

		Resources res = getResources();
		String format = "%d %s";

		actionMode.setTitle(String.format(format, users.size(),res.getQuantityString(R.plurals.general_num_request, users.size())));

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
	public void hideMultipleSelect() {
		log("hideMultipleSelect");
		adapterList.setMultipleSelect(false);
		((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public void selectAll() {
		if (adapterList != null) {
			if (adapterList.isMultipleSelect()) {
				adapterList.selectAll();
			} else {
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();

				actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
			}

			updateActionModeTitle();
		}
	}
	/////END Multiselect/////

	public static SentRequestsFragmentLollipop newInstance() {
		log("newInstance");
		SentRequestsFragmentLollipop fragment = new SentRequestsFragmentLollipop();
		return fragment;
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}	
    }

	public void updateView(){
		log("updateView");

		contacts = megaApi.getOutgoingContactRequests();
		if(contacts!=null) {
			log("Sent requests: "+contacts.size());
			if (adapterList == null) {
				adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, listView, Constants.OUTGOING_REQUEST_ADAPTER);
				listView.setAdapter(adapterList);
			} else {
				adapterList.setContacts(contacts);
			}

			if (contacts.size() > 0) {
				contentText.setText(contacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, contacts.size()));
			}

			adapterList.setPositionClicked(-1);

			if (adapterList.getItemCount() == 0) {
				log("adapterList.getItemCount() == 0");
				emptyImageView.setImageResource(R.drawable.sent_requests_empty);
				emptyTextView.setText(R.string.sent_requests_empty);
				listView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			} else {
				log("adapterList.getItemCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
    	log("onCreateView");

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
    	
    	contacts = megaApi.getOutgoingContactRequests();
    	if(contacts!=null)
    	{
    		log("Number of requests: "+contacts.size());
    		for(int i=0;i<contacts.size();i++)
    		{
    			log("-----------------REQUEST: "+i);
    			MegaContactRequest contactRequest = contacts.get(i);
    			log("user sent: "+contactRequest.getSourceEmail());
    		}
    	}
    	else{
    		log("Number of requests: NULL");
    	}

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
    	
    	if (isList){
	        View v = inflater.inflate(R.layout.contacts_sent_requests_tab, container, false);			
	        listView = (RecyclerView) v.findViewById(R.id.incoming_contacts_list_view);
	        listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
	        listView.setClipToPadding(false);;
			
			listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.setItemAnimator(new DefaultItemAnimator());		        
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_contacts_requests);
			emptyTextView = (TextView) v.findViewById(R.id.empty_text_contacts_requests);	
			
			emptyImageView.setImageResource(R.drawable.sent_requests_empty);
			emptyTextView.setText(R.string.sent_requests_empty);

			contentTextLayout = (RelativeLayout) v.findViewById(R.id.contact_requests_list_content_text_layout);

			contentText = (TextView) v.findViewById(R.id.contact_requests_list_content_text);

			if (adapterList == null){
				adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, listView, Constants.OUTGOING_REQUEST_ADAPTER);
			}
			else{
				adapterList.setContacts(contacts);
			}

			if (contacts.size() > 0) {
				contentText.setText(contacts.size()+ " " +context.getResources().getQuantityString(R.plurals.general_num_contacts, contacts.size()));
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){				
				log("adapterList.getItemCount() == 0");
				emptyImageView.setImageResource(R.drawable.sent_requests_empty);
				emptyTextView.setText(R.string.sent_requests_empty);
				listView.setVisibility(View.GONE);
				contentTextLayout.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("adapterList.getItemCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				contentTextLayout.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	

			return v;
    	}
    	else{

    	    View v = inflater.inflate(R.layout.contacts_sent_requests_tab, container, false);
    	    return v;
    	}
    }

	private static void log(String log) {		
		Util.log("SentRequestsFragmentLollipop", log);
	}
	
	public void setPositionClicked(int positionClicked){

		if (adapterList != null){
			adapterList.setPositionClicked(positionClicked);
		}
	}

	public int getItemCount(){
		if(adapterList!=null){
			return adapterList.getItemCount();
		}
		return 0;
	}
	
	public void notifyDataSetChanged(){

		if (adapterList != null){
			adapterList.notifyDataSetChanged();
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
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		if (adapterList.isMultipleSelect()){
			adapterList.toggleSelection(position);

			List<MegaContactRequest> users = adapterList.getSelectedRequest();
			if (users.size() > 0){
				updateActionModeTitle();
				((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
			}
		}
		else{
			log("nothing, not multiple select");
		}
	}
}
