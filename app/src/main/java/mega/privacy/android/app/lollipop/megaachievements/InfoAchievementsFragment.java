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
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		View v = inflater.inflate(R.layout.fragment_info_achievements, container, false);

		icon = v.findViewById(R.id.icon_info_achievements);
		checkIcon = v.findViewById(R.id.icon_achievement_completed);
		title = v.findViewById(R.id.title_info_achievements);
		firstParagraph = v.findViewById(R.id.info_achievements_how_works_first_p);
		secondParagraph = v.findViewById(R.id.info_achievements_how_works_second_p);

		final Bundle arguments = getArguments();
		if (arguments == null) {
			logWarning("Arguments are null. No achievement type.");
			return v;
		}
		achievementType = arguments.getInt("achievementType");

		final AchievementsActivity achievementsActivity = (AchievementsActivity)context;
		if (achievementsActivity.megaAchievements == null) {
			logWarning("MegaAchievementsDetails are null.");
			return v;
		}

		long count = achievementsActivity.megaAchievements.getAwardsCount();
		for(int i=0; i<count; i++) {
			int type = achievementsActivity.megaAchievements.getAwardClass(i);

			if(type == achievementType) {
				awardId = achievementsActivity.megaAchievements.getAwardId(i);

				rewardId = achievementsActivity.megaAchievements.getRewardAwardId(awardId);
				logDebug("AWARD ID: " + awardId + " REWARD id: " + rewardId);

				long daysLeft= achievementsActivity.megaAchievements.getAwardExpirationTs(i);

				Calendar start = calculateDateFromTimestamp(daysLeft);
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
				logWarning("No match for achievement award!");
			}

		}

		if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL){
			aB.setTitle(getString(R.string.title_install_app));
            long installAppStorageValue = achievementsActivity.megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            long installAppTransferValue = achievementsActivity.megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mobile_big));

			if(awardId==-1){
				logWarning("No award for this achievement");
				checkIcon.setVisibility(View.GONE);
				title.setText(getString(R.string.figures_achievements_text, getSizeString(installAppStorageValue), getSizeString(installAppTransferValue)));
				title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
				firstParagraph.setText(getString(R.string.paragraph_info_achievement_install_mobile_app, getSizeString(installAppStorageValue), getSizeString(installAppTransferValue)));
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
					title.setText(context.getResources().getString(R.string.expired_label));
				}

				long storageAppInstall = achievementsActivity.megaAchievements.getRewardStorageByAwardId(awardId);
				long transferAppInstall = achievementsActivity.megaAchievements.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_install_mobile_app, getSizeString(storageAppInstall), getSizeString(transferAppInstall)));
				secondParagraph.setVisibility(View.GONE);
			}
		}else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
            aB.setTitle(getString(R.string.title_add_phone));
            long addPhoneStorageValue = achievementsActivity.megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
            long addPhoneTransferValue = achievementsActivity.megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.il_verify_phone_big));

            if(awardId==-1){
                logDebug("No award for this achievement");
                checkIcon.setVisibility(View.GONE);
                title.setText(getString(R.string.figures_achievements_text, getSizeString(addPhoneStorageValue), getSizeString(addPhoneTransferValue)));
                title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                firstParagraph.setText(getString(R.string.paragraph_info_achievement_add_phone, getSizeString(addPhoneStorageValue), getSizeString(addPhoneTransferValue)));
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
                    title.setText(context.getResources().getString(R.string.expired_label));
                }

                long storageAddPhone = achievementsActivity.megaAchievements.getRewardStorageByAwardId(awardId);
                long transferAddPhone = achievementsActivity.megaAchievements.getRewardTransferByAwardId(awardId);
                firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_add_phone, getSizeString(storageAddPhone), getSizeString(transferAddPhone)));
                secondParagraph.setVisibility(View.GONE);
            }
        }
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){

			aB.setTitle(getString(R.string.title_install_desktop));
            long installDesktopStorageValue = achievementsActivity.megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
            long installDesktopTransferValue = achievementsActivity.megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mega_big));

			if(awardId==-1) {
				logWarning("No award for this achievement");
				checkIcon.setVisibility(View.GONE);
				title.setText(getString(R.string.figures_achievements_text, getSizeString(installDesktopStorageValue), getSizeString(installDesktopTransferValue)));
				title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
				firstParagraph.setText(getString(R.string.paragraph_info_achievement_install_desktop, getSizeString(installDesktopStorageValue), getSizeString(installDesktopTransferValue)));
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
					title.setText(context.getResources().getString(R.string.expired_label));
				}

				long storageDesktopInstall = achievementsActivity.megaAchievements.getRewardStorageByAwardId(awardId);
				long transferDesktopInstall = achievementsActivity.megaAchievements.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_install_desktop, getSizeString(storageDesktopInstall), getSizeString(transferDesktopInstall)));
				secondParagraph.setVisibility(View.GONE);

			}
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
			aB.setTitle(getString(R.string.title_regitration));
			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_registration_big));
//            long registrationStorageValue = achievementsActivity.megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
//            long registrationTransferValue = achievementsActivity.megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);

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
				title.setText(context.getResources().getString(R.string.expired_label));
			}

			long storageRegistration = achievementsActivity.megaAchievements.getRewardStorageByAwardId(awardId);
			firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_registration, getSizeString(storageRegistration)));
			secondParagraph.setVisibility(View.GONE);

		}
//		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE){
//			aB.setTitle(getString(R.string.title_referral_bonuses));
//            long referralsStorageValue = achievementsActivity.megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
//            long referralsTransfeValue = achievementsActivity.megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
//			icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_invite_friends));
//
//            title.setText(getString(R.string.figures_achievements_text, getSizeString(referralsStorageValue), getSizeString(referralsTransfeValue)));
//            firstParagraph.setText(getString(R.string.string_test));
//            secondParagraph.setVisibility(View.GONE);
//        }

		return v;
	}

	@Override
	public void onAttach(Activity activity) {
		logDebug("onAttach");
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onAttach(Context context) {
		logDebug("onAttach context");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
	}
}
