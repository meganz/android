package mega.privacy.android.app.lollipop.megaachievements;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Date;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaAchievementsDetails;

import static mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity.sFetcher;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Util.calculateDateFromTimestamp;
import static mega.privacy.android.app.utils.Util.getSizeString;

public class InfoAchievementsFragment extends Fragment implements AchievementsFetcher.DataCallback {
	ActionBar actionBar;

	ImageView icon;
	ImageView checkIcon;
	TextView title;
	TextView firstParagraph;
	TextView secondParagraph;

	int achievementType=-1;
	int awardId = -1;
	int rewardId = -1;
	long diffDays;
	int indexAward;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

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

		return v;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// The root view has been created, fill it with the data when data ready
		if (sFetcher != null) {
			sFetcher.setDataCallback(this);
		}

		updateBarTitle();
	}

	private void updateBarTitle() {
		actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
		if (actionBar == null) return;
		String title = "";

		switch (achievementType) {
			case MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL:
				title = getString(R.string.title_install_app);
				break;
			case MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE:
				title = getString(R.string.title_add_phone);
				break;
			case MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL:
				title = getString(R.string.title_install_desktop);
				break;
			case MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME:
				title = getString(R.string.title_regitration);
				break;
			default:
				break;
		}

		actionBar.setTitle(title);
	}

	private void updateUI() {
		if (sFetcher == null) return;
		MegaAchievementsDetails details = sFetcher.getAchievementsDetails();
		Context context = getContext();

		if (details == null || context == null) return;

		long count = details.getAwardsCount();
		for(int i=0; i<count; i++) {
			int type = details.getAwardClass(i);

			if(type == achievementType) {
				awardId = details.getAwardId(i);

				rewardId = details.getRewardAwardId(awardId);
				logDebug("AWARD ID: " + awardId + " REWARD id: " + rewardId);

				long daysLeft= details.getAwardExpirationTs(i);

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
			long installAppStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
			long installAppTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
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

				long storageAppInstall = details.getRewardStorageByAwardId(awardId);
				long transferAppInstall = details.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_install_mobile_app, getSizeString(storageAppInstall), getSizeString(transferAppInstall)));
				secondParagraph.setVisibility(View.GONE);
			}
		}else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
			long addPhoneStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
			long addPhoneTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
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

				long storageAddPhone = details.getRewardStorageByAwardId(awardId);
				long transferAddPhone = details.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_add_phone, getSizeString(storageAddPhone), getSizeString(transferAddPhone)));
				secondParagraph.setVisibility(View.GONE);
			}
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){
			long installDesktopStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
			long installDesktopTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
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

				long storageDesktopInstall = details.getRewardStorageByAwardId(awardId);
				long transferDesktopInstall = details.getRewardTransferByAwardId(awardId);
				firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_install_desktop, getSizeString(storageDesktopInstall), getSizeString(transferDesktopInstall)));
				secondParagraph.setVisibility(View.GONE);

			}
		}
		else if(achievementType== MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
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

			long storageRegistration = details.getRewardStorageByAwardId(awardId);
			firstParagraph.setText(getString(R.string.result_paragraph_info_achievement_registration, getSizeString(storageRegistration)));
			secondParagraph.setVisibility(View.GONE);
		}
	}

	@Override
	public void onAchievementsReceived() {
		updateUI();
	}
}
