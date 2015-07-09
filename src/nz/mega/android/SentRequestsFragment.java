package nz.mega.android;

import java.util.ArrayList;

import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaUser;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class SentRequestsFragment extends Fragment implements OnClickListener, OnItemLongClickListener{
	
	public static int GRID_WIDTH =400;
	
	public static final String ARG_OBJECT = "object";
	
	MegaApiAndroid megaApi;	
	
	Context context;
	ActionBar aB;
	ListView listView;
	MegaContactRequestListAdapter adapterList;
	MegaContactsGridAdapter adapterGrid;
	ImageView emptyImageView;
	TextView emptyTextView;
	
//	LinearLayout outSpaceLayout=null;
//	TextView outSpaceText;
//	Button outSpaceButton;
//	int usedSpacePerc;
	
	private Button addContactButton;
	private ActionMode actionMode;
	
	boolean isList = true;
	
	SentRequestsFragment sentRequestsFragment = this;
	
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
	        View v = inflater.inflate(R.layout.contacts_requests_tab, container, false);
	        
	        listView = (ListView) v.findViewById(R.id.incoming_contacts_list_view);
//			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setItemsCanFocus(false);
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);		        
	        
	        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_contacts_requests);
			emptyTextView = (TextView) v.findViewById(R.id.empty_text_contacts_requests);			
			
			listView.setItemsCanFocus(false);
			if (adapterList == null){
				adapterList = new MegaContactRequestListAdapter(context, contacts, emptyImageView, emptyTextView, listView, ManagerActivity.OUTGOING_REQUEST_ADAPTER);
			}
			else{
				adapterList.setContacts(contacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
			addContactButton = (Button) v.findViewById(R.id.invite_contact_button);
			addContactButton.setOnClickListener(this);
						
			if (adapterList.getCount() == 0){				
				log("adapterList.getCount() == 0");
				emptyImageView.setImageResource(R.drawable.ic_empty_folder);
				emptyTextView.setText(R.string.sent_requests_empty);
				listView.setVisibility(View.GONE);
				addContactButton.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);
			}
			else{
				log("adapterList.getCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				addContactButton.setVisibility(View.GONE);
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
		
		Util.log("SentRequestsFragment", log);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		return false;
	}

//	@Override
//	public void onItemClick(AdapterView<?> parent, View view, int position,
//			long id) {
//		// TODO Auto-generated method stub
//		
//	}
	
//	public ListView getListView(){
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
		contacts = megaApi.getOutgoingContactRequests();
    	if(contacts!=null)
    	{
    		adapterList.setContacts(contacts);
    		adapterList.notifyDataSetChanged();
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
}
