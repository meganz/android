package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;


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
		LogUtil.logDebug("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip, parent, false);

		holderList = new ViewHolderChips(v);
		holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout_chip);

		holderList.textViewName = (TextView) v.findViewById(R.id.name_chip);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			LogUtil.logDebug("Landscape");
			holderList.textViewName.setMaxWidth(Util.scaleWidthPx(260, outMetrics));
		}else{
			LogUtil.logDebug("Portrait");
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
		LogUtil.logDebug("onBindViewHolderList");

		String name = (String) getItem(position);
		holder.textViewName.setText(name);
	}

	@Override
	public void onClick(View v) {
		LogUtil.logDebug("onClick");

		MegaInviteFriendsAdapter.ViewHolderChips holder = (MegaInviteFriendsAdapter.ViewHolderChips) v.getTag();
		if(holder!=null){
			int currentPosition = holder.getLayoutPosition();
			LogUtil.logDebug("Current position: " + currentPosition);

			if(currentPosition<0){
				LogUtil.logWarning("Current position error - not valid value");
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
			LogUtil.logWarning("Error. Holder is Null");
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
		LogUtil.logDebug("setPositionClicked: " + p);
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
		LogUtil.logDebug("setNames");
		this.names = names;

		notifyDataSetChanged();
	}

	public Object getItem(int position) {
		LogUtil.logDebug("getItem");
		return names.get(position);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
