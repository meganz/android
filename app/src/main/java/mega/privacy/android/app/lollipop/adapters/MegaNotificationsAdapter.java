package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.NotificationsFragmentLollipop;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUserAlert;


public class MegaNotificationsAdapter extends RecyclerView.Adapter<MegaNotificationsAdapter.ViewHolderNotifications> implements OnClickListener{

	public static int MAX_WIDTH_CONTACT_NAME_LAND=270;
	public static int MAX_WIDTH_CONTACT_NAME_PORT=230;

	private Context context;
	private int positionClicked;
	private ArrayList<MegaUserAlert> notifications;
	private RecyclerView listFragment;
	private MegaApiAndroid megaApi;

	DisplayMetrics outMetrics;

	private NotificationsFragmentLollipop fragment;

	public MegaNotificationsAdapter(Context _context, NotificationsFragmentLollipop _fragment, ArrayList<MegaUserAlert> _notifications, RecyclerView _listView) {
		this.context = _context;
		this.notifications = _notifications;
		this.fragment = _fragment;
		this.positionClicked = -1;


		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);

		listFragment = _listView;
	}

	/*private view holder class*/
    public static class ViewHolderNotifications extends RecyclerView.ViewHolder{
    	public ViewHolderNotifications(View v) {
			super(v);
		}

		LinearLayout itemLayout;

		ImageView sectionIcon;
    	TextView sectionText;

    	ImageView titleIcon;
    	TextView titleText;
    	TextView newText;

    	TextView descriptionText;
    	TextView dateText;

    	LinearLayout separator;
    }

	ViewHolderNotifications holder = null;

	@Override
	public ViewHolderNotifications onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_list, parent, false);

		holder = new ViewHolderNotifications(v);

		holder.itemLayout = (LinearLayout) v.findViewById(R.id.notification_list_item_layout);

		holder.sectionIcon = (ImageView) v.findViewById(R.id.notification_title_icon);
		holder.sectionText = (TextView) v.findViewById(R.id.notification_title_text);

		holder.titleIcon = (ImageView) v.findViewById(R.id.notification_first_line_icon);
		holder.titleText = (TextView) v.findViewById(R.id.notification_first_line_text);
		holder.newText = (TextView) v.findViewById(R.id.notification_new_label);

		holder.descriptionText = (TextView) v.findViewById(R.id.notifications_text);
		holder.dateText = (TextView) v.findViewById(R.id.notifications_date);

		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			log("onCreate: Landscape configuration");
			holder.titleText.setMaxWidth(Util.scaleWidthPx(MAX_WIDTH_CONTACT_NAME_LAND, outMetrics));
		}
		else{
			holder.titleText.setMaxWidth(Util.scaleWidthPx(MAX_WIDTH_CONTACT_NAME_PORT, outMetrics));
		}

		holder.separator = (LinearLayout) v.findViewById(R.id.notifications_separator);

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

		String section = alert.getHeading();
		log("****" + alert.getHeading()+ " " +alert.getTypeString() + " " + alert.getTitle() + " "+alert.getString(0));
		log("****"+ alert.getTypeString() + ": " + alert.getNodeHandle() + " " + alert.getPath());

		String description = alert.getTitle();

		switch (alertType){

			case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);

				holder.titleText.setText(context.getString(R.string.title_contact_request_notification));

				String email = alert.getEmail();
				description = context.getString(R.string.notification_new_contact_request, email);

				break;
			}
			case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				holder.titleText.setText(context.getString(R.string.title_acceptance_contact_request_notification));

				String email = alert.getEmail();
				description = context.getString(R.string.notification_new_contact, email);
				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED:{
				//android100@yopmail.com
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);

				holder.titleText.setText(alert.getEmail());

				break;
			}
			case MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU:{
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED:{
				section = context.getString(R.string.section_contacts).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.green_notif_contacts));
				holder.sectionIcon.setVisibility(View.GONE);
				holder.titleIcon.setVisibility(View.GONE);
				break;
			}
			case MegaUserAlert.TYPE_NEWSHARE:{
				section = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_y_arrow_in));
				holder.sectionIcon.setVisibility(View.VISIBLE);
				holder.titleIcon.setVisibility(View.VISIBLE);
				holder.titleText.setText("Folder name");

				String email = alert.getEmail();
				description = context.getString(R.string.notification_new_shared_folder, email);

				break;
			}
			case MegaUserAlert.TYPE_DELETEDSHARE:{
				section = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_y_arrow_in));
				holder.sectionIcon.setVisibility(View.VISIBLE);
				holder.titleIcon.setVisibility(View.VISIBLE);

				break;
			}
			case MegaUserAlert.TYPE_NEWSHAREDNODES:{
				section = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_y_arrow_in));
				holder.sectionIcon.setVisibility(View.VISIBLE);
				holder.titleIcon.setVisibility(View.VISIBLE);

				break;
			}
			case MegaUserAlert.TYPE_REMOVEDSHAREDNODES:{
				section = context.getString(R.string.title_incoming_shares_explorer).toUpperCase();
				holder.sectionText.setTextColor(ContextCompat.getColor(context, R.color.yellow_notif_shares));
				holder.sectionIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_y_arrow_in));
				holder.sectionIcon.setVisibility(View.VISIBLE);
				holder.titleIcon.setVisibility(View.VISIBLE);

				break;
			}
			case MegaUserAlert.TYPE_PAYMENT_SUCCEEDED:{
				section = alert.getHeading();

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

		holder.sectionText.setText(section);
		holder.descriptionText.setText(description);

		String date = TimeChatUtils.formatDateAndTime(alert.getTimestamp(0), TimeChatUtils.DATE_LONG_FORMAT);
		holder.dateText.setText(date);

		if(alert.getSeen()==false){
			holder.newText.setVisibility(View.VISIBLE);
			holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));

			if(position<(notifications.size()-1)){
				MegaUserAlert nextAlert = (MegaUserAlert) getItem(position+1);
				if(nextAlert.getSeen()==false){
					LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)holder.separator.getLayoutParams();
					textParams.setMargins(Util.scaleWidthPx(16, outMetrics), 0, Util.scaleWidthPx(16, outMetrics), 0);
					holder.separator.setLayoutParams(textParams);
				}
				else{
					LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)holder.separator.getLayoutParams();
					textParams.setMargins(0, 0, 0, 0);
					holder.separator.setLayoutParams(textParams);
				}
			}
			else{
				log("Last element of the notifications");
				LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)holder.separator.getLayoutParams();
				textParams.setMargins(0, 0, 0, 0);
				holder.separator.setLayoutParams(textParams);
			}
		}
		else{
			holder.newText.setVisibility(View.GONE);
			holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.color_background_new_messages));

			LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)holder.separator.getLayoutParams();
			textParams.setMargins(Util.scaleWidthPx(16, outMetrics), 0, Util.scaleWidthPx(16, outMetrics), 0);
			holder.separator.setLayoutParams(textParams);
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
