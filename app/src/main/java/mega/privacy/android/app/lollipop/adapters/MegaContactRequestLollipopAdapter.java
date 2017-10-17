package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.ReceivedRequestsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SentRequestsFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;


public class MegaContactRequestLollipopAdapter extends RecyclerView.Adapter<MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList> implements OnClickListener, View.OnLongClickListener {
	
	Context context;
	int positionClicked;
	ArrayList<MegaContactRequest> contacts;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	boolean multipleSelect;
	int type;
	private SparseBooleanArray selectedItems;
	Object fragment;

	public MegaContactRequestLollipopAdapter(Context _context, Object _fragment, ArrayList<MegaContactRequest> _contacts, RecyclerView _listView, int type) {
		log("new adapter");
		this.context = _context;
		this.contacts = _contacts;
		this.positionClicked = -1;
		this.type = type;
		this.fragment = _fragment;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;
		
		if(contacts!=null)
    	{
    		log("Number of requests: "+contacts.size());
    	}
    	else{
    		log("Number of requests: NULL");
    	}
	}
	
	/*private view holder class*/
    class ViewHolderContactsRequestList extends ViewHolder{
    	public ViewHolderContactsRequestList(View arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}
    	RoundedImageView imageView;
    	TextView contactInitialLetter;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
//        ImageView arrowSelection;
        String contactMail;
    	boolean name = false;

    }
    ViewHolderContactsRequestList holder;
    
	@Override
	public void onBindViewHolder(ViewHolderContactsRequestList holder, int position) {		

		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		log("Get the MegaContactRequest");
		MegaContactRequest contact = (MegaContactRequest) getItem(position);

						
		if(type==Constants.OUTGOING_REQUEST_ADAPTER)
		{
			holder.contactMail = contact.getTargetEmail();
			holder.textViewContactName.setText(contact.getTargetEmail());
			log("--------------user target: "+contact.getTargetEmail());
		}
		else{
			//Incoming request
						
			holder.contactMail = contact.getSourceEmail();
			holder.textViewContactName.setText(contact.getSourceEmail());
			log("--------------user source: "+contact.getSourceEmail());	
			
		}

		if (!multipleSelect) {
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			createDefaultAvatar(holder);
		} else {
			log("Multiselect ON");

			if(this.isItemChecked(position)){
				holder.imageView.setImageResource(R.drawable.ic_select_avatar);
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
			}
			else{
				log("NOT selected");
				holder.itemLayout.setBackgroundColor(Color.WHITE);

				createDefaultAvatar(holder);
			}
		}

//		holder.name=false;
//		holder.firstName=false;
//		megaApi.getUserAttribute(contact, 1, listener);
//		megaApi.getUserAttribute(contact, 2, listener);
		
		int status = contact.getStatus();
		switch(status)
		{
			case MegaContactRequest.STATUS_ACCEPTED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (ACCEPTED)");
				break;
			}
			case MegaContactRequest.STATUS_DELETED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (DELETED)");
				break;
			}
			case MegaContactRequest.STATUS_DENIED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (DENIED)");
				break;
			}
			case MegaContactRequest.STATUS_IGNORED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (IGNORED)");
				break;
			}
			case MegaContactRequest.STATUS_REMINDED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (REMINDED)");
				break;
			}
			case MegaContactRequest.STATUS_UNRESOLVED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (PENDING)");
				break;
			}
		}		
		holder.itemLayout.setOnLongClickListener(this);
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);		
	}

	@Override
	public ViewHolderContactsRequestList onCreateViewHolder(ViewGroup parent,int viewType) {
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density); 		
	
		if(type==Constants.OUTGOING_REQUEST_ADAPTER)
		{
			log("ManagerActivityLollipop.OUTGOING_REQUEST_ADAPTER");
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_outg_request_list, parent, false);	
			holder = new ViewHolderContactsRequestList(v);
			holder.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_request_list_item_layout);
			holder.imageView = (RoundedImageView) v.findViewById(R.id.contact_request_list_thumbnail);	
			holder.contactInitialLetter = (TextView) v.findViewById(R.id.contact_request_list_initial_letter);
			holder.textViewContactName = (TextView) v.findViewById(R.id.contact_request_list_name);
			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				log("Landscape");
				holder.textViewContactName.setMaxWidth(Util.scaleWidthPx(280, outMetrics));
			}else{
				log("Portrait");
				holder.textViewContactName.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
			}
			holder.textViewContent = (TextView) v.findViewById(R.id.contact_request_list_content);
			holder.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.contact_request_list_three_dots);
			//Right margin
			RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holder.imageButtonThreeDots.getLayoutParams();
			actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0); 
			holder.imageButtonThreeDots.setLayoutParams(actionButtonParams);

			holder.itemLayout.setOnClickListener(this);

