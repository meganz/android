package mega.privacy.android.app.lollipop;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactsGridAdapter;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.SlidingUpPanelLayout;
import mega.privacy.android.app.components.SlidingUpPanelLayout.PanelState;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;


public class ReceivedRequestsFragmentLollipop extends Fragment implements OnClickListener{
	
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
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionAccept;
	public LinearLayout optionDecline;
	public LinearLayout optionIgnore;
	
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
				adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, ManagerActivityLollipop.INCOMING_REQUEST_ADAPTER);
			}
			else{
				adapterList.setContacts(contacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){				
				log("adapterList.getCount() == 0");
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
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
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			optionsLayout = (LinearLayout) v.findViewById(R.id.contact_request_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.contact_request_list_out_options);
			optionAccept = (LinearLayout) v.findViewById(R.id.contact_list_option_accept_layout);
			optionDecline = (LinearLayout) v.findViewById(R.id.contact_list_option_decline_layout);
			optionIgnore = (LinearLayout) v.findViewById(R.id.contact_list_option_ignore_layout);
			
			optionAccept.setOnClickListener(this);
			optionDecline.setOnClickListener(this);
			optionIgnore.setOnClickListener(this);
			
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

    	    View v = inflater.inflate(R.layout.contacts_requests_tab, container, false);
    	    return v;
    	}		

    }
    
	public void showOptionsPanel(MegaContactRequest request){		
		log("showOptionsPanel");	
		
		this.selectedRequest = request;		
		slidingOptionsPanel.setVisibility(View.VISIBLE);
		slidingOptionsPanel.setPanelState(PanelState.COLLAPSED);
	}
	
	public void hideOptionsPanel(){
		log("hideOptionsPanel");
				
		adapterList.setPositionClicked(-1);
		slidingOptionsPanel.setPanelState(PanelState.HIDDEN);
		slidingOptionsPanel.setVisibility(View.GONE);
	}
    
	private static void log(String log) {
		Util.log("ReceivedRequestsFragmentLollipop", log);
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch(v.getId()){
			case R.id.contact_list_option_accept_layout:{
				log("option Accept");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).acceptInvitationContact(selectedRequest);			
				break;
			}
			case R.id.contact_list_option_decline_layout:{
				log("Remove Invitation");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).declineInvitationContact(selectedRequest);
				break;
			}
			case R.id.contact_list_option_ignore_layout:{
				log("Ignore Invitation");
				slidingOptionsPanel.setPanelState(PanelState.HIDDEN);				
				slidingOptionsPanel.setVisibility(View.GONE);
				setPositionClicked(-1);
				notifyDataSetChanged();
				((ManagerActivityLollipop) context).ignoreInvitationContact(selectedRequest);
				break;
			}			
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
    			adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, ManagerActivityLollipop.INCOMING_REQUEST_ADAPTER);
    		} 
    		
    		if (adapterList.getItemCount() == 0){				
				log("adapterList.getCount() == 0");
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.sent_requests_empty);
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
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

}
