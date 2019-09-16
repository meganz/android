package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class InfoAchievementsFragment extends Fragment{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;
	int height;

	ImageView icon;
	ImageView checkIcon;
	TextView title;
	TextView firstParagraph;
	TextView secondParagraph;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	int achievementType=-1;
	int awardId = -1;
	int rewardId = -1;
	long diffDays;
	int indexAward;

	@Override
	public void onCreate (Bundle savedInstanceState){
		LogUtil.logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LogUtil.logDebug("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		achievementType = getArguments().getInt("achievementType");

		View v = inflater.inflate(R.layout.fragment_info_achievements, container, false);

		icon = (ImageView) v.findViewById(R.id.icon_info_achievements);
		checkIcon = (ImageView) v.findViewById(R.id.icon_achievement_completed);
		title = (TextView) v.findViewById(R.id.title_info_achievements);
		firstParagraph = (TextView) v.findViewById(R.id.info_achievements_how_works_first_p);
		secondParagraph = (TextView) v.findViewById(R.id.info_achievements_how_works_second_p);

		long count = ((AchievementsActivity)context).megaAchievements.getAwardsCount();
		for(int i=0; i<count; i++) {
			int type = ((AchievementsActivity) context).megaAchievements.getAwardClass(i);

			if(type == achievementType) {
				awardId = ((AchievementsActivity) context).megaAchievements.getAwardId(i);

				rewardId = ((AchievementsActivity) context).megaAchievements.getRewardAwardId(awardId);
				LogUtil.logDebug("AWARD ID: " + awardId + " REWARD id: " + rewardId);

				long daysLeft= ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);

				Calendar start = Util.calculateDateFromTimestamp(daysLeft);
				Calendar end = Calendar.getInstance();
				Date startDate = start.getTime();
				Date endDate = end.getTime();
				long startTime = startDate.getTime();
				long endTime = endDate.getTime();
				long diffTime = startTime - endTime;
				diffDays = diffTime / (1000 * 60 * 60 * 24);

				indexAward = i;
				break;
			}
			else{
				LogUtil.logWarning("No match for achievement award!");
			}

		}

		if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL){
			aB.setTitle(getString(R.string.title_install_app));
            long installAppStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            long installAppTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mobile_big));

			if(awardId==-1){
				LogUtil.logWarning("No award for this achievement");
				checkIcon.setVisibility(View.GONE);
				title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(installAppStorageValue), Util.getSizeString(installAppTransferValue)));
				title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
				firstParagraph.setText(getString(R.string.paragraph_info_achievement_install_mobile_app, Util.getSizeString(installAppStorageValue), Util.getSizeString(installAppTransferValue)));
				secondParagraph.setVisibility(View.GONE);
			}
			else{
				if(diffDays<=15){
					title.setTextColor(ContextCompat.getColor(context,R.color.login_title));
					title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
				}
				else{
					title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
				}

				if(diffDays>0){
					title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
				}
				else{
					title.setText(context.getResources().getString(R.string.expired_achievement));
				}

				long storageAppInstall = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				long transferAppInstall = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_install_mobile_app, Util.getSizeString(storageAppInstall), Util.getSizeString(transferAppInstall)));
				secondParagraph.setVisibility(View.GONE);
			}
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){

			aB.setTitle(getString(R.string.title_install_desktop));
            long installDesktopStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
            long installDesktopTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mega_big));

			if(awardId==-1) {
				LogUtil.logWarning("No award for this achievement");
				checkIcon.setVisibility(View.GONE);
				title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(installDesktopStorageValue), Util.getSizeString(installDesktopTransferValue)));
				title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
				firstParagraph.setText(getString(R.string.paragraph_info_achievement_install_desktop, Util.getSizeString(installDesktopStorageValue), Util.getSizeString(installDesktopTransferValue)));
				secondParagraph.setVisibility(View.GONE);
			}
			else{

				if(diffDays<=15){
					title.setTextColor(ContextCompat.getColor(context,R.color.login_title));
					title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
				}
				else{
					title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
				}

				if(diffDays>0){
					title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
				}
				else{
					title.setText(context.getResources().getString(R.string.expired_achievement));
				}

				long storageDesktopInstall = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				long transferDesktopInstall = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_install_desktop, Util.getSizeString(storageDesktopInstall), Util.getSizeString(transferDesktopInstall)));
				secondParagraph.setVisibility(View.GONE);

			}
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
			aB.setTitle(getString(R.string.title_regitration));
			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_registration_big));
//            long registrationStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
//            long registrationTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);

			if(diffDays<=15){
				title.setTextColor(ContextCompat.getColor(context,R.color.login_title));
				title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
			}
			else{
				title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
			}

			if(diffDays>0){
				title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
			}
			else{
				title.setText(context.getResources().getString(R.string.expired_achievement));
			}

			long storageRegistration = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
			firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_registration, Util.getSizeString(storageRegistration)));
			secondParagraph.setVisibility(View.GONE);

		}
//		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE){
//			aB.setTitle(getString(R.string.title_referral_bonuses));
//            long referralsStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
//            long referralsTransfeValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
//			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_invite_friends));
//
//            title.setText(getString(R.string.figures_achievements_text, Util.getSizeString(referralsStorageValue), Util.getSizeString(referralsTransfeValue)));
//            firstParagraph.setText(getString(R.string.string_test));
//            secondParagraph.setVisibility(View.GONE);
//        }

		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		LogUtil.logDebug("onAttach");
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onAttach(Context context) {
		LogUtil.logDebug("onAttach context");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
	}
}