//			holder.optionsLayout = (LinearLayout) v.findViewById(R.id.contact_request_list_options);
			v.setTag(holder);	
		}
		else{
			//Incoming request
			log("ManagerActivityLollipop.INCOMING_REQUEST_ADAPTER");
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_incom_request_list, parent, false);
			holder = new ViewHolderContactsRequestList(v);
			holder.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_request_list_item_layout);
			holder.imageView = (RoundedImageView) v.findViewById(R.id.contact_request_list_thumbnail);	
			holder.contactInitialLetter = (TextView) v.findViewById(R.id.contact_request_list_initial_letter);
			holder.textViewContactName = (TextView) v.findViewById(R.id.contact_request_list_name);
			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				log("Landscape");
				holder.textViewContactName.setMaxWidth(Util.scaleWidthPx(280, outMetrics));
			}else{
				log("Portrait");
				holder.textViewContactName.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
			}
			holder.textViewContent = (TextView) v.findViewById(R.id.contact_request_list_content);
			holder.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.contact_request_list_three_dots);
			//Right margin
			RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holder.imageButtonThreeDots.getLayoutParams();
			actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0); 
			holder.imageButtonThreeDots.setLayoutParams(actionButtonParams);

			holder.itemLayout.setOnClickListener(this);

//			holder.optionsLayout = (LinearLayout) v.findViewById(R.id.contact_request_list_options);
			
			v.setTag(holder);		
		}
		return holder;
	}
	
	public void createDefaultAvatar(ViewHolderContactsRequestList holder){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
		
		int radius; 
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
        	radius = defaultAvatar.getWidth()/2;
        else
        	radius = defaultAvatar.getHeight()/2;
        
		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		holder.imageView.setImageBitmap(defaultAvatar);
		
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = context.getResources().getDisplayMetrics().density;
	    
	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (holder.contactMail != null){
		    if (holder.contactMail.length() > 0){
		    	String firstLetter = holder.contactMail.charAt(0) + "";
		    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		    	holder.contactInitialLetter.setVisibility(View.VISIBLE);
		    	holder.contactInitialLetter.setText(firstLetter);
		    	holder.contactInitialLetter.setTextSize(24);
		    	holder.contactInitialLetter.setTextColor(Color.WHITE);
		    }
	    }
	}
		
	private int getAvatarTextSize (float density){
		float textSize = 0.0f;
		
		if (density > 3.0){
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		}
		else if (density > 2.0){
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		}
		else if (density > 1.5){
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		}
		else if (density > 1.0){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		}
		else if (density > 0.75){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		}
		else{
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f); 
		}
		
		return (int)textSize;
	}

	@Override
    public int getItemCount() {
        return contacts.size();
    }
 
	public boolean isMultipleSelect() {
		log("isMultipleSelect");
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		log("setMultipleSelect");
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
		}
		if(this.multipleSelect)
		{
			selectedItems = new SparseBooleanArray();
		}
	}
	
	public void toggleSelection(int pos) {
		log("toggleSelection");
		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList view = (MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			log("Start animation: "+pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (selectedItems.size() <= 0){
						if(type==Constants.OUTGOING_REQUEST_ADAPTER)
						{
							((SentRequestsFragmentLollipop) fragment).hideMultipleSelect();
						}
						else{
							((ReceivedRequestsFragmentLollipop) fragment).hideMultipleSelect();
						}
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
	}

	public void toggleAllSelection(int pos) {
		log("toggleSelection: "+pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}

		MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList view = (MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			log("Start animation: "+pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (selectedItems.size() <= 0){
						if(type==Constants.OUTGOING_REQUEST_ADAPTER)
						{
							((SentRequestsFragmentLollipop) fragment).hideMultipleSelect();
						}
						else{
							((ReceivedRequestsFragmentLollipop) fragment).hideMultipleSelect();
						}
					}
					notifyItemChanged(positionToflip);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
		else{
			log("NULL view pos: "+positionToflip);
			notifyItemChanged(pos);
		}
	}
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleSelection(i);
			}
		}
	}

	public void clearSelections() {
		log("clearSelections");
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}
	
	private boolean isItemChecked(int position) {
		if(selectedItems!=null){
			return selectedItems.get(position);
		}
		return false;
    }

	public int getSelectedItemCount() {
		return selectedItems.size();
	}

	public List<Integer> getSelectedItems() {
		List<Integer> items = new ArrayList<Integer>(selectedItems.size());
		for (int i = 0; i < selectedItems.size(); i++) {
			items.add(selectedItems.keyAt(i));
		}
		return items;
	}
	
	/*
	 * Get request at specified position
	 */
	public MegaContactRequest getContactAt(int position) {
		try {
			if (contacts != null) {
				return contacts.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}
	
	/*
	 * Get list of all selected contacts
	 */
	public List<MegaContactRequest> getSelectedRequest() {
		ArrayList<MegaContactRequest> requests = new ArrayList<MegaContactRequest>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaContactRequest r = getContactAt(selectedItems.keyAt(i));
				if (r != null){
					requests.add(r);
				}
			}
		}
		return requests;
	}
	
    public Object getItem(int position) {
        return contacts.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
		log("setPositionClicked: "+p);
    	positionClicked = p;
		notifyDataSetChanged();
    }
    
	@Override
	public void onClick(View v) {

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if(!Util.isOnline(context)){
			if(context instanceof ManagerActivityLollipop){
				((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
			}
			return;
		}

		ViewHolderContactsRequestList holder = (ViewHolderContactsRequestList) v.getTag();
		int currentPosition = holder.getAdapterPosition();
		MegaContactRequest c = (MegaContactRequest) getItem(currentPosition);
		
		switch (v.getId()){	
			case R.id.contact_request_list_three_dots:{
				if(multipleSelect){
					if(type==Constants.OUTGOING_REQUEST_ADAPTER)
					{
						((SentRequestsFragmentLollipop) fragment).itemClick(currentPosition);
					}
					else{
						((ReceivedRequestsFragmentLollipop) fragment).itemClick(currentPosition);
					}
				}
				else{
					if(type==Constants.OUTGOING_REQUEST_ADAPTER)
					{
						((ManagerActivityLollipop) context).showSentRequestOptionsPanel(c);
					}
					else{
						((ManagerActivityLollipop) context).showReceivedRequestOptionsPanel(c);
					}
				}

				break;
			}
			case R.id.contact_request_list_item_layout:{
				if(type==Constants.OUTGOING_REQUEST_ADAPTER)
				{
					((SentRequestsFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else{
					((ReceivedRequestsFragmentLollipop) fragment).itemClick(currentPosition);
				}
				break;
			}
		}
	}

	@Override
	public boolean onLongClick(View view) {
		log("OnLongCLick");

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		ViewHolderContactsRequestList holder = (ViewHolderContactsRequestList) view.getTag();
		int currentPosition = holder.getAdapterPosition();

		if(type==Constants.OUTGOING_REQUEST_ADAPTER)
		{
			((SentRequestsFragmentLollipop) fragment).activateActionMode();
			((SentRequestsFragmentLollipop) fragment).itemClick(currentPosition);
		}
		else{
			((ReceivedRequestsFragmentLollipop) fragment).activateActionMode();
			((ReceivedRequestsFragmentLollipop) fragment).itemClick(currentPosition);
		}
		return true;
	}
	
	public void setContacts (ArrayList<MegaContactRequest> contacts){
		log("SETCONTACTS!!!!");
		this.contacts = contacts;
		if(contacts!=null)
		{
			log("num requests: "+contacts.size());
		}
		positionClicked = -1;
//		listFragment.invalidate();
		notifyDataSetChanged();
	}
	
//	public String getDescription(ArrayList<MegaNode> nodes){
//		int numFolders = 0;
//		int numFiles = 0;
//
//		for (int i=0;i<nodes.size();i++){
//			MegaNode c = nodes.get(i);
//			if (c.isFolder()){
//				numFolders++;
//			}
//			else{
//				numFiles++;
//			}
//		}
//
//		String info = "";
//		if (numFolders > 0){
//			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
//			if (numFiles > 0){
//				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
//			}
//		}
//		else {
//			if (numFiles == 0){
//				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
//			}
//			else{
//				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
//			}
//		}
//
//		return info;
//	}
	
	private static void log(String log) {
		Util.log("MegaContactRequestLollipopAdapter", log);
	}

}
