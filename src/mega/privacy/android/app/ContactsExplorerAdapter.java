package mega.privacy.android.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.ContactsExplorerActivity.PhoneContacts;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;


/*
 * Adapter for FilestorageActivity list
 */
public class ContactsExplorerAdapter extends BaseAdapter implements OnClickListener {
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();
	
	private class UserAvatarListenerExplorer implements MegaRequestListenerInterface{

		Context context;
		ViewHolderContactsExplorer holder;
		ContactsExplorerAdapter adapter;
		
		public UserAvatarListenerExplorer(Context context, ViewHolderContactsExplorer holder, ContactsExplorerAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("onRequestFinish()");
			if (e.getErrorCode() == MegaError.API_OK){
				
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
					adapter.notifyDataSetChanged();
				}
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
	
	// Listener for item check
	public interface OnItemCheckClickListener {
		public void onItemCheckClick(int position);
	}
		
	private Context mContext;
	MegaApiAndroid megaApi;
	
	private List<MegaUser> contactsFromMEGA;
	private List<PhoneContacts> contactsFromPhone;
	private boolean megaContacts = true;
	
	private OnItemCheckClickListener checkClickListener;
	
	public ContactsExplorerAdapter(Context context, ArrayList<MegaUser> contactsFromMEGA, ArrayList<PhoneContacts> contactsFromPhone, boolean megaContacts) {
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		setContext(context);
		setContacts(contactsFromMEGA, contactsFromPhone);
		this.megaContacts = megaContacts;
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public void setOnItemCheckClickListener(OnItemCheckClickListener listener) {
		this.checkClickListener = listener;
	}
	
	// Set new contacts
	public void setContacts(List<MegaUser> contactsFromMEGA, List<PhoneContacts> contactsFromPhone){
		this.contactsFromMEGA = contactsFromMEGA;
		this.contactsFromPhone = contactsFromPhone;
		notifyDataSetChanged();
	}
	
	public Object getDocumentAt(int position) 
	{
		if (megaContacts)
		{
			if(position < contactsFromMEGA.size())
			{	
				return contactsFromMEGA.get(position);
			}
		}
		else
		{
			if(position < contactsFromPhone.size())
			{	
				return contactsFromPhone.get(position);
			}
		}
		return null;
	}
	
	@Override
	public int getCount() {
		if (megaContacts){
			if (contactsFromMEGA == null) {
				return 0;
			}
			int size = contactsFromMEGA.size();
			return size == 0 ? 1 : size;
		}
		else{
			if (contactsFromPhone == null){
				return 0;
			}
			int size = contactsFromPhone.size();
			return size == 0 ? 1 : size;
		}
	}

	@Override
    public Object getItem(int position) {
		if (megaContacts)
		{
			if(position < contactsFromMEGA.size())
			{	
				return contactsFromMEGA.get(position);
			}
		}
		else
		{
			if(position < contactsFromPhone.size())
			{
				return contactsFromPhone.get(position);
			}
		}
		return null;
    }

	@Override
    public long getItemId(int position) {
        return position;
    } 
	
	public class ViewHolderContactsExplorer{
		TextView contactNameTextView;
		TextView phoneEmailTextView;
		RoundedImageView imageView;
		RelativeLayout contactImageLayout;
		TextView initialLetter;
		long contactId;
		String contactName;
		String contactMail;
		String phoneNumber;
		int currentPosition;
	}

	@Override
	public View getView(final int position, View rowView, ViewGroup parentView) {
//		boolean isCheckable = mode == Mode.PICK_FILE;
		
		if (megaContacts){
			boolean isCheckable = false;
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (position == 0 && contactsFromMEGA.size() == 0) {
				TextView textView = (TextView) inflater.inflate(R.layout.file_list_empty, parentView, false);
				int resId = R.string.file_browser_empty_folder;
				textView.setText(mContext.getString(resId));
				return textView;
			}
			MegaUser contact = (MegaUser) getItem(position);
			
			int layoutId;
	
	//		if (isCheckable){
	//			layoutId = R.layout.contacts_explorer_item_checkable;
	//		}
	//		else{
				layoutId = R.layout.contact_explorer_item;
	//		}
			
			ViewHolderContactsExplorer holder = new ViewHolderContactsExplorer();
			rowView = inflater.inflate(layoutId, parentView, false);
			holder.contactNameTextView = (TextView) rowView.findViewById(R.id.contact_explorer_name);
			holder.phoneEmailTextView = (TextView) rowView.findViewById(R.id.contact_explorer_phone_mail);
			holder.phoneEmailTextView.setVisibility(View.GONE);
			holder.imageView = (RoundedImageView) rowView.findViewById(R.id.contact_explorer_thumbnail);
			holder.contactImageLayout = (RelativeLayout) rowView.findViewById(R.id.contact_explorer_relative_layout_avatar);
			holder.initialLetter = (TextView) rowView.findViewById(R.id.contact_explorer_initial_letter);
			holder.currentPosition = position;
			holder.contactMail = contact.getEmail();
			
	//		if (isCheckable) {
	//			View checkArea = rowView.findViewById(R.id.checkbox);
	//			checkArea.setOnClickListener(new OnClickListener() {
	//				@Override
	//				public void onClick(View v) {
	//					checkClickListener.onItemCheckClick(position);
	//				}
	//			});
	//		}
			
			createDefaultAvatar(holder, true);
			
			UserAvatarListenerExplorer listener = new UserAvatarListenerExplorer(mContext, holder, this);
			holder.contactNameTextView.setText(contact.getEmail());
			File avatar = null;
			if (mContext.getExternalCacheDir() != null){
				avatar = new File(mContext.getExternalCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
			}
			else{
				avatar = new File(mContext.getCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
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
						if (mContext.getExternalCacheDir() != null){
							megaApi.getUserAvatar(contact, mContext.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(contact, mContext.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
						}
					}
					else{
						holder.imageView.setImageBitmap(bitmap);
						holder.initialLetter.setVisibility(View.GONE);
					}
				}
				else{
					if (mContext.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, mContext.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);	
					}
					else{
						megaApi.getUserAvatar(contact, mContext.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);	
					}			
				}
			}	
			else{
				if (!pendingAvatars.contains(contact.getEmail())){
					pendingAvatars.add(contact.getEmail());
					
					if (mContext.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, mContext.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact, mContext.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
				}
			}
			
			return rowView;
		}
		else{
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			if (position == 0 && contactsFromPhone.size() == 0) {
                TextView textView = (TextView) inflater.inflate(R.layout.file_list_empty, parentView, false);
                int resId = R.string.file_browser_empty_folder;
                textView.setText(mContext.getString(resId));
                return textView;
            }	
			
			PhoneContacts contact = (PhoneContacts) getItem(position);
			
			int layoutId;
			layoutId = R.layout.contact_explorer_item;
			
			ViewHolderContactsExplorer holder = new ViewHolderContactsExplorer();
			
			rowView = inflater.inflate(layoutId, parentView, false);
			holder.contactNameTextView = (TextView) rowView.findViewById(R.id.contact_explorer_name);
			holder.phoneEmailTextView = (TextView) rowView.findViewById(R.id.contact_explorer_phone_mail);
			holder.phoneEmailTextView.setVisibility(View.VISIBLE);
			holder.imageView = (RoundedImageView) rowView.findViewById(R.id.contact_explorer_thumbnail);
			holder.contactImageLayout = (RelativeLayout) rowView.findViewById(R.id.contact_explorer_relative_layout_avatar);
			holder.initialLetter = (TextView) rowView.findViewById(R.id.contact_explorer_initial_letter);
			holder.currentPosition = position;
			holder.contactId = contact.getId();
			holder.contactName = contact.getName();
			holder.contactMail = contact.getEmail();
			holder.phoneNumber = contact.getPhoneNumber();
			
			holder.contactNameTextView.setText(contact.getName());
			if (contact.getEmail() != null){
				holder.phoneEmailTextView.setText(contact.getEmail());
			}
			else if (contact.getPhoneNumber() != null){
				holder.phoneEmailTextView.setText(contact.getPhoneNumber());
			}
			else{
				holder.phoneEmailTextView.setText("");
			}
			
			createDefaultAvatar(holder, false);
//			holder.imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_contact_picture_holo_light));
			
			Uri contactPhotoUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contact.getId()));
			log("PHOTOURI: " + contactPhotoUri);			
			
			InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream( mContext.getContentResolver(), contactPhotoUri);
			if (photo_stream != null){
				BufferedInputStream buf = new BufferedInputStream(photo_stream);
	            Bitmap photoBitmap = BitmapFactory.decodeStream(buf);
	            holder.imageView.setImageBitmap(photoBitmap);
	            holder.initialLetter.setVisibility(View.GONE);
			}
			
			return rowView;
		}
	}
	
	public void createDefaultAvatar(ViewHolderContactsExplorer holder, boolean isMegaContact){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(ManagerActivity.DEFAULT_AVATAR_WIDTH_HEIGHT,ManagerActivity.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		if (isMegaContact){
			p.setColor(mContext.getResources().getColor(R.color.color_default_avatar_mega));
		}
		else{
			p.setColor(mContext.getResources().getColor(R.color.color_default_avatar_phone));
		}
		
		int radius; 
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
        	radius = defaultAvatar.getWidth()/2;
        else
        	radius = defaultAvatar.getHeight()/2;
        
		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		holder.imageView.setImageBitmap(defaultAvatar);
		
		
		Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = mContext.getResources().getDisplayMetrics().density;
	    
	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (isMegaContact){
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
	    else{
	    	if (holder.contactName != null){
	    		if (holder.contactName.length() > 0){
	    			String firstLetter = holder.contactName.charAt(0) + "";
			    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
			    	holder.initialLetter.setVisibility(View.VISIBLE);
			    	holder.initialLetter.setText(firstLetter);
			    	holder.initialLetter.setTextSize(32);
			    	holder.initialLetter.setTextColor(Color.WHITE);
	    		}
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
	
	public void setMegaContacts(boolean megaContacts){
		this.megaContacts = megaContacts;
	}
	
	public boolean getMegaContacts(){
		return megaContacts;
		
	}
	
	@Override
	public void onClick(View v) {
		log("click!");
	}	

	private static void log(String message) {
		Util.log("ContactsExplorerAdapter", message);
	}
}
