package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactsGridAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;


public class ReceivedRequestsFragmentLollipop extends Fragment{
	
	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	RecyclerView listView;
	MegaContactRequestLollipopAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	ImageView emptyImageView;
	TextView emptyTextView;
	RecyclerView.LayoutManager mLayoutManager;
	MegaContactRequest selectedRequest;

	private ActionMode actionMode;
	
	boolean isList = true;
	
	ReceivedRequestsFragmentLollipop receivedRequestsFragment = this;
	
	ArrayList<MegaContactRequest> contacts;
//	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}	
		
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {                
        log("onCreateView");
    	
    	contacts = megaApi.getIncomingContactRequests();
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
    	
    	if (isList){
     
	        View v = inflater.inflate(R.layout.contacts_received_requests_tab, container, false);			
	        listView = (RecyclerView) v.findViewById(R.id.incoming_contacts_list_view);

			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			//Just onClick implemented
//			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator());		        
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_contacts_requests);
			emptyTextView = (TextView) v.findViewById(R.id.empty_text_contacts_requests);			
			
			if (adapterList == null){
				adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, Constants.INCOMING_REQUEST_ADAPTER);
			}
			else{
				adapterList.setContacts(contacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){				
				log("adapterList.getCount() == 0");
				emptyImageView.setImageResource(R.drawable.received_requests_empty);
				emptyTextView.setText(R.string.received_requests_empty);
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("adapterList.getCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
						
			return v;
    	}
    	else{

    	    View v = inflater.inflate(R.layout.contacts_requests_tab, container, false);
    	    return v;
    	}		

    }

	private static void log(String log) {
		Util.log("ReceivedRequestsFragmentLollipop", log);
	}

	public void updateView(){
		contacts = megaApi.getIncomingContactRequests();
		if (adapterList == null){
			adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, Constants.INCOMING_REQUEST_ADAPTER);
			listView.setAdapter(adapterList);
		}
		else{
			adapterList.setContacts(contacts);
		}
	
		adapterList.setPositionClicked(-1);
		
		if (adapterList.getItemCount() == 0){				
			log("adapterList.getCount() == 0");
			emptyImageView.setImageResource(R.drawable.received_requests_empty);
			emptyTextView.setText(R.string.received_requests_empty);
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);
		}
		else{
			log("adapterList.getCount() NOT = 0");
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}		
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
//			if (adapterGrid != null){
//				adapterGrid.setPositionClicked(positionClicked);
//			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
//			if (adapterGrid != null){
//				adapterGrid.notifyDataSetChanged();
//			}
		}
	}
	
	public void setContactRequests()
	{
		log("setContactRequests");
		contacts = megaApi.getIncomingContactRequests();
    	if(contacts!=null)
    	{
    		if(adapterList!=null)
    		{
    			adapterList.setContacts(contacts);
        		adapterList.notifyDataSetChanged();
    		}
    		else
    		{
    			log("adapter==NULL");
    			adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, Constants.INCOMING_REQUEST_ADAPTER);
    		}
    		
    		if (adapterList.getItemCount() == 0){				
				log("adapterList.getCount() == 0");
				emptyImageView.setImageResource(R.drawable.received_requests_empty);
				emptyTextView.setText(R.string.received_requests_empty);
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("adapterList.getCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
		}
	}
	
	public int onBackPressed(){
		log("onBackPressed");

		if (adapterList.getPositionClicked() != -1){
			adapterList.setPositionClicked(-1);
			adapterList.notifyDataSetChanged();
			return 1;
		}
		else{
			return 0;
		}
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

	public void resetAdapter(){
		log("resetAdapter");
		if(adapterList!=null){
			adapterList.setPositionClicked(-1);
		}
	}

}
