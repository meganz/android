package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.ChatRoom;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.Message;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;


public class MegaRecentChatLollipopAdapter extends RecyclerView.Adapter<MegaRecentChatLollipopAdapter.ViewHolderRecentChatList> implements OnClickListener {

	Context context;
	int positionClicked;
	ArrayList<ChatRoom> chats;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	boolean multipleSelect;
	int type;
	private SparseBooleanArray selectedItems;
	Object fragment;

	public MegaRecentChatLollipopAdapter(Context _context, Object _fragment, ArrayList<ChatRoom> _chats, RecyclerView _listView, int type) {
		log("new adapter");
		this.context = _context;
		this.chats = _chats;
		this.positionClicked = -1;
		this.type = type;
		this.fragment = _fragment;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;
		
		if(chats!=null)
    	{
    		log("Number of chats: "+chats.size());
    	}
    	else{
    		log("Number of chats: NULL");
    	}
	}
	
	/*private view holder class*/
    class ViewHolderRecentChatList extends ViewHolder{
    	public ViewHolderRecentChatList(View arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}
    	RoundedImageView imageView;
    	TextView contactInitialLetter;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
		TextView textViewDate;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
//        ImageView arrowSelection;
        int currentPosition;
        String contactMail;
    	boolean name = false;
    	boolean firstName = false;
    	String nameText;
    	String firstNameText;
    }
    ViewHolderRecentChatList holder;
    
	@Override
	public void onBindViewHolder(ViewHolderRecentChatList holder, int position) {

		holder.currentPosition = position;
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		log("Get the ChatRoom");
		ChatRoom chat = (ChatRoom) getItem(position);
		
		if (!multipleSelect) {
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
			if (positionClicked != -1){
				if (positionClicked == position){
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
					listFragment.smoothScrollToPosition(positionClicked);
				}
				else{
					holder.itemLayout.setBackgroundColor(Color.WHITE);
				}
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			}
		} else {
			log("Multiselect ON");
			holder.imageButtonThreeDots.setVisibility(View.GONE);

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
			}
			else{
				log("NOT selected");
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			}
		}

		ArrayList<MegaContact> contacts = chat.getContacts();
		ArrayList<Message> messages = chat.getMessages();

		if(contacts!=null){
			if(contacts.size()==1){
				holder.contactMail = contacts.get(0).getMail();
				createDefaultAvatar(holder);
				holder.textViewContactName.setText(contacts.get(0).getMail());
			}
			else{
				log("GROUP chat, more than one contact involved");
			}
		}

		if(messages!=null){
			Message lastMessage = messages.get(messages.size()-1);
			String myMail = ((ManagerActivityLollipop) context).getMyAccountInfo().getMyUser().getEmail();
			if(lastMessage.getUser().getMail().equals(myMail)){
				log("The last message is mine");

				Spannable me = new SpannableString("Me: ");
				me.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				holder.textViewContent.setText(me);
				Spannable myMessage = new SpannableString(lastMessage.getMessage());
				myMessage.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				holder.textViewContent.append(myMessage);
			}
			else{
				log("The last message NOT mine");
				holder.textViewContent.setText(lastMessage.getMessage());
			}
			holder.textViewDate.setText(lastMessage.getDate().toString());
		}

		

		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);		
	}

	@Override
	public ViewHolderRecentChatList onCreateViewHolder(ViewGroup parent, int viewType) {
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density); 		
	
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat_list, parent, false);
		holder = new ViewHolderRecentChatList(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.recent_chat_list_item_layout);
		holder.imageView = (RoundedImageView) v.findViewById(R.id.recent_chat_list_thumbnail);
		holder.contactInitialLetter = (TextView) v.findViewById(R.id.recent_chat_list_initial_letter);
		holder.textViewContactName = (TextView) v.findViewById(R.id.recent_chat_list_name);
		holder.textViewContent = (TextView) v.findViewById(R.id.recent_chat_list_content);
		holder.textViewDate = (TextView) v.findViewById(R.id.recent_chat_list_date);
		holder.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.recent_chat_list_three_dots);
		//Right margin
		RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holder.imageButtonThreeDots.getLayoutParams();
		actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0);
		holder.imageButtonThreeDots.setLayoutParams(actionButtonParams);

		holder.itemLayout.setOnClickListener(this);

		v.setTag(holder);

		return holder;
	}
	
	public void createDefaultAvatar(ViewHolderRecentChatList holder){
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
        return chats.size();
    }
 
	public boolean isMultipleSelect() {
		log("isMultipleSelect");
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		log("setMultipleSelect");
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
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
	}
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleSelection(i);
			}
		}
	}

	public void clearSelections() {
		if(selectedItems!=null){
			selectedItems.clear();
		}
		notifyDataSetChanged();
	}
	
	private boolean isItemChecked(int position) {
        return selectedItems.get(position);
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
	public ChatRoom getChatAt(int position) {
		try {
			if (chats != null) {
				return chats.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}
	
	/*
	 * Get list of all selected chats
	 */
	public List<ChatRoom> getSelectedChats() {
		ArrayList<ChatRoom> chats = new ArrayList<ChatRoom>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				ChatRoom r = getChatAt(selectedItems.keyAt(i));
				if (r != null){
					chats.add(r);
				}
			}
		}
		return chats;
	}
	
    public Object getItem(int position) {
        return chats.get(position);
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
		ViewHolderRecentChatList holder = (ViewHolderRecentChatList) v.getTag();
		int currentPosition = holder.currentPosition;
		ChatRoom c = (ChatRoom) getItem(currentPosition);
		
		switch (v.getId()){	
			case R.id.contact_request_list_three_dots:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				log("click three dots!");
				break;
			}
			case R.id.contact_request_list_item_layout:{
				log("click layout!");
				break;
			}
		}
	}
	
	public void setContacts (ArrayList<ChatRoom> chats){
		log("SETCONTACTS!!!!");
		this.chats = chats;
		if(chats!=null)
		{
			log("num requests: "+chats.size());
		}
		positionClicked = -1;
//		listFragment.invalidate();
		notifyDataSetChanged();
	}
	
	public String getDescription(ArrayList<MegaNode> nodes){
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			}
			else{
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		
		return info;
	}
	
	private static void log(String log) {
		Util.log("MegaRecentChatLollipopAdapter", log);
	}

}