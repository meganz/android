package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.PhoneContactInfo;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;


/*
 * Adapter for FilestorageActivity list
 */
public class PhoneContactsLollipopAdapter extends RecyclerView.Adapter<PhoneContactsLollipopAdapter.ViewHolderPhoneContactsLollipop> implements OnClickListener, SectionTitleProvider {

	DatabaseHandler dbH = null;
	public static int MAX_WIDTH_CONTACT_NAME_LAND=450;
	public static int MAX_WIDTH_CONTACT_NAME_PORT=200;

	@Override
	public String getSectionTitle(int position) {
		return phoneContacts.get(position).getName().substring(0, 1).toUpperCase();
	}

	private class ContactPicture extends AsyncTask<Void, Void, Long> {

		Context context;
		ViewHolderPhoneContactsLollipop holder;
		PhoneContactsLollipopAdapter adapter;
		Bitmap photo = null;


		public ContactPicture(Context context, ViewHolderPhoneContactsLollipop holder, PhoneContactsLollipopAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		protected Long doInBackground(Void... args) {
			logDebug("doInBackGround");

			try {
				InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
						ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(holder.contactId)));

				if (inputStream != null) {
					photo = BitmapFactory.decodeStream(inputStream);
				}

				assert inputStream != null;
				inputStream.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			return new Long(holder.contactId);
		}


		@Override
		protected void onPostExecute(Long id) {
			if (photo != null){
				if (holder.contactId == id){
					holder.imageView.setImageBitmap(photo);
					adapter.notifyDataSetChanged();
				}
			}
		}
	}

	// Listener for item check
	public interface OnItemCheckClickListener {
		public void onItemCheckClick(int position);
	}

	private Context mContext;
	MegaApiAndroid megaApi;
	OnItemClickListener mItemClickListener;
	private List<PhoneContactInfo> phoneContacts;
	SparseBooleanArray selectedContacts;

	private OnItemCheckClickListener checkClickListener;

	public PhoneContactsLollipopAdapter(Context context, ArrayList<PhoneContactInfo> phoneContacts, SparseBooleanArray selectedContacts) {
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		setContext(context);
		this.phoneContacts = phoneContacts;
		this.selectedContacts = selectedContacts;
	}

	public PhoneContactsLollipopAdapter(Context context, ArrayList<PhoneContactInfo> phoneContacts) {
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		setContext(context);
		this.phoneContacts = phoneContacts;
		this.selectedContacts = null;
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public void setOnItemCheckClickListener(OnItemCheckClickListener listener) {
		this.checkClickListener = listener;
	}

	// Set new contacts
	public void setContacts(List<PhoneContactInfo> phoneContacts){
		this.phoneContacts = phoneContacts;
		notifyDataSetChanged();

	}

	@Override
	public int getItemCount() {

		if (phoneContacts == null) {
			return 0;
		}

		return phoneContacts.size();

	}

    public PhoneContactInfo getItem(int position) {

		if(position < phoneContacts.size())
		{
			return phoneContacts.get(position);
		}

		return null;
    }

	@Override
    public long getItemId(int position) {
        return position;
    }

	public class ViewHolderPhoneContactsLollipop extends RecyclerView.ViewHolder implements OnClickListener{
		RelativeLayout contactLayout;
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
		
		public ViewHolderPhoneContactsLollipop(View itemView) {
			super(itemView);
            itemView.setOnClickListener(this);
		}
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(mItemClickListener != null){
				mItemClickListener.onItemClick(v, getPosition());
			}			
		}
	}
	
	public interface OnItemClickListener {
		   public void onItemClick(View view, int position);
	}
	
	public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
		this.mItemClickListener = mItemClickListener;
	}
	
	public ViewHolderPhoneContactsLollipop onCreateViewHolder(ViewGroup parentView, int viewType) {
		
	    dbH = DatabaseHandler.getDbHandler(mContext);
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		
		View rowView = inflater.inflate(R.layout.contact_explorer_item, parentView, false);
		ViewHolderPhoneContactsLollipop holder = new ViewHolderPhoneContactsLollipop(rowView);

		holder.contactLayout = (RelativeLayout) rowView.findViewById(R.id.contact_list_item_layout);
		holder.contactNameTextView = (TextView) rowView.findViewById(R.id.contact_explorer_name);

		if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_LAND, mContext.getResources().getDisplayMetrics());
			holder.contactNameTextView.setMaxWidth((int) width);
		}
		else{
			float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_PORT, mContext.getResources().getDisplayMetrics());
			holder.contactNameTextView.setMaxWidth((int) width);
		}

		holder.phoneEmailTextView = (TextView) rowView.findViewById(R.id.contact_explorer_phone_mail);
//		holder.phoneEmailTextView.setVisibility(View.GONE);
		holder.imageView = (RoundedImageView) rowView.findViewById(R.id.contact_explorer_thumbnail);
		holder.contactImageLayout = (RelativeLayout) rowView.findViewById(R.id.contact_explorer_relative_layout_avatar);
		holder.initialLetter = (TextView) rowView.findViewById(R.id.contact_explorer_initial_letter);

		return holder;
		
	}

	@Override
	public void onBindViewHolder(ViewHolderPhoneContactsLollipop holder, int position) {

		boolean isCheckable = false;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		PhoneContactInfo contact = getItem(position);

		holder.currentPosition = position;
		holder.contactMail = contact.getEmail();
		holder.contactName = contact.getName();
		holder.contactId = contact.getId();

		holder.contactNameTextView.setText(contact.getName());
		holder.phoneEmailTextView.setText(contact.getEmail());

		holder.contactLayout.setBackgroundColor(Color.WHITE);

		createDefaultAvatar(holder, false);

		try {
			InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(),
					ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(holder.contactId)));

			if (inputStream != null) {
				Bitmap photo = BitmapFactory.decodeStream(inputStream);
				holder.imageView.setImageBitmap(photo);
				inputStream.close();
				holder.initialLetter.setVisibility(View.GONE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createDefaultAvatar(ViewHolderPhoneContactsLollipop holder, boolean isMegaContact){
		logDebug("isMegaContact: " + isMegaContact);
		
		Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		if (isMegaContact){
			p.setColor(ContextCompat.getColor(mContext, R.color.lollipop_primary_color));
		}
		else{
			p.setColor(ContextCompat.getColor(mContext, R.color.color_default_avatar_phone));
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
		logDebug("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (isMegaContact){
		    if (holder.contactMail != null){
			    if (holder.contactMail.length() > 0){
			    	String firstLetter = holder.contactMail.charAt(0) + "";
			    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
			    	holder.initialLetter.setVisibility(View.VISIBLE);
			    	holder.initialLetter.setText(firstLetter);
			    	holder.initialLetter.setTextSize(24);
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
			    	holder.initialLetter.setTextSize(24);
			    	holder.initialLetter.setTextColor(Color.WHITE);
	    		}
	    	}
	    }
	}
	
	@Override
	public void onClick(View v) {
		logDebug("click!");
	}
}
