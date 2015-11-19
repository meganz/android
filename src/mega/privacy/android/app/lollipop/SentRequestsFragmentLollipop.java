package mega.privacy.android.app.lollipop;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.SlidingUpPanelLayout;
import mega.privacy.android.app.components.SlidingUpPanelLayout.PanelState;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SentRequestsFragmentLollipop extends Fragment implements OnClickListener{
	
	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	RecyclerView listView;
	MegaContactRequestLollipopAdapter adapterList;
	ImageView emptyImageView;
	TextView emptyTextView;
	RecyclerView.LayoutManager mLayoutManager;
	MegaContactRequest selectedRequest = null;
	
	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;
	
    ImageButton fabButton;
	private ActionMode actionMode;
	
	boolean isList = true;
	
	SentRequestsFragmentLollipop sentRequestsFragment = this;
	
	ArrayList<MegaContactRequest> contacts;
//	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	int orderContacts = MegaApiJava.ORDER_DEFAULT_ASC;	
	
	//OPTIONS PANEL
	private SlidingUpPanelLayout slidingOptionsPanel;
	public FrameLayout optionsOutLayout;
	public LinearLayout optionsLayout;
	public LinearLayout optionReinvite;
	public LinearLayout optionDelete;
	
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
    	
    	if (isList){
	        View v = inflater.inflate(R.layout.contacts_sent_requests_tab, container, false);			
	        listView = (RecyclerView) v.findViewById(R.id.incoming_contacts_list_view);
	        listView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
	        listView.setClipToPadding(false);;
			
			listView.addItemDecoration(new SimpleDividerItemDecoration(context));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			//Just onClick implemented
//			listView.addOnItemTouchListener(this);
			listView.setItemAnimator(new DefaultItemAnimator());		        
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_contacts_requests);
			emptyTextView = (TextView) v.findViewById(R.id.empty_text_contacts_requests);	
			
			fabButton = (ImageButton) v.findViewById(R.id.invite_contact_button);
			fabButton.setOnClickListener(this);

			if (adapterList == null){
				adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, ManagerActivityLollipop.OUTGOING_REQUEST_ADAPTER);
			}
			else{
				adapterList.setContacts(contacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){				
				log("adapterList.getItemCount() == 0");
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.sent_requests_empty);
				listView.setVisibility(View.GONE);

				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("adapterList.getItemCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}	
			
			slidingOptionsPanel = (SlidingUpPanelLayout) v.findViewById(R.id.sliding_layout);
			optionsLayout = (LinearLayout) v.findViewById(R.id.contact_request_list_options);
			optionsOutLayout = (FrameLayout) v.findViewById(R.id.contact_request_list_out_options);
			optionReinvite = (LinearLayout) v.findViewById(R.id.contact_list_option_reinvite_layout);
			optionDelete = (LinearLayout) v.findViewById(R.id.contact_list_option_delete_layout);
			
			optionReinvite.setOnClickListener(this);
			optionDelete.setOnClickListener(this);
			
			optionsOutLayout.setOnClickListener(this);
			
			slidingOptionsPanel.setVisibility(View.INVISIBLE);
			slidingOptionsPanel.setPanelState(PanelState.HIDDEN);		
			
			slidingOptionsPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
	            @Override
	            public void onPanelSlide(View panel, float slideOffset) {
	            	log("onPanelSlide, offset " + slideOffset);
//	            	if(slideOffset==0){
//	            		hideOptionsPanel();
//	            	}
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

    	    View v = inflater.inflate(R.layout.contacts_sent_requests_tab, container, false);
    	    return v;
    	}
    }

	public void showOptionsPanel(MegaContactRequest request){		
		log("showOptionsPanel");	
		
		this.selectedRequest = request;
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
    
	private static void log(String log) {		
		Util.log("SentRequestsFragmentLollipop", log);
	}


//	@Override
//	public void onItemClick(AdapterView<?> parent, View view, int position,
//			long id) {
//		// TODO Auto-generated method stub
//		
//	}
	
//	public RecyclerView getListView(){
//		return listView;
//	}
//	
//	public void updateListView(){
//		log("updateListView");
//		if(adapterList!=null)
//		{
////			adapterList.notifyDataSetInvalidated();
//			adapterList.notifyDataSetChanged();
//		}
//	}
	
	public void setContactRequests()
	{
		log("setContactRequests");
		contacts = megaApi.getOutgoingContactRequests();
    	if(contacts!=null)
    	{
    		log("Sent requests: "+contacts.size());
    		if(adapterList!=null){
    			log("adapter!=NULL");
    			adapterList.setContacts(contacts);
        		adapterList.notifyDataSetChanged();
    		}
    		else{
    			adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, emptyImageView, emptyTextView, listView, ManagerActivityLollipop.OUTGOING_REQUEST_ADAPTER);
    		}
    		
    		if (adapterList.getItemCount() == 0){				
				log("adapterList.getItemCount() == 0");
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.sent_requests_empty);
				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("adapterList.getItemCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}    		
		}
	}
	
	public void setPositionClicked(int positionClicked){

		if (adapterList != null){
			adapterList.setPositionClicked(positionClicked);
		}

	}
	
	public void notifyDataSetChanged(){

		if (adapterList != null){
			adapterList.notifyDataSetChanged();
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.invite_contact_button:{				
				((ManagerActivityLollipop)context).showNewContactDialog(null);				
				break;
			}
			case R.id.contact_request_list_out_options:{
				hideOptionsPanel();
				break;
			}
			case R.id.contact_list_option_reinvite_layout:{
				log("optionReinvite");
				hideOptionsPanel();
				((ManagerActivityLollipop) context).reinviteContact(selectedRequest);				
				break;
			}
			case R.id.contact_list_option_delete_layout:{
				log("Remove Invitation");
				hideOptionsPanel();
				((ManagerActivityLollipop) context).removeInvitationContact(selectedRequest);
				break;
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
			hideOptionsPanel();
			setPositionClicked(-1);
			notifyDataSetChanged();
			return 4;
		}
		
		log("Sliding not shown");
		
//		if (adapterList.isMultipleSelect()){
//			adapterList.hideMultipleSelect();
//			return 2;
//		}
		
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

}
