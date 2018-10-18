package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.managerSections.NotificationsFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;


public class MegaNotificationsAdapter extends RecyclerView.Adapter<MegaNotificationsAdapter.ViewHolderNotifications> implements OnClickListener{

	public static int MAX_WIDTH_CONTACT_NAME_LAND=270;
	public static int MAX_WIDTH_CONTACT_NAME_PORT=230;

	private Context context;
	private int positionClicked;
	private ArrayList<MegaUserAlert> notifications;
	private RecyclerView listFragment;
	private MegaApiAndroid megaApi;

	private NotificationsFragmentLollipop fragment;

	public MegaNotificationsAdapter(Context _context, NotificationsFragmentLollipop _fragment, ArrayList<MegaUserAlert> _notifications, RecyclerView _listView) {
		this.context = _context;
		this.notifications = _notifications;
		this.fragment = _fragment;
		this.positionClicked = -1;


		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;
	}

	/*private view holder class*/
    public static class ViewHolderNotifications extends RecyclerView.ViewHolder{
    	public ViewHolderNotifications(View v) {
			super(v);
		}

		LinearLayout itemLayout;

		ImageView titleIcon;
    	TextView titleText;

    	ImageView firstLineIcon;
    	TextView firstLineText;
    	TextView newText;

    	TextView secondLineText;
    	TextView dateText;
    }

	ViewHolderNotifications holder = null;

	@Override
	public ViewHolderNotifications onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_list, parent, false);

		holder = new ViewHolderNotifications(v);

		holder.itemLayout = (LinearLayout) v.findViewById(R.id.notification_list_item_layout);

		holder.titleIcon = (ImageView) v.findViewById(R.id.notification_title_icon);
		holder.titleText = (TextView) v.findViewById(R.id.notification_title_text);

		holder.firstLineIcon = (ImageView) v.findViewById(R.id.notification_first_line_icon);
		holder.firstLineText = (TextView) v.findViewById(R.id.notification_first_line_text);
		holder.newText = (TextView) v.findViewById(R.id.notification_new_label);

		holder.secondLineText = (TextView) v.findViewById(R.id.notifications_text);
		holder.dateText = (TextView) v.findViewById(R.id.notifications_date);

		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			log("onCreate: Landscape configuration");
			holder.firstLineText.setMaxWidth(Util.scaleWidthPx(MAX_WIDTH_CONTACT_NAME_LAND, outMetrics));
		}
		else{
			holder.firstLineText.setMaxWidth(Util.scaleWidthPx(MAX_WIDTH_CONTACT_NAME_PORT, outMetrics));
		}

		holder.itemLayout.setTag(holder);
		holder.itemLayout.setOnClickListener(this);

		v.setTag(holder);

		return holder;
	}

	@Override
	public void onBindViewHolder(ViewHolderNotifications holder, int position) {
		log("onBindViewHolder");

		MegaUserAlert alert = (MegaUserAlert) getItem(position);

		int alertType = alert.getType();

		String title = alert.getHeading();
		log(alert.getHeading()+ " " +alert.getTypeString() + " " + alert.getTitle() + alert.getString(0));

		String text = alert.getTitle();

		switch (alertType){

			case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);

				holder.firstLineText.setText(context.getString(R.string.title_contact_request_notification));

				String email = alert.getEmail();
				text = context.getString(R.string.notification_new_contact_request, email);

				break;
			}
			case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				holder.firstLineText.setText(context.getString(R.string.title_acceptance_contact_request_notification));

				String email = alert.getEmail();
				text = context.getString(R.string.notification_new_contact, email);
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED:{
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU:{
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED:{
				title = context.getString(R.string.section_contacts).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.titleIcon.setVisibility(View.GONE);
				holder.firstLineIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_NEWSHARE:{
				title = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				long handle = alert.getNodeHandle();
				MegaNode node = megaApi.getNodeByHandle(handle);
				if(node!=null){
					if(node.isInShare()){
						holder.titleIcon.setVisibility(View.VISIBLE);
						holder.firstLineIcon.setVisibility(View.VISIBLE);
						holder.firstLineText.setText(node.getName());
					}
				}
				else{
					log("Node path: "+alert.getPath());
				}

				String email = alert.getEmail();
				text = context.getString(R.string.notification_new_shared_folder, email);

				break;
			}
			case MegaUserAlert.TYPE_DELETEDSHARE:{
				title = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.titleIcon.setVisibility(View.VISIBLE);
				holder.firstLineIcon.setVisibility(View.VISIBLE);

				break;
			}
			case MegaUserAlert.TYPE_NEWSHAREDNODES:{
				title = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.titleIcon.setVisibility(View.VISIBLE);
				holder.firstLineIcon.setVisibility(View.VISIBLE);

				break;
			}
			case MegaUserAlert.TYPE_REMOVEDSHAREDNODES:{
				title = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.titleIcon.setVisibility(View.VISIBLE);
				holder.firstLineIcon.setVisibility(View.VISIBLE);

				break;
			}
			case MegaUserAlert.TYPE_PAYMENT_SUCCEEDED:{
				title = alert.getHeading();

				break;
			}
			case MegaUserAlert.TYPE_PAYMENT_FAILED:{
				break;
			}
			case MegaUserAlert.TYPE_TAKEDOWN:{
				break;
			}
			case MegaUserAlert.TYPE_TAKEDOWN_REINSTATED:{
				break;
			}
			case MegaUserAlert.TYPE_PAYMENTREMINDER:{
				break;
			}
			case MegaUserAlert.TOTAL_OF_ALERT_TYPES:{
				break;
			}
		}

		holder.titleText.setText(title);
		holder.secondLineText.setText(text);

		String date = TimeChatUtils.formatDateAndTime(alert.getTimestamp(0), TimeChatUtils.DATE_LONG_FORMAT);
		holder.dateText.setText(date);

		if(alert.getSeen()==false){
			holder.newText.setVisibility(View.GONE);
		}
		else{
			holder.newText.setVisibility(View.VISIBLE);
		}


//		holder.imageButtonThreeDots.setTag(holder);
//		holder.imageButtonThreeDots.setOnClickListener(this);
	}

	@Override
    public int getItemCount() {
        return notifications.size();
    }

	public Object getItem(int position) {
		log("getItem");
		return notifications.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		log("setPositionClicked: "+p);
		positionClicked = p;
		notifyDataSetChanged();
	}


	@Override
	public void onClick(View v) {
		log("onClick");

		ViewHolderNotifications holder = (ViewHolderNotifications) v.getTag();
		int currentPosition = holder.getAdapterPosition();
		try {
			MegaContactAdapter c = (MegaContactAdapter) getItem(currentPosition);

			switch (v.getId()){
				case R.id.contact_list_item_layout:
				case R.id.contact_grid_item_layout:{
					log("contact_item_layout");
					if (fragment != null){
						fragment.itemClick(currentPosition);
					}
					break;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			log("EXCEPTION: "+e.getMessage());
		}
	}
	
	public void setNotifications (ArrayList<MegaUserAlert> notifications){
		log("setNotifications");
		this.notifications = notifications;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	private static void log(String log) {
		Util.log("MegaNotificationsAdapter", log);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
