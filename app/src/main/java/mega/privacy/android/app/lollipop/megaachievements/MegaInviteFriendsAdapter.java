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
import android.widget.ImageView;
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
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class MegaInviteFriendsAdapter extends RecyclerView.Adapter<MegaInviteFriendsAdapter.ViewHolderChips> implements View.OnClickListener {

	private Context context;
	private int positionClicked;
	ArrayList<String> names;
	private RecyclerView listFragment;
	private MegaApiAndroid megaApi;
	DatabaseHandler dbH = null;
	private InviteFriendsFragment fragment;

	public MegaInviteFriendsAdapter(Context _context, InviteFriendsFragment _fragment, ArrayList<String> _names, RecyclerView _listView) {
		this.context = _context;
		this.names = _names;
		this.fragment = _fragment;
		this.positionClicked = -1;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;

		dbH = DatabaseHandler.getDbHandler(context);
	}

	/*private view holder class*/
    public static class ViewHolderChips extends RecyclerView.ViewHolder{
    	public ViewHolderChips(View v) {
			super(v);
		}


        TextView textViewName;
		ImageView deleteIcon;
        RelativeLayout itemLayout;

    }

	ViewHolderChips holderList = null;

	@Override
	public MegaInviteFriendsAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip, parent, false);

		holderList = new ViewHolderChips(v);
		holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout_chip);

		holderList.textViewName = (TextView) v.findViewById(R.id.name_chip);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			log("Landscape");
			holderList.textViewName.setMaxWidth(Util.scaleWidthPx(260, outMetrics));
		}else{
			log("Portrait");
			holderList.textViewName.setMaxWidth(Util.scaleWidthPx(230, outMetrics));
		}

		holderList.deleteIcon = (ImageView) v.findViewById(R.id.delete_icon_chip);
		holderList.deleteIcon.setOnClickListener(this);

		holderList.deleteIcon.setTag(holderList);

		v.setTag(holderList);

		return holderList;

	}

	@Override
	public void onBindViewHolder(ViewHolderChips holder, int position) {
		log("onBindViewHolderList");

		String name = (String) getItem(position);
		holder.textViewName.setText(name);
	}

	@Override
	public void onClick(View v) {
		log("onClick");

		MegaInviteFriendsAdapter.ViewHolderChips holder = (MegaInviteFriendsAdapter.ViewHolderChips) v.getTag();
		if(holder!=null){
			int currentPosition = holder.getLayoutPosition();
			log("onClick -> Current position: "+currentPosition);

			if(currentPosition<0){
				log("Current position error - not valid value");
				return;
			}
			switch (v.getId()) {
				case R.id.delete_icon_chip: {
					fragment.deleteMail(currentPosition);
					break;
				}
			}
		}
		else{
			log("Error. Holder is Null");
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
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		return names.size();
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		log("setPositionClicked: "+p);
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
	public void setNames (ArrayList<String> names){
		log("setNames");
		this.names = names;

		notifyDataSetChanged();
	}

	public Object getItem(int position) {
		log("getItem");
		return names.get(position);
	}

	private static void log(String log) {
		Util.log("MegaInviteFriendsAdapter", log);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
