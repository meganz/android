package nz.mega.android.lollipop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nz.mega.android.MegaApplication;
import nz.mega.android.R;
import nz.mega.android.utils.Util;
import nz.mega.components.RoundedImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MegaSharedFolderLollipopAdapter extends RecyclerView.Adapter<MegaSharedFolderLollipopAdapter.ViewHolderShareList> implements OnClickListener, MegaRequestListenerInterface {
	
	Context context;
	int positionClicked;
	ArrayList<MegaShare> shareList;
	MegaNode node;
	RecyclerView listViewActivity;
	
	MegaApiAndroid megaApi;
	
	boolean removeShare = false;
	boolean multipleSelect = false;
	
	OnItemClickListener mItemClickListener;
	RecyclerView listFragment;
	
	AlertDialog permissionsDialog;
	private SparseBooleanArray selectedItems;
	
	final MegaSharedFolderLollipopAdapter megaSharedFolderAdapter;
	
	ProgressDialog statusDialog;
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();
	
	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderShareList holder;
		MegaSharedFolderLollipopAdapter adapter;
		
		public UserAvatarListenerList(Context context, ViewHolderShareList holder, MegaSharedFolderLollipopAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart() avatar");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("onRequestFinish() avatar");
			if (e.getErrorCode() == MegaError.API_OK){
				
				if(request.getEmail()!=null)
				{
					pendingAvatars.remove(request.getEmail());
					
					if (holder.contactMail.compareTo(request.getEmail()) == 0){
						File avatar = null;
						if (context.getExternalCacheDir() != null){
							avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
						}
						else{
							avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
						}
						Bitmap bitmap = null;
						if (avatar.exists()){
							if (avatar.length() > 0){
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
								}
								else{
									holder.imageView.setImageBitmap(bitmap);
									holder.initialLetter.setVisibility(View.GONE);
								}
							}
						}
						
						if(request.getParamType()==1){
							log("(1)request.getText(): "+request.getText());
							holder.nameText=request.getText();
							holder.name=true;
						}
						else if(request.getParamType()==2){
							log("(2)request.getText(): "+request.getText());
							holder.firstNameText = request.getText();
							holder.firstName = true;
						}
						if(holder.name&&holder.firstName){
							holder.textViewContactName.setText(holder.nameText+" "+holder.firstNameText);
							holder.name= false;
							holder.firstName = false;
						}
					}
				}
			}
			else{
				log("E: " + e.getErrorCode() + "_" + e.getErrorString());	
				//TODO Si no tiene avatar, holder.imageView.setImageBitmap(IMAGEN_POR_DEFECTO);
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("onRequestTemporaryError");
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public MegaSharedFolderLollipopAdapter(Context _context, MegaNode node, ArrayList<MegaShare> _shareList, RecyclerView _lv) {
		this.context = _context;
		this.node = node;
		this.shareList = _shareList;
		this.positionClicked = -1;
		this.megaSharedFolderAdapter = this;
		this.listViewActivity = _lv;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	public void setContext(Context context){
		this.context = context;
	}
	
	public void setNode(MegaNode node){
		this.node = node;
	}
	
	public void setListViewActivity(RecyclerView lv){
		this.listViewActivity = lv;
	}
		
	/*private view holder class*/
    class ViewHolderShareList extends RecyclerView.ViewHolder implements View.OnClickListener{
    	RoundedImageView imageView;
    	TextView initialLetter;
//        ImageView imageView;
        TextView textViewContactName; 
        TextView textViewPermissions;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        LinearLayout optionsLayout;
        RelativeLayout optionPermissions;
        RelativeLayout optionRemoveShare;
        int currentPosition;
        String contactMail;
    	boolean name = false;
    	boolean firstName = false;
    	String nameText;
    	String firstNameText;
    	
    	public ViewHolderShareList(View itemView) {
			super(itemView);
            itemView.setOnClickListener(this);
		}
    	
    	@Override
		public void onClick(View v) {
			if(mItemClickListener != null){
				mItemClickListener.onItemClick(v, getPosition());
			}			
		}
    }
    
    public interface OnItemClickListener {
		   public void onItemClick(View view , int position);
	}
	
	public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
		this.mItemClickListener = mItemClickListener;
	}


	@Override
	public ViewHolderShareList onCreateViewHolder(ViewGroup parent, int viewType) {
		
		listViewActivity = (RecyclerView) parent;
		

		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
	    
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shared_folder, parent, false);
		ViewHolderShareList holder = new ViewHolderShareList(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.shared_folder_item_layout);
		holder.imageView = (RoundedImageView) v.findViewById(R.id.shared_folder_contact_thumbnail);
		holder.imageView.getLayoutParams().width = Util.px2dp((54*scaleW), outMetrics);
		holder.imageView.getLayoutParams().height = Util.px2dp((54*scaleH), outMetrics);
		holder.initialLetter = (TextView) v.findViewById(R.id.shared_folder_contact_initial_letter);
		
		holder.textViewContactName = (TextView) v.findViewById(R.id.shared_folder_contact_name);
		holder.textViewContactName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
		holder.textViewContactName.setSingleLine();
		holder.textViewPermissions = (TextView) v.findViewById(R.id.shared_folder_contact_permissions);
		holder.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.shared_folder_contact_three_dots);
		holder.optionsLayout = (LinearLayout) v.findViewById(R.id.shared_folder_options);
		holder.optionPermissions = (RelativeLayout) v.findViewById(R.id.shared_folder_permissions_option_layout);			
		holder.optionRemoveShare = (RelativeLayout) v.findViewById(R.id.shared_folder_remove_share_option_layout);			
		v.setTag(holder); 
		
		return holder;
	}
	

	@Override
	public void onBindViewHolder(ViewHolderShareList holder, int position) {
		log("onBindViewHolder");
		
		if (!multipleSelect) {
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
			
			if (positionClicked != -1) {
				if (positionClicked == position) {
					//				holder.arrowSelection.setVisibility(View.VISIBLE);
//					holder.optionsLayout.setVisibility(View.GONE);
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
					listFragment.smoothScrollToPosition(positionClicked);
				}
				else {
					//				holder.arrowSelection.setVisibility(View.GONE);
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				}
			} 
			else {
				//			holder.arrowSelection.setVisibility(View.GONE);
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
		} else {
			holder.imageButtonThreeDots.setVisibility(View.GONE);		

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			}
		}		

		holder.currentPosition = position;
		
		//Check if the share
		MegaShare share = (MegaShare) getItem(position);
		if (share.getUser() == null){
			holder.contactMail = context.getString(R.string.file_properties_shared_folder_public_link);
		}
		else{
			holder.contactMail = share.getUser();
			MegaUser contact = megaApi.getContact(holder.contactMail);	
			
			holder.textViewContactName.setText(holder.contactMail);
						
			createDefaultAvatar(holder);
			
			int accessLevel = share.getAccess();
			switch(accessLevel){
				case MegaShare.ACCESS_OWNER:
				case MegaShare.ACCESS_FULL:{
					holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_full_access));
					break;
				}
				case MegaShare.ACCESS_READ:{
					holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_read_only));
					break;
				}
				case MegaShare.ACCESS_READWRITE:{
					holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_read_write));
					break;	
				}
			}
			
			UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
			
			holder.name=false;
			holder.firstName=false;
			megaApi.getUserAttribute(contact, 1, listener);
			megaApi.getUserAttribute(contact, 2, listener);
			
			File avatar = null;
			if (context.getExternalCacheDir() != null){
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
			}
			else{
				avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
			}
			Bitmap bitmap = null;
			if (avatar.exists()){
				if (avatar.length() > 0){
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (bitmap == null) {
						avatar.delete();
						if (context.getExternalCacheDir() != null){
							megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + holder.contactMail + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + holder.contactMail + ".jpg", listener);
						}
					}
					else{
						holder.imageView.setImageBitmap(bitmap);
						holder.initialLetter.setVisibility(View.GONE);
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + holder.contactMail + ".jpg", listener);	
					}
					else{
						megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + holder.contactMail + ".jpg", listener);	
					}			
				}
			}	
			else{
				if (!pendingAvatars.contains(holder.contactMail)){
					pendingAvatars.add(holder.contactMail);
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + holder.contactMail + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + holder.contactMail + ".jpg", listener);
					}
				}
			}
		}
		
        holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		
		if (positionClicked != -1){
			if (positionClicked == position){
//				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));				
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				listFragment.smoothScrollToPosition(position);
			}
			else{
//				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_properties_available_layout));
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
		}
		else{
//			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_properties_available_layout));
			holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
		}
		
		if (share.getUser() != null){
			holder.optionPermissions.setVisibility(View.VISIBLE);
			holder.optionPermissions.setTag(holder);
			holder.optionPermissions.setOnClickListener(this);
		}
		else{
			holder.optionPermissions.setVisibility(View.GONE);			
		}
		
		holder.optionRemoveShare.setTag(holder);
		holder.optionRemoveShare.setOnClickListener(this);

	}
	
	public void createDefaultAvatar(ViewHolderShareList holder){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(ManagerActivityLollipop.DEFAULT_AVATAR_WIDTH_HEIGHT,ManagerActivityLollipop.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.color_default_avatar_mega));
		
		
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
		    	holder.initialLetter.setVisibility(View.VISIBLE);
		    	holder.initialLetter.setText(firstLetter);
		    	holder.initialLetter.setTextSize(32);
		    	holder.initialLetter.setTextColor(Color.WHITE);
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
        return shareList.size();
    }
 
    public Object getItem(int position) {
        return shareList.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }
    
	@Override
	public void onClick(View v) {
		log("onClick");
		ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
		int currentPosition = holder.currentPosition;
		final MegaShare s = (MegaShare) getItem(currentPosition);
				
		switch (v.getId()){
			case R.id.shared_folder_permissions_option_layout:{
				log("En el adapter - change");
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
				dialogBuilder.setTitle(context.getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {context.getString(R.string.file_properties_shared_folder_read_only), context.getString(R.string.file_properties_shared_folder_read_write), context.getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, s.getAccess(), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						removeShare = false;
						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(context);
							temp.setMessage(((Activity)context).getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						switch(item) {
	                        case 0:{
	                        	MegaUser u = megaApi.getContact(s.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_READ, megaSharedFolderAdapter);
	                        	break;
	                        }
	                        case 1:{
	                        	MegaUser u = megaApi.getContact(s.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_READWRITE, megaSharedFolderAdapter);
                                break;
	                        }
	                        case 2:{
	                        	MegaUser u = megaApi.getContact(s.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_FULL, megaSharedFolderAdapter);
                                break;
	                        }
	                    }
					}
				});
				permissionsDialog = dialogBuilder.create();
				permissionsDialog.show();
				Resources resources = permissionsDialog.getContext().getResources();
				int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
				TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
		        alertTitle.setTextColor(resources.getColor(R.color.mega));
				int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
				View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
				titleDivider.setBackgroundColor(resources.getColor(R.color.mega));				
				
				positionClicked = -1;
//				((FileContactListActivityLollipop)context).refreshView();
				break;
			}
			case R.id.shared_folder_remove_share_option_layout:{
				log("En el adapter - remove");
				MegaUser c = null;
				if (s.getUser() != null){
					c = megaApi.getContact(s.getUser());
				}
				((FileContactListActivityLollipop)context).removeShare(c);
				positionClicked = -1;
//				((FileContactListActivityLollipop)context).refreshView();
				break;
			}
			case R.id.shared_folder_contact_three_dots:{
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
				break;
			}
		}
	}
	
	public void setShareList (ArrayList<MegaShare> shareList){
		log("setShareList");
		this.shareList = shareList;
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	private static void log(String log) {
		Util.log("MegaSharedFolderAdapter", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		try { 
			statusDialog.dismiss();
		} 
		catch (Exception ex) {}
		
		if (request.getType() == MegaRequest.TYPE_EXPORT){
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, context.getString(R.string.context_node_private), Toast.LENGTH_LONG).show();
			}
			else{
				Util.showErrorAlertDialog(e, (Activity)context);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			if (removeShare){
				if (e.getErrorCode() == MegaError.API_OK){
					ArrayList<MegaShare> sl = megaApi.getOutShares(node);
					Toast.makeText(context, context.getString(R.string.context_share_correctly_removed), Toast.LENGTH_LONG).show();
					for(int i=0;i<sl.size();i++){
						MegaShare sh = sl.get(i);
						if (sh.getAccess() == MegaShare.ACCESS_UNKNOWN){
							sl.remove(i);
						}
					}
					setShareList(sl);
				}
				else{
					Util.showErrorAlertDialog(e, (Activity)context);
				}
				removeShare = false;
			}
			else{
				permissionsDialog.dismiss();
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(context, context.getString(R.string.context_correctly_shared), Toast.LENGTH_LONG).show();
					ArrayList<MegaShare> sl = megaApi.getOutShares(node);
					setShareList(sl);
				}
				else{
					Util.showErrorAlertDialog(e, (Activity)context);
				}
			}
		}
		
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
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

	public MegaShare getContactAt(int position) {
		try {
			if(shareList != null){
				return shareList.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
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
	
	/*
	 * Get list of all selected contacts
	 */
//	public List<MegaUser> getSelectedUsers() {
//		ArrayList<MegaUser> users = new ArrayList<MegaUser>();
//		
//		for (int i = 0; i < selectedItems.size(); i++) {
//			if (selectedItems.valueAt(i) == true) {
//				MegaUser u = getContactAt(selectedItems.keyAt(i));
//				if (u != null){
//					users.add(u);
//				}
//			}
//		}
//		return users;
//	}
	
	/*
	 * Get list of all selected shares
	 */
	public List<MegaShare> getSelectedShares() {
		ArrayList<MegaShare> shares = new ArrayList<MegaShare>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaShare s = getContactAt(selectedItems.keyAt(i));
				if (s != null){
					shares.add(s);
				}
			}
		}
		return shares;
	}
	
	public void setNodes(ArrayList <MegaShare> _shareList){
		this.shareList = _shareList;
		notifyDataSetChanged();
	}
	
	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

}
