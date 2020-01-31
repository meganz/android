package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;

public class MegaReferralBonusesAdapter extends RecyclerView.Adapter<MegaReferralBonusesAdapter.ViewHolderReferralBonuses>{

	private Context context;
	private int positionClicked;
	ArrayList<ReferralBonus> referralBonuses;
	private RecyclerView listFragment;
	private MegaApiAndroid megaApi;
	DatabaseHandler dbH = null;
	private ReferralBonusesFragment fragment;

	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderReferralBonusesList holder;
		MegaReferralBonusesAdapter adapter;

		public UserAvatarListenerList(Context context, ViewHolderReferralBonusesList holder, MegaReferralBonusesAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			logDebug("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
			logDebug("onRequestFinish()");
			if (e.getErrorCode() == MegaError.API_OK){
				boolean avatarExists = false;

				if (holder.contactMail.compareTo(request.getEmail()) == 0){
					File avatar = buildAvatarFile(context,holder.contactMail + ".jpg");
					Bitmap bitmap = null;
					if (isFileAvailable(avatar)){
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
								holder.imageView.setImageBitmap(bitmap);
							}
						}
					}
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			logWarning("onRequestTemporaryError");
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

		}

	}

	public MegaReferralBonusesAdapter(Context _context, ReferralBonusesFragment _fragment, ArrayList<ReferralBonus> _referralBonuses, RecyclerView _listView) {
		this.context = _context;
		this.referralBonuses = _referralBonuses;
		this.fragment = _fragment;
		this.positionClicked = -1;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;

		dbH = DatabaseHandler.getDbHandler(context);
	}

	/*private view holder class*/
    public static class ViewHolderReferralBonuses extends RecyclerView.ViewHolder{
    	public ViewHolderReferralBonuses(View v) {
			super(v);
		}

        TextView textViewContactName;
        TextView textViewStorage;
		TextView textViewTransfer;
		TextView textViewDaysLeft;
        RelativeLayout itemLayout;
        String contactMail;
    }

    public class ViewHolderReferralBonusesList extends ViewHolderReferralBonuses{
    	public ViewHolderReferralBonusesList(View v) {
			super(v);
		}
    	RoundedImageView imageView;
    }

	ViewHolderReferralBonusesList holderList = null;
	@Override
	public MegaReferralBonusesAdapter.ViewHolderReferralBonuses onCreateViewHolder(ViewGroup parent, int viewType) {
		logDebug("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_referral_bonus, parent, false);

		holderList = new ViewHolderReferralBonusesList(v);
		holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.referral_bonus_item_layout);
		holderList.imageView = (RoundedImageView) v.findViewById(R.id.referral_bonus_thumbnail);
		holderList.textViewContactName = (TextView) v.findViewById(R.id.referral_bonus_name);
		holderList.textViewStorage = (TextView) v.findViewById(R.id.referral_bonus_storage);
		holderList.textViewTransfer = (TextView) v.findViewById(R.id.referral_bonus_transfer);
		holderList.textViewDaysLeft = (TextView) v.findViewById(R.id.referral_bonus_days_left);

		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			logDebug("Landscape configuration");
			holderList.textViewContactName.setMaxWidth(scaleWidthPx(280, outMetrics));
		}
		else{
			holderList.textViewContactName.setMaxWidth(scaleWidthPx(230, outMetrics));
		}

		holderList.itemLayout.setTag(holderList);

		v.setTag(holderList);

		return holderList;

	}

	@Override
	public void onBindViewHolder(ViewHolderReferralBonuses holder, int position) {
		logDebug("onBindViewHolderList");
		((ViewHolderReferralBonusesList)holder).imageView.setImageBitmap(null);

		ReferralBonus referralBonus = (ReferralBonus) getItem(position);
		holder.contactMail = referralBonus.getEmails().get(0);
		MegaUser contact = megaApi.getContact(holder.contactMail);
		long handle = contact.getHandle();

		String fullName = getContactNameDB(megaApi, context, handle);
		if(fullName ==  null) fullName = holder.contactMail;

		logDebug("Contact: " + holder.contactMail + " name: " + fullName);

		holder.textViewContactName.setText(fullName);

		holder.itemLayout.setBackgroundColor(Color.WHITE);

		Bitmap defaultAvatar = getDefaultAvatar(context, getColorAvatar(context, megaApi, contact), fullName, AVATAR_SIZE, true);
		((ViewHolderReferralBonusesList)holder).imageView.setImageBitmap(defaultAvatar);

		UserAvatarListenerList listener = new UserAvatarListenerList(context, ((ViewHolderReferralBonusesList)holder), this);

		File avatar = buildAvatarFile(context,holder.contactMail + ".jpg");
        Bitmap bitmap = null;
        if (isFileAvailable(avatar)){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (bitmap == null) {
					avatar.delete();
                    megaApi.getUserAvatar(contact,buildAvatarFile(context,contact.getEmail() + ".jpg").getAbsolutePath(),listener);
                }
				else{
					logDebug("Do not ask for user avatar - its in cache: " + avatar.getAbsolutePath());
					((ViewHolderReferralBonusesList)holder).imageView.setImageBitmap(bitmap);
				}
			}
			else{
                megaApi.getUserAvatar(contact,buildAvatarFile(context,contact.getEmail() + ".jpg").getAbsolutePath(),listener);
			}
		}
		else{
            megaApi.getUserAvatar(contact,buildAvatarFile(context,contact.getEmail() + ".jpg").getAbsolutePath(),listener);
		}


		holder.textViewStorage.setText(getSizeString(referralBonus.getStorage()));
		holder.textViewTransfer.setText(getSizeString(referralBonus.getTransfer()));

		if(referralBonus.getDaysLeft()<=15){
			holderList.textViewDaysLeft.setTextColor(ContextCompat.getColor(context,R.color.login_title));
		}

		if(referralBonus.getDaysLeft()>0){
			holderList.textViewDaysLeft.setText(context.getResources().getString(R.string.general_num_days_left, (int)referralBonus.getDaysLeft()));

		}
		else{
			holderList.textViewDaysLeft.setText(context.getResources().getString(R.string.expired_achievement));
		}

//		holder.imageButtonThreeDots.setTag(holder);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		return referralBonuses.size();
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		logDebug("setPositionClicked: " + p);
		positionClicked = p;
		notifyDataSetChanged();
	}

//	public MegaUser getDocumentAt(int position) {
//		MegaContactAdapter megaContactAdapter = null;
//		if(position < contacts.size())
//		{
//			megaContactAdapter = contacts.get(position);
//			return megaContactAdapter.getMegaUser();
//		}
//
//		return null;
//	}
//
	public void setReferralBonuses (ArrayList<ReferralBonus> referralBonuses){
		logDebug("setReferralBonuses");
		this.referralBonuses = referralBonuses;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	public Object getItem(int position) {
		logDebug("getItem");
		return referralBonuses.get(position);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
