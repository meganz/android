package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class InfoAchievementsFragment extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;
	int height;

	ImageView icon;
	TextView title;
	TextView firstParagraph;
	TextView secondParagraph;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	int achievementType=-1;

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		achievementType = getArguments().getInt("achievementType");

		View v = inflater.inflate(R.layout.fragment_info_achievements, container, false);

		icon = (ImageView) v.findViewById(R.id.icon_info_achievements);
		title = (TextView) v.findViewById(R.id.title_info_achievements);
		firstParagraph = (TextView) v.findViewById(R.id.info_achievements_how_works_first_p);
		secondParagraph = (TextView) v.findViewById(R.id.info_achievements_how_works_second_p);


		if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL){
			aB.setTitle(getString(R.string.title_install_app));
            long installAppStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            long installAppTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);

            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mobile_big));
			title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(installAppStorageValue), Util.getSizeString(installAppTransferValue)));
			firstParagraph.setText(getString(R.string.string_test));
			secondParagraph.setVisibility(View.GONE);
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){

			aB.setTitle(getString(R.string.title_install_desktop));
            long installDesktopStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
            long installDesktopTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);

			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mega_big));
			title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(installDesktopStorageValue), Util.getSizeString(installDesktopTransferValue)));
			firstParagraph.setText(getString(R.string.string_test));
			secondParagraph.setVisibility(View.GONE);
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
			aB.setTitle(getString(R.string.title_regitration));
            long registrationStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
            long registrationTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);

			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_registration_big));
			title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(registrationStorageValue), Util.getSizeString(registrationTransferValue)));
			firstParagraph.setText(getString(R.string.string_test));
			secondParagraph.setVisibility(View.GONE);
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE){
			aB.setTitle(getString(R.string.title_referral_bonuses));
            long referralsStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
            long referralsTransfeValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_invite_friends));

            title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(referralsStorageValue), Util.getSizeString(referralsTransfeValue)));
            firstParagraph.setText(getString(R.string.string_test));
            secondParagraph.setVisibility(View.GONE);
        }

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

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
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch (v.getId()) {

		}
	}

	public static void log(String log) {
		Util.log("InviteFriendsFragment", log);
	}
}
