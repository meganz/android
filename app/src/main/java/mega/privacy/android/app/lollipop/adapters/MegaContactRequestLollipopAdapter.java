package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.ReceivedRequestsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SentRequestsFragmentLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

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
		logDebug("new adapter");
		this.context = _context;
		this.contacts = _contacts;
		this.positionClicked = -1;
		this.type = type;
		this.fragment = _fragment;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;
		
		if(contacts != null) {
			logDebug("Number of requests: " + contacts.size());
    	} else {
			logWarning("Number of requests: NULL");
    	}
	}
	
	/*private view holder class*/
    class ViewHolderContactsRequestList extends ViewHolder{
    	public ViewHolderContactsRequestList(View arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}
    	RoundedImageView imageView;
		EmojiTextView textViewContactName;
        TextView textViewContent;
        RelativeLayout threeDotsLayout;
        RelativeLayout itemLayout;
        String contactMail;
    	boolean name = false;

    }
    ViewHolderContactsRequestList holder;
    
	@Override
	public void onBindViewHolder(ViewHolderContactsRequestList holder, int position) {		

		holder.imageView.setImageBitmap(null);
		logDebug("Get the MegaContactRequest");
		MegaContactRequest contact = (MegaContactRequest) getItem(position);

						
		if(type==OUTGOING_REQUEST_ADAPTER)
		{
			holder.contactMail = contact.getTargetEmail();
			holder.textViewContactName.setText(contact.getTargetEmail());
			logDebug("User target: " + contact.getTargetEmail());
		}
		else{
			//Incoming request
						
			holder.contactMail = contact.getSourceEmail();
			holder.textViewContactName.setText(contact.getSourceEmail());
			logDebug("User source: " + contact.getSourceEmail());
			
		}

		if (!multipleSelect) {
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			createDefaultAvatar(holder);
		} else {
			logDebug("Multiselect ON");

			if(this.isItemChecked(position)){
				holder.imageView.setImageResource(R.drawable.ic_select_avatar);
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
			}
			else{
				logDebug("NOT selected");
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				createDefaultAvatar(holder);
			}
		}
        
        int status = contact.getStatus();
        String timeStamp = formatLongDateTime(contact.getCreationTime());
        switch (status) {
            case MegaContactRequest.STATUS_ACCEPTED: {
                holder.textViewContent.setText(context.getString(R.string.contact_request_status_accepted,timeStamp));
                break;
            }
            case MegaContactRequest.STATUS_DELETED: {
                holder.textViewContent.setText(context.getString(R.string.contact_request_status_deleted,timeStamp));
                break;
            }
            case MegaContactRequest.STATUS_DENIED: {
                holder.textViewContent.setText(context.getString(R.string.contact_request_status_denied,timeStamp));
                break;
            }
            case MegaContactRequest.STATUS_IGNORED: {
                holder.textViewContent.setText(context.getString(R.string.contact_request_status_ignored,timeStamp));
                break;
            }
            case MegaContactRequest.STATUS_REMINDED: {
                holder.textViewContent.setText(context.getString(R.string.contact_request_status_reminded,timeStamp));
                break;
            }
            case MegaContactRequest.STATUS_UNRESOLVED: {
                holder.textViewContent.setText(context.getString(R.string.contact_request_status_pending,timeStamp));
                break;
            }
        }
		
		holder.itemLayout.setOnLongClickListener(this);
		holder.threeDotsLayout.setTag(holder);
		holder.threeDotsLayout.setOnClickListener(this);
	}

	@Override
	public ViewHolderContactsRequestList onCreateViewHolder(ViewGroup parent,int viewType) {
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

	    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_incom_request_list, parent, false);
		holder = new ViewHolderContactsRequestList(v);
		holder.itemLayout = v.findViewById(R.id.contact_request_list_item_layout);
		holder.imageView = v.findViewById(R.id.contact_request_list_thumbnail);
		holder.textViewContactName = v.findViewById(R.id.contact_request_list_name);
		if(!isScreenInPortrait(context)){
			holder.textViewContactName.setMaxWidthEmojis(scaleWidthPx(280, outMetrics));
		}else{
			holder.textViewContactName.setMaxWidthEmojis(scaleWidthPx(250, outMetrics));
		}
		holder.textViewContent = v.findViewById(R.id.contact_request_list_content);
		holder.threeDotsLayout = v.findViewById(R.id.contact_request_three_dots_layout);

		holder.itemLayout.setOnClickListener(this);

		v.setTag(holder);

		return holder;
	}
	
	private void createDefaultAvatar(ViewHolderContactsRequestList holder){
		int color = ContextCompat.getColor(context, R.color.lollipop_primary_color);
		Bitmap defaultAvatar = getDefaultAvatar(context, color, holder.contactMail , AVATAR_SIZE, true);
		holder.imageView.setImageBitmap(defaultAvatar);
	}
		

	@Override
    public int getItemCount() {
        return contacts.size();
    }
 
	public boolean isMultipleSelect() {
		logDebug("isMultipleSelect");
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		logDebug("multipleSelect: " + multipleSelect);
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
		}
		if(this.multipleSelect)
		{
			selectedItems = new SparseBooleanArray();
		}
	}
	
	public void toggleSelection(int pos) {
		logDebug("Position: " + pos);
		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList view = (MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			logDebug("Start animation: " + pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (selectedItems.size() <= 0){
						if(type==OUTGOING_REQUEST_ADAPTER)
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
		logDebug("Position: " + pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}

		MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList view = (MegaContactRequestLollipopAdapter.ViewHolderContactsRequestList) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			logDebug("Start animation: " + pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (selectedItems.size() <= 0){
						if(type==OUTGOING_REQUEST_ADAPTER)
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
			logWarning("NULL view pos: " + positionToflip);
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
		logDebug("clearSelections");
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
		logDebug("Position: " + p);
    	positionClicked = p;
		notifyDataSetChanged();
    }
    
	@Override
	public void onClick(View v) {

		if(!isOnline(context)){
			if(context instanceof ManagerActivityLollipop){
				((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
			}
			return;
		}

		ViewHolderContactsRequestList holder = (ViewHolderContactsRequestList) v.getTag();
		int currentPosition = holder.getAdapterPosition();
		MegaContactRequest c = (MegaContactRequest) getItem(currentPosition);
		
		switch (v.getId()){	
			case R.id.contact_request_three_dots_layout:{
				if(multipleSelect){
					if(type==OUTGOING_REQUEST_ADAPTER)
					{
						((SentRequestsFragmentLollipop) fragment).itemClick(currentPosition);
					}
					else{
						((ReceivedRequestsFragmentLollipop) fragment).itemClick(currentPosition);
					}
				}
				else{
					if(type==OUTGOING_REQUEST_ADAPTER)
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
				if(type==OUTGOING_REQUEST_ADAPTER)
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
		logDebug("OnLongCLick");

		ViewHolderContactsRequestList holder = (ViewHolderContactsRequestList) view.getTag();
		int currentPosition = holder.getAdapterPosition();

		if(type==OUTGOING_REQUEST_ADAPTER)
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
		logDebug("SETCONTACTS!!!!");
		this.contacts = contacts;
		if(contacts!=null)
		{
			logDebug("Num requests: " + contacts.size());
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
}
