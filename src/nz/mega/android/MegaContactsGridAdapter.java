package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nz.mega.android.utils.Util;
import nz.mega.components.RoundedImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MegaContactsGridAdapter extends BaseAdapter{
	
	Context context;
	int positionClicked;
	ArrayList<MegaUser> contacts;
	int numberOfCells;
	MegaApiAndroid megaApi;
	ListView listFragment;
	
	SparseBooleanArray checkedItems = new SparseBooleanArray();
	private ActionMode actionMode;
	boolean multipleSelect = false;
	
	private class ActionBarCallBack implements ActionMode.Callback {
//		
//		boolean selectAll = true;
//		boolean unselectAll = false;
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			List<MegaUser> users = getSelectedUsers();
			final List<MegaUser> multipleContacts = users;;
			
			switch(item.getItemId()){
				case R.id.cab_menu_share_folder:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						((ManagerActivity) context).pickFolderToShare(users);
					}										
					break;
				}
				case R.id.cab_menu_delete:{
					clearSelections();
					hideMultipleSelect();
					if (users.size()>0){
						DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
						        switch (which){
						        case DialogInterface.BUTTON_POSITIVE:						        	
						        	((ManagerActivity) context).removeMultipleContacts(multipleContacts);		        	
						            break;
			
						        case DialogInterface.BUTTON_NEGATIVE:
						            //No button clicked
						            break;
						        }
						    }
						};
			
						AlertDialog.Builder builder = new AlertDialog.Builder(context);
						builder.setMessage(context.getResources().getString(R.string.confirmation_remove_multiple_contacts)).setPositiveButton(R.string.general_yes, dialogClickListener)
						    .setNegativeButton(R.string.general_no, dialogClickListener).show();
						
					}	
					//TODO remove contact
					
					break;
				}
				case R.id.cab_menu_select_all:{
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					actionMode.invalidate();
					break;
				}				
			}
			return false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.contact_fragment_action, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			multipleSelect = false;
			clearSelections();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaUser> selected = getSelectedUsers();

			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_delete).setVisible(true);
				menu.findItem(R.id.cab_menu_share_folder).setVisible(true);
				menu.findItem(R.id.cab_menu_help).setVisible(true);
				menu.findItem(R.id.cab_menu_upgrade_account).setVisible(true);
				menu.findItem(R.id.cab_menu_settings).setVisible(true);
				
				if(selected.size()==contacts.size()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);			
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);	
				}			
								
			}	
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);	
			}
			
			menu.findItem(R.id.cab_menu_help).setVisible(false);
			menu.findItem(R.id.cab_menu_upgrade_account).setVisible(false);
			menu.findItem(R.id.cab_menu_settings).setVisible(false);
			
			return false;
		}		
	}
	
	private class UserAvatarListenerGrid implements MegaRequestListenerInterface{
		
		Context context;
		ViewHolderContactsGrid holder;
		MegaContactsGridAdapter adapter;
		int numView;
		int totalPosition;
		
		public UserAvatarListenerGrid(Context context, ViewHolderContactsGrid holder, MegaContactsGridAdapter adapter, int totalPosition, int numView){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
			this.numView = numView;
			this.totalPosition = totalPosition;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("onRequestFinish()");
			boolean avatarExists = false;
			if (e.getErrorCode() == MegaError.API_OK){
				if (holder.contactMails.get(numView).compareTo(request.getEmail()) == 0){
					File avatar = null;
					if (context.getExternalCacheDir() != null){
						avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMails.get(numView) + ".jpg");
					}
					else{
						avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMails.get(numView) + ".jpg");
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
								avatarExists = true;
								holder.imageViews.get(numView).setImageBitmap(bitmap);
								Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
								holder.imageViews.get(numView).startAnimation(fadeInAnimation);
								holder.initialLetters.get(numView).setVisibility(View.GONE);
							}
						}
					}
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("onRequestTemporaryError()");
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public MegaContactsGridAdapter(Context _context, ArrayList<MegaUser> _contacts, ListView _listView, int _numberOfCells) {
		this.context = _context;
		this.contacts = _contacts;
		this.numberOfCells = _numberOfCells;
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;
	}
	
	/*private view holder class*/
    private class ViewHolderContactsGrid {
    	public LinearLayout cellLayout;
    	public ArrayList<RelativeLayout> relativeLayoutsUser;
    	public ArrayList<RelativeLayout> relativeLayoutsEmpty;
    	public ArrayList<LinearLayout> menuLayouts;
    	public ArrayList<LinearLayout> longClickLayoutsSelected;
    	public ArrayList<LinearLayout> longClickLayoutsUnselected;
    	public ArrayList<ImageButton> threeDots;
    	public ArrayList<RoundedImageView> imageViews;
    	public ArrayList<TextView> initialLetters;
    	public ArrayList<TextView> contactNameViews;
    	public ArrayList<TextView> contactContentViews;
    	
    	public ArrayList<ImageView> optionsProperties;
    	public ArrayList<ImageView> optionsShare;
    	public ArrayList<ImageView> optionsDelete;
    	public ArrayList<ImageView> optionsSendFile;
    	public ArrayList<ImageView> optionsOverflow;
    	
    	public ArrayList<String> contactMails;
    }
    
    ViewHolderContactsGrid holder = null;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (convertView == null){
			holder = new ViewHolderContactsGrid();
			holder.relativeLayoutsUser = new ArrayList<RelativeLayout>();
			holder.relativeLayoutsEmpty = new ArrayList<RelativeLayout>();
			holder.menuLayouts = new ArrayList<LinearLayout>();
			holder.longClickLayoutsSelected = new ArrayList<LinearLayout>();
			holder.longClickLayoutsUnselected = new ArrayList<LinearLayout>();
			holder.threeDots = new ArrayList<ImageButton>();
			holder.imageViews = new ArrayList<RoundedImageView>();
			holder.initialLetters = new ArrayList<TextView>();
			holder.contactNameViews = new ArrayList<TextView>();
			holder.contactContentViews = new ArrayList<TextView>();
			
			holder.optionsProperties = new ArrayList<ImageView>();
			holder.optionsShare = new ArrayList<ImageView>();
			holder.optionsDelete = new ArrayList<ImageView>();
			holder.optionsSendFile = new ArrayList<ImageView>();
			holder.optionsOverflow = new ArrayList<ImageView>();
			
			holder.contactMails = new ArrayList<String>();
			
			convertView = inflater.inflate(R.layout.item_contact_grid_list, parent, false);
			
			holder.cellLayout = (LinearLayout) convertView.findViewById(R.id.contact_cell_layout);
			
			for (int i=0;i<numberOfCells;i++){
				View rLView = inflater.inflate(R.layout.cell_contact_grid_fill, holder.cellLayout, false);
				RelativeLayout rL = (RelativeLayout) rLView.findViewById(R.id.contact_grid_item_complete_layout);
				rL.setLayoutParams(new LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
				holder.cellLayout.addView(rL);
				
				RelativeLayout rLT = (RelativeLayout) rLView.findViewById(R.id.contact_grid_item_layout);
				holder.relativeLayoutsUser.add(rLT);
				
				RelativeLayout rLE = (RelativeLayout) rLView.findViewById(R.id.contact_cell_item_layout_empty);
				holder.relativeLayoutsEmpty.add(rLE);
				
				LinearLayout mL = (LinearLayout) rLView.findViewById(R.id.contact_cell_menu_layout);
				holder.menuLayouts.add(mL);
				
				LinearLayout lcLS = (LinearLayout) rLView.findViewById(R.id.contact_cell_menu_long_click_selected);
				holder.longClickLayoutsSelected.add(lcLS);
				
				LinearLayout lcLU = (LinearLayout) rLView.findViewById(R.id.contact_cell_menu_long_click_unselected);
				holder.longClickLayoutsUnselected.add(lcLU);
				
				ImageView oP = (ImageView) rLView.findViewById(R.id.contact_grid_menu_layout_option_properties);
				holder.optionsProperties.add(oP);
				
				ImageView oS = (ImageView) rLView.findViewById(R.id.contact_grid_menu_layout_option_share);
				holder.optionsShare.add(oS);
				
				ImageView oD = (ImageView) rLView.findViewById(R.id.contact_grid_menu_layout_option_delete);
				holder.optionsDelete.add(oD);
				
				ImageView oSF = (ImageView) rLView.findViewById(R.id.contact_grid_menu_layout_option_send_file);
				holder.optionsSendFile.add(oSF);
				
				ImageButton tD = (ImageButton) rLView.findViewById(R.id.contact_cell_three_dots);
				holder.threeDots.add(tD);
				
				RoundedImageView iV = (RoundedImageView) rLView.findViewById(R.id.contact_grid_thumbnail);
				holder.imageViews.add(iV);
				
				TextView iL = (TextView) rLView.findViewById(R.id.contact_grid_initial_letter);
				holder.initialLetters.add(iL);
				
				TextView cNV = (TextView) rLView.findViewById(R.id.contact_cell_name);
				cNV.setEllipsize(TextUtils.TruncateAt.END);
				cNV.setSingleLine(true);
				holder.contactNameViews.add(cNV);
				
				TextView cCV = (TextView) rLView.findViewById(R.id.contact_cell_content);
				holder.contactContentViews.add(cCV);
				
				holder.contactMails.add("");
			}
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderContactsGrid) convertView.getTag();
		}
		
		for (int i=0;i<numberOfCells;i++){
			int totalPosition = position*numberOfCells + i;
			log("TOTALPOSITION:"  +totalPosition + "__ SIZE: " + contacts.size());
			if (totalPosition > (contacts.size() - 1)){
				holder.relativeLayoutsUser.get(i).setVisibility(View.GONE);
				holder.relativeLayoutsEmpty.get(i).setVisibility(View.VISIBLE);
//				if (holder.contactMails.size() > i){
					holder.contactMails.set(i,  "");
//				}
//				else{
//					holder.contactMails.add(i, "");
//				}
			}
			else{
				holder.relativeLayoutsUser.get(i).setVisibility(View.VISIBLE);
				holder.relativeLayoutsEmpty.get(i).setVisibility(View.GONE);
				
				MegaUser contact = (MegaUser) contacts.get(totalPosition);
				holder.contactMails.set(i, contact.getEmail());
				holder.contactNameViews.get(i).setText(contact.getEmail());
				
				ArrayList<MegaNode> nodes = megaApi.getInShares(contact);
				holder.contactContentViews.get(i).setText(getDescription(nodes));
				
				createDefaultAvatar(holder, totalPosition, i);
				
				if (multipleSelect){
					if (isChecked(totalPosition)){
						holder.longClickLayoutsSelected.get(i).setVisibility(View.VISIBLE);
						holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
						holder.initialLetters.get(i).setVisibility(View.GONE);
					}
					else{
						holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
						holder.longClickLayoutsUnselected.get(i).setVisibility(View.VISIBLE);
						holder.initialLetters.get(i).setVisibility(View.VISIBLE);
					}
				}
				else{
					holder.longClickLayoutsSelected.get(i).setVisibility(View.GONE);
					holder.longClickLayoutsUnselected.get(i).setVisibility(View.GONE);
					holder.initialLetters.get(i).setVisibility(View.VISIBLE);
				}
				
				if (totalPosition == positionClicked){
					holder.menuLayouts.get(i).setVisibility(View.VISIBLE);
				}
				else{
					holder.menuLayouts.get(i).setVisibility(View.GONE);
				}
				
				UserAvatarListenerGrid listener = new UserAvatarListenerGrid(context, holder, this, totalPosition, i);
				
				File avatar = null;
				if (context.getExternalCacheDir() != null){
					avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMails.get(i) + ".jpg");
				}
				else{
					avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMails.get(i) + ".jpg");
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
								megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
							}
							else{
								megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
							}
						}
						else{
							holder.imageViews.get(i).setImageBitmap(bitmap);
							holder.initialLetters.get(i).setVisibility(View.GONE);
						}
					}
					else{
						megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
				}
			}
		}
		
		for (int i=0;i<holder.imageViews.size();i++){
			final int index = i;
			final int totalPosition = position*numberOfCells + i;
			final int positionFinal = position;
			ImageView iV = holder.imageViews.get(i);
			iV.setTag(holder);
			iV.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderContactsGrid holder= (ViewHolderContactsGrid) v.getTag();
					
					String mail = holder.contactMails.get(index);
					onContactClick(holder, positionFinal, index, totalPosition);
				}
			} );
			
			iV.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					ViewHolderContactsGrid holder = (ViewHolderContactsGrid) v.getTag();
					String mail = holder.contactMails.get(index);
					
					onContactLongClick(holder, positionFinal, index, totalPosition);
					
					return true;
				}
			});
			
			ImageButton tD = holder.threeDots.get(i);
			tD.setTag(holder);
			tD.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					log("POSITION: " + positionFinal + "___" + index);
					ViewHolderContactsGrid holder = (ViewHolderContactsGrid) v.getTag();
					
					onThreeDotsClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oP = holder.optionsProperties.get(i);
			oP.setTag(holder);
			oP.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderContactsGrid holder = (ViewHolderContactsGrid) v.getTag();
					
					onPropertiesClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oS = holder.optionsShare.get(i);
			oS.setTag(holder);
			oS.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderContactsGrid holder = (ViewHolderContactsGrid) v.getTag();
					
					onShareClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oDe = holder.optionsDelete.get(i);
			oDe.setTag(holder);
			oDe.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderContactsGrid holder = (ViewHolderContactsGrid) v.getTag();
					
					onDeleteClick(holder, positionFinal, index, totalPosition);
				}
			});
			
			ImageView oSF = holder.optionsSendFile.get(i);
			oSF.setTag(holder);
			oSF.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					ViewHolderContactsGrid holder = (ViewHolderContactsGrid) v.getTag();
					
					onSendFileClick(holder, positionFinal, index, totalPosition);
				}
			});
		}
		
		return convertView;
	}
	
	public void onContactClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onContactClick");
		if (!multipleSelect){
			MegaUser c = megaApi.getContact(holder.contactMails.get(index));
			if(c == null)
			{
				return;
			}
			
			Intent i = new Intent(context, ContactPropertiesMainActivity.class);
			i.putExtra("name", c.getEmail());
			context.startActivity(i);							
			positionClicked = -1;
			notifyDataSetChanged();
		}
		else{
			if (checkedItems.get(totalPosition, false) == false){
				checkedItems.append(totalPosition, true);
			}
			else{
				checkedItems.append(totalPosition, false);
			}				
			updateActionModeTitle();
			notifyDataSetChanged();
		}
	}
	
	public void onContactLongClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onContactLongClick");
    	if (!multipleSelect){
	    	if (positionClicked == -1){
	        	clearSelections();
	        	actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());
	        	checkedItems.append(totalPosition, true);
	        	this.multipleSelect = true;
				updateActionModeTitle();
				notifyDataSetChanged();
	    	}
    	}
    	else{
    		onContactClick(holder, positionFinal, index, totalPosition);
    	}
	}	
	
	public void onThreeDotsClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onThreeDotsClick");
		if (!multipleSelect){
			if (positionClicked == totalPosition){
				holder.menuLayouts.get(index).setVisibility(View.GONE);
				this.positionClicked = -1;
				notifyDataSetChanged();
			}
			else{
				holder.menuLayouts.get(index).setVisibility(View.VISIBLE);
				this.positionClicked = totalPosition;
				notifyDataSetChanged();
			}
		}
	}
	
	public void onPropertiesClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onPropertiesClick");
		MegaUser c = megaApi.getContact(holder.contactMails.get(index));
		if(c == null)
		{
			return;
		}
		
		Intent i = new Intent(context, ContactPropertiesMainActivity.class);
		i.putExtra("name", c.getEmail());
		context.startActivity(i);							
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public void onShareClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onShareClick");
		MegaUser c = megaApi.getContact(holder.contactMails.get(index));
		if(c == null)
		{
			return;
		}
		
		List<MegaUser> user = new ArrayList<MegaUser>();
		user.add(c);
		((ManagerActivity) context).pickFolderToShare(user);
		notifyDataSetChanged();	
	}
	
	public void onDeleteClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onDeleteClick");
		MegaUser c = megaApi.getContact(holder.contactMails.get(index));
		if(c == null)
		{
			return;
		}
		
		((ManagerActivity) context).removeContact(c);
		positionClicked = -1;
		notifyDataSetChanged();	
	}
	
	public void onSendFileClick(ViewHolderContactsGrid holder, int positionFinal, int index, int totalPosition){
		log("onSendFileClick");
		MegaUser c = megaApi.getContact(holder.contactMails.get(index));
		if(c == null)
		{
			return;
		}		

		List<MegaUser> user = new ArrayList<MegaUser>();
		user.add(c);
		((ManagerActivity) context).pickContacToSendFile(user);		
		
		positionClicked = -1;
		notifyDataSetChanged();	
	}
	
	public void createDefaultAvatar(ViewHolderContactsGrid holder, int totalPosition, int numView){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(ManagerActivity.DEFAULT_AVATAR_WIDTH_HEIGHT,ManagerActivity.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
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
		holder.imageViews.get(numView).setImageBitmap(defaultAvatar);
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = context.getResources().getDisplayMetrics().density;
	    
	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (holder.contactMails.get(numView) != null){
		    if (holder.contactMails.get(numView).length() > 0){
		    	log("TEXT: " + holder.contactMails.get(numView));
		    	log("TEXT AT 0: " + holder.contactMails.get(numView).charAt(0));
		    	String firstLetter = holder.contactMails.get(numView).charAt(0) + "";
		    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		    	holder.initialLetters.get(numView).setVisibility(View.VISIBLE);
		    	holder.initialLetters.get(numView).setText(firstLetter);
		    	holder.initialLetters.get(numView).setTextSize(100);
		    	holder.initialLetters.get(numView).setTextColor(Color.WHITE);
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
    public int getCount() {
		float numberOfRows = (float)(contacts.size()) / (float)numberOfCells;
		
		if (numberOfRows > (int)numberOfRows){
			numberOfRows = (int)numberOfRows + 1;
		}
		return (int)numberOfRows;
	}
 
    @Override
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
    	positionClicked = p;
    }
	
	public void setContacts (ArrayList<MegaUser> contacts){
		this.contacts = contacts;
		positionClicked = -1;
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
	
	private void updateActionModeTitle() {
		if (actionMode == null || context == null) {
			return;
		}
		List<MegaUser> users = getSelectedUsers();
		
		Resources res = context.getResources();
		String format = "%d %s";
		
		actionMode.setTitle(String.format(format, users.size(),res.getQuantityString(R.plurals.general_num_contacts, contacts.size())+ " selected"));

		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}
	
	/*
	 * Get list of all selected documents
	 */
	private List<MegaUser> getSelectedUsers() {
		log("getSelectedUsers");
		ArrayList<MegaUser> users = new ArrayList<MegaUser>();
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				MegaUser user = null;
				try {
					if (users != null) {
						user = contacts.get(checkedItems.keyAt(i));
					}
				}
				catch (IndexOutOfBoundsException e) {}
				
				if (user != null){
					users.add(user);
				}
			}
		}
		
		return users;
	}
	
	/*
	 * Clear all selected items
	 */
	private void clearSelections() {
		log("clearSelections");
		for (int i = 0; i < checkedItems.size(); i++) {
			if (checkedItems.valueAt(i) == true) {
				int checkedPosition = checkedItems.keyAt(i);
				checkedItems.append(checkedPosition, false);
			}
		}
		updateActionModeTitle();
		notifyDataSetChanged();
	}
	
	public boolean isChecked(int totalPosition){
		
		if (!multipleSelect){
			return false;
		}
		else{
			if (checkedItems.get(totalPosition, false) == false){
				return false;
			}
			else{
				return true;
			}	
		}
	}
	
	/*
	 * Disable selection
	 */
	void hideMultipleSelect() {
		this.multipleSelect = false;
		if (actionMode != null) {
			actionMode.finish();
		}
	}
	
	public void selectAll(){
		actionMode = ((ActionBarActivity)context).startSupportActionMode(new ActionBarCallBack());

		this.multipleSelect = true;
		for ( int i=0; i< contacts.size(); i++ ) {
			checkedItems.append(i, true);
		}
		updateActionModeTitle();
		notifyDataSetChanged();
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	private static void log(String log) {
		Util.log("MegaContactsGridAdapter", log);
	}
}
