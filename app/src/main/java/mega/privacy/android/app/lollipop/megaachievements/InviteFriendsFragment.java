package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;

public class InviteFriendsFragment extends Fragment implements OnClickListener{

	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;
	int height;

	Button inviteContactsBtn;

	TextView titleCard;

	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		height = outMetrics.heightPixels;
		int width = outMetrics.widthPixels;

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		log("The achievements are: "+enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_invite_friends, container, false);

		titleCard = (TextView) v.findViewById(R.id.title_card_invite_fragment);

		long referralsStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long referralsTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);

		titleCard.setText(getString(R.string.figures_achievements_text_referrals, Util.getSizeString(referralsStorageValue), Util.getSizeString(referralsTransferValue)));
        inviteContactsBtn = v.findViewById(R.id.invite_contacts_button);
        inviteContactsBtn.setOnClickListener(this);
		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		log("onAttach");
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onAttach(Context context) {
		log("onAttach context");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
	}

	@Override
	public void onClick(View v) {
		log("onClick");
		switch (v.getId()) {
			case R.id.invite_contacts_button: {
				Intent intent = new Intent(context, AddContactActivityLollipop.class);
				intent.putExtra("contactType", Constants.CONTACT_TYPE_DEVICE);
				intent.putExtra("fromAchievements", true);
				((AchievementsActivity)context).startActivityForResult(intent, Constants.REQUEST_CODE_GET_CONTACTS);
				break;
			}
		}
	}

	public int onBackPressed(){
		log("onBackPressed");

		((AchievementsActivity) context).showFragment(Constants.ACHIEVEMENTS_FRAGMENT, -1);
		return 0;
	}

	public static void log(String log) {
		Util.log("InviteFriendsFragment", log);
	}

}
