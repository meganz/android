package mega.privacy.android.app.lollipop.megaachievements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;

import static mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity.INVALID_TYPE;
import static mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity.sFetcher;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.*;

public class AchievementsFragment extends BaseFragment implements OnClickListener
		, GetAchievementsListener.DataCallback {
	private LinearLayout parentLinearLayout;

	private RelativeLayout referralBonusesLayout;

	private RelativeLayout addPhoneLayout;
	private RelativeLayout registrationLayout;
	private RelativeLayout installAppLayout;
	private RelativeLayout installDesktopLayout;

	private LinearLayout figuresInstallAppLayout;
	private TextView zeroFiguresInstallAppText;
	private long transferQuota = 0;
	private TextView textUnlockedRewardTransfer;

	private LinearLayout figuresReferralBonusesLayout;
	private TextView zeroFiguresReferralBonusesText;

	private LinearLayout figuresRegistrationLayout;

	private LinearLayout figuresInstallDesktopLayout;
	private TextView zeroFiguresInstallDesktopText;

	private LinearLayout figuresAddPhoneLayout;
	private TextView zeroFiguresAddPhoneText;

	private ImageView installAppIcon;
	private ImageView installDesktopIcon;
	private ImageView registrationIcon;
	private ImageView addPhoneIcon;
	private ImageView referralBonusIcon;

	private TextView figureUnlockedRewardStorage, figureUnlockedRewardStorageUnit;
	private long storageQuota = 0;
	private TextView figureUnlockedRewardTransfer, figureUnlockedRewardTransferUnit;

	private TextView titleReferralBonuses;
	private TextView figureReferralBonusesStorage;
	private TextView figureReferralBonusesTransfer;
	private long storageReferrals;
	private long transferReferrals;
	private TextView textReferralBonusesStorage;
	private TextView textReferralBonusesTransfer;

	private TextView figureBaseQuotaStorage;
	private TextView figureBaseQuotaTransfer;
	private TextView textBaseQuotaStorage;
	private TextView textBaseQuotaTransfer;

	private TextView titleInstallApp;
	private TextView figureInstallAppStorage;
	private TextView figureInstallAppTransfer;
	private long storageInstallApp;
	private long transferInstallApp;
	private long daysLeftInstallApp;

	private TextView textInstallAppStorage;
	private TextView textInstallAppTransfer;
	private TextView daysLeftInstallAppText;

	private TextView titleAddPhone;
	private TextView figureAddPhoneStorage;
	private TextView figureAddPhoneTransfer;
	private long storageAddPhone;
	private long transferAddPhone;
	private long daysLeftAddPhone;

	private TextView textAddPhoneStorage;
	private TextView textAddPhoneTransfer;
	private TextView daysLeftAddPhoneText;

	private TextView titleRegistration;
	private TextView figureRegistrationStorage;
	private TextView figureRegistrationTransfer;
	private long storageRegistration;
	private long transferRegistration;
	private long daysLeftRegistration;

	private TextView textRegistrationStorage;
	private TextView textRegistrationTransfer;
	private TextView daysLeftRegistrationText;

	private TextView titleInstallDesktop;
	private TextView figureInstallDesktopStorage;
	private TextView figureInstallDesktopTransfer;
	private long storageInstallDesktop;
	private long transferInstallDesktop;
	private long daysLeftInstallDesktop;

	private TextView textInstallDesktopStorage;
	private TextView textInstallDesktopTransfer;
	private TextView daysLeftInstallDesktopText;

	private Button inviteFriendsButton;

	private AchievementsActivity mActivity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		mActivity = (AchievementsActivity)super.mActivity;
		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		logDebug("The achievements are: " + enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_achievements, container, false);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.main_linear_layout_achievements);

		inviteFriendsButton = (Button) v.findViewById(R.id.invite_button);
		inviteFriendsButton.setOnClickListener(this);

		referralBonusesLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_layout);
		referralBonusesLayout.setOnClickListener(this);

		titleReferralBonuses = (TextView) v.findViewById(R.id.title_referral_bonuses);

		boolean isPortrait = Util.isScreenInPortrait(MegaApplication.getInstance());

		titleReferralBonuses.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, outMetrics));

		figuresReferralBonusesLayout = (LinearLayout) v.findViewById(R.id.figures_referral_bonuses_layout);
		figuresReferralBonusesLayout.setVisibility(View.GONE);

		zeroFiguresReferralBonusesText = (TextView) v.findViewById(R.id.zero_figures_referral_bonuses_text);

        registrationLayout = (RelativeLayout) v.findViewById(R.id.registration_layout);
        registrationLayout.setOnClickListener(this);

		titleRegistration = (TextView) v.findViewById(R.id.title_registration);
		titleRegistration.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, outMetrics));

		figuresRegistrationLayout = (LinearLayout) v.findViewById(R.id.figures_registration_layout);

        installAppLayout = (RelativeLayout) v.findViewById(R.id.install_app_layout);
        installAppLayout.setOnClickListener(this);

		titleInstallApp = (TextView) v.findViewById(R.id.title_install_app);
		titleInstallApp.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, outMetrics));

		figuresInstallAppLayout = (LinearLayout) v.findViewById(R.id.figures_install_app_layout);
		figuresInstallAppLayout.setVisibility(View.GONE);
		zeroFiguresInstallAppText = (TextView) v.findViewById(R.id.zero_figures_install_app_text);

        addPhoneLayout = v.findViewById(R.id.add_phone_layout);
        if(megaApi.smsAllowedState() == 2) {
            addPhoneLayout.setOnClickListener(this);
            titleAddPhone = v.findViewById(R.id.title_add_phone);
			titleAddPhone.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, outMetrics));
        } else {
            v.findViewById(R.id.separator_add_phone).setVisibility(View.GONE);
            addPhoneLayout.setVisibility(View.GONE);
        }
        figuresAddPhoneLayout = v.findViewById(R.id.figures_add_phone_layout);
        figuresAddPhoneLayout.setVisibility(View.GONE);
        zeroFiguresAddPhoneText = v.findViewById(R.id.zero_figures_add_phone_text);

        installDesktopLayout = (RelativeLayout) v.findViewById(R.id.install_desktop_layout);
        installDesktopLayout.setOnClickListener(this);

		titleInstallDesktop = (TextView) v.findViewById(R.id.title_install_desktop);
		titleInstallDesktop.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, outMetrics));

		figuresInstallDesktopLayout = (LinearLayout) v.findViewById(R.id.figures_install_desktop_layout);
		figuresInstallDesktopLayout.setVisibility(View.GONE);

		zeroFiguresInstallDesktopText = (TextView) v.findViewById(R.id.zero_figures_install_desktop_text);

		installAppIcon = (ImageView) v.findViewById(R.id.install_app_icon);
		addPhoneIcon = v.findViewById(R.id.add_phone_icon);
		installDesktopIcon = (ImageView) v.findViewById(R.id.install_desktop_icon);
		registrationIcon = (ImageView) v.findViewById(R.id.registration_icon);
		referralBonusIcon = (ImageView) v.findViewById(R.id.referral_bonuses_icon);

		String transferQuotaString = getString(R.string.label_transfer_quota_achievements);
		transferQuotaString = transferQuotaString.toLowerCase(Locale.getDefault());

		String storageQuotaString = getString(R.string.unlocked_storage_title);
		storageQuotaString = storageQuotaString.toLowerCase(Locale.getDefault());

		figureUnlockedRewardStorage = (TextView) v.findViewById(R.id.unlocked_storage_text);
		figureUnlockedRewardStorageUnit = v.findViewById(R.id.unlocked_storage_unit);
		figureUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_text);
        figureUnlockedRewardTransferUnit = v.findViewById(R.id.unlocked_transfer_unit);

		figureUnlockedRewardStorage.setText(getSizeString(0));
		figureUnlockedRewardTransfer.setText(getSizeString(0));

		textUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title);

		figureReferralBonusesStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_referral);
		figureReferralBonusesTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_referral);

		figureReferralBonusesStorage.setText(getSizeString(0));
		figureReferralBonusesTransfer.setText(getSizeString(0));

		textReferralBonusesStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_referral);
		textReferralBonusesStorage.setText(storageQuotaString);
		textReferralBonusesTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_referral);
		textReferralBonusesTransfer.setText(transferQuotaString);

		figureBaseQuotaStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_base_quota);
		figureBaseQuotaTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_base_quota);

		figureBaseQuotaStorage.setText(getSizeString(0));
		figureBaseQuotaTransfer.setText(getSizeString(0));
		figureBaseQuotaTransfer.setVisibility(View.INVISIBLE);

		textBaseQuotaStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_base_quota);
		textBaseQuotaStorage.setText(storageQuotaString);
		textBaseQuotaTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_base_quota);
		textBaseQuotaTransfer.setText(transferQuotaString);
		textBaseQuotaTransfer.setVisibility(View.INVISIBLE);

		figureInstallAppStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_app);
		figureInstallAppTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_install_app);

		figureInstallAppStorage.setText(getSizeString(0));
		figureInstallAppTransfer.setText(getSizeString(0));

		textInstallAppStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_app);
		textInstallAppStorage.setText(storageQuotaString);
		textInstallAppTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_install_app);
		textInstallAppTransfer.setText(transferQuotaString);
		daysLeftInstallAppText = (TextView) v.findViewById(R.id.days_left_text_install_app);
		daysLeftInstallAppText.setText(("..."));

        figureAddPhoneStorage = v.findViewById(R.id.figure_unlocked_storage_text_add_phone);
        figureAddPhoneTransfer = v.findViewById(R.id.figure_unlocked_transfer_text_add_phone);

        figureAddPhoneStorage.setText(getSizeString(0));
        figureAddPhoneTransfer.setText(getSizeString(0));

        textAddPhoneStorage = v.findViewById(R.id.unlocked_storage_title_add_phone);
        textAddPhoneStorage.setText(storageQuotaString);
        textAddPhoneTransfer = v.findViewById(R.id.unlocked_transfer_title_add_phone);
        textAddPhoneTransfer.setText(transferQuotaString);
        daysLeftAddPhoneText = v.findViewById(R.id.days_left_text_add_phone);
        daysLeftAddPhoneText.setText(("..."));

		figureRegistrationStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_registration);
		figureRegistrationTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_registration);

		figureRegistrationStorage.setText(getSizeString(0));
		figureRegistrationTransfer.setText(getSizeString(0));

		textRegistrationStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_registration);
		textRegistrationStorage.setText(storageQuotaString);
		textRegistrationTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_registration);
		textRegistrationTransfer.setText(transferQuotaString);
		daysLeftRegistrationText = (TextView) v.findViewById(R.id.days_left_text_registration);
		daysLeftRegistrationText.setText(("..."));

		figureInstallDesktopStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_desktop);
		figureInstallDesktopTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_install_desktop);

		figureInstallDesktopStorage.setText(getSizeString(0));
		figureInstallDesktopTransfer.setText(getSizeString(0));

		textInstallDesktopStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_desktop);
		textInstallDesktopStorage.setText(storageQuotaString);
		textInstallDesktopTransfer= (TextView) v.findViewById(R.id.unlocked_transfer_title_install_desktop);
		textInstallDesktopTransfer.setText(transferQuotaString);
		daysLeftInstallDesktopText = (TextView) v.findViewById(R.id.days_left_text_install_desktop);
		daysLeftInstallDesktopText.setText(("..."));

		daysLeftInstallDesktopText.setVisibility(View.INVISIBLE);
		daysLeftInstallAppText.setVisibility(View.INVISIBLE);
		daysLeftAddPhoneText.setVisibility(View.INVISIBLE);

		figureUnlockedRewardStorage.setText("...");

		figureUnlockedRewardTransfer.setText("...");

		return v;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mActivity != null) {
			ActionBar actionBar = mActivity.getSupportActionBar();
			if (actionBar != null) {
				actionBar.setTitle(getString(R.string.achievements_title));
			}
		}

		// The root view has been created, fill it with the data when data ready
		if (sFetcher != null) {
			sFetcher.setDataCallback(this);
		}
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		switch (v.getId()) {
			case R.id.referral_bonuses_layout:{
				logDebug("Go to section Referral bonuses");
				mActivity.showFragment((transferReferrals > 0 || storageReferrals > 0)
						? BONUSES_FRAGMENT : INVITE_FRIENDS_FRAGMENT, INVALID_TYPE);
				break;
			}
			case R.id.install_app_layout:{
				logDebug("Go to info app install");
				mActivity.showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
				break;
			}
            case R.id.add_phone_layout:{
                logDebug("Go to info add phone");
				mActivity.showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
                break;
            }
			case R.id.registration_layout:{
				logDebug("Go to info registration");
				mActivity.showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
				break;
			}
			case R.id.install_desktop_layout:{
				logDebug("Go to info desktop install");
				mActivity.showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
				break;
			}
			case R.id.invite_button:{
				logDebug("Invite friends");
				mActivity.showFragment(INVITE_FRIENDS_FRAGMENT, -1);
				break;
			}
		}
	}

	private void updateUI(){
		logDebug("updateValues");
		if (sFetcher == null) return;

		MegaAchievementsDetails details = sFetcher.getAchievementsDetails();
		ArrayList<ReferralBonus> bonuses = sFetcher.getReferralBonuses();

		if(details == null || context == null || bonuses == null) {
			return;
		}

		long totalStorage = 0;
		long totalTransfer = 0;

		storageReferrals = details.currentStorageReferrals();
		totalStorage = totalStorage + storageReferrals;
		transferReferrals = details.currentTransferReferrals();
		totalTransfer = totalTransfer + transferReferrals;

		logDebug("After referrals: storage: " + getSizeString(totalStorage) + " transfer " + getSizeString(totalTransfer));

		long referralsStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long referralsTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long installAppStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
		long installAppTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
        long addPhoneStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
        long addPhoneTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
		long installDesktopStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
		long installDesktopTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);

		if(transferReferrals>0||storageReferrals>0){

			figureReferralBonusesTransfer.setText(getSizeString(transferReferrals));
			figureReferralBonusesStorage.setText(getSizeString(storageReferrals));

			figuresReferralBonusesLayout.setVisibility(View.VISIBLE);
			zeroFiguresReferralBonusesText.setVisibility(View.GONE);

			logDebug("Check if referrals are expired");
			int expiredNumber = 0;

			for (int i = 0; i < bonuses.size(); i++) {
				ReferralBonus referralBonus = bonuses.get(i);
				if (referralBonus.getDaysLeft() < 0) {
					expiredNumber++;
				}
			}

			if(expiredNumber>=bonuses.size()-1){
				logDebug("All the referrals are expired");
				figuresReferralBonusesLayout.setAlpha(0.5f);
				referralBonusIcon.setAlpha(0.5f);
			}

		}
		else{
			figuresReferralBonusesLayout.setVisibility(View.GONE);
			zeroFiguresReferralBonusesText.setText(getString(R.string.figures_achievements_text_referrals, getSizeString(referralsStorageValue), getSizeString(referralsTransferValue)));
			zeroFiguresReferralBonusesText.setVisibility(View.VISIBLE);
		}

		zeroFiguresInstallAppText.setText(getString(R.string.figures_achievements_text, getSizeString(installAppStorageValue), getSizeString(installAppTransferValue)));
		zeroFiguresAddPhoneText.setText(getString(R.string.figures_achievements_text, getSizeString(addPhoneStorageValue), getSizeString(addPhoneTransferValue)));
		zeroFiguresInstallDesktopText.setText(getString(R.string.figures_achievements_text, getSizeString(installDesktopStorageValue), getSizeString(installDesktopTransferValue)));

		long count = details.getAwardsCount();

		for(int i=0; i<count; i++){
			int type = details.getAwardClass(i);

			int awardId = details.getAwardId(i);

			int rewardId = details.getRewardAwardId(awardId);
			logDebug("AWARD ID: "+awardId+" REWARD id: "+rewardId);
			logDebug("type: " + type + " AWARD ID: "+awardId+" REWARD id: "+rewardId);

			if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL){
				logDebug("MEGA_ACHIEVEMENT_MOBILE_INSTALL");

				figuresInstallAppLayout.setVisibility(View.VISIBLE);
				zeroFiguresInstallAppText.setVisibility(View.GONE);

				storageInstallApp = details.getRewardStorageByAwardId(awardId);
				if(storageInstallApp>0){
					figureInstallAppStorage.setText(getSizeString(storageInstallApp));
					figureInstallAppStorage.setVisibility(View.VISIBLE);
					textInstallAppStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallAppStorage.setVisibility(View.INVISIBLE);
					textInstallAppStorage.setVisibility(View.INVISIBLE);
				}

				transferInstallApp = details.getRewardTransferByAwardId(awardId);
				if(transferInstallApp>0){
					figureInstallAppTransfer.setText(getSizeString(transferInstallApp));
					figureInstallAppTransfer.setVisibility(View.VISIBLE);
					textInstallAppTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallAppTransfer.setVisibility(View.INVISIBLE);
					textInstallAppTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftInstallAppText.setVisibility(View.VISIBLE);
				daysLeftInstallApp = details.getAwardExpirationTs(i);
				logDebug("Install App AwardExpirationTs: " + daysLeftInstallApp);

				Calendar start = calculateDateFromTimestamp(daysLeftInstallApp);
				Calendar end = Calendar.getInstance();
				Date startDate = start.getTime();
				Date endDate = end.getTime();
				long startTime = startDate.getTime();
				long endTime = endDate.getTime();
				long diffTime = startTime - endTime;
				long diffDays = diffTime / (1000 * 60 * 60 * 24);

				if(diffDays<=15){
					daysLeftInstallAppText.setTextColor(ContextCompat.getColor(context,R.color.login_title));
				}

				if(diffDays>0){
					daysLeftInstallAppText.setText(context.getResources().getString(R.string.general_num_days_left, (int)diffDays));
					totalStorage = totalStorage + storageInstallApp;
					totalTransfer = totalTransfer + transferInstallApp;
					logDebug("After mobile install: storage: " + getSizeString(totalStorage) + " transfer " + getSizeString(totalTransfer));
				}
				else{
					daysLeftInstallAppText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
					figuresInstallAppLayout.setAlpha(0.5f);
					installAppIcon.setAlpha(0.5f);
					daysLeftInstallAppText.setPadding(scaleWidthPx(8,outMetrics), scaleHeightPx(4,outMetrics),scaleWidthPx(8,outMetrics),scaleHeightPx(4,outMetrics));
					daysLeftInstallAppText.setText(context.getResources().getString(R.string.expired_label));
				}

			}else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
                logDebug("MEGA_ACHIEVEMENT_ADD_PHONE");

                figuresAddPhoneLayout.setVisibility(View.VISIBLE);
                zeroFiguresAddPhoneText.setVisibility(View.GONE);

                storageAddPhone = details.getRewardStorageByAwardId(awardId);
                if(storageAddPhone>0){
                    figureAddPhoneStorage.setText(getSizeString(storageAddPhone));
                    figureAddPhoneStorage.setVisibility(View.VISIBLE);
                    textAddPhoneStorage.setVisibility(View.VISIBLE);
                }
                else{
                    figureAddPhoneStorage.setVisibility(View.INVISIBLE);
                    textAddPhoneStorage.setVisibility(View.INVISIBLE);
                }

                transferAddPhone = details.getRewardTransferByAwardId(awardId);
                if(transferAddPhone>0){
                    figureAddPhoneTransfer.setText(getSizeString(transferAddPhone));
                    figureAddPhoneTransfer.setVisibility(View.VISIBLE);
                    textAddPhoneTransfer.setVisibility(View.VISIBLE);
                }
                else{
                    figureAddPhoneTransfer.setVisibility(View.INVISIBLE);
                    textAddPhoneTransfer.setVisibility(View.INVISIBLE);
                }

                daysLeftAddPhoneText.setVisibility(View.VISIBLE);
                daysLeftAddPhone = details.getAwardExpirationTs(i);
                logDebug("Add phone AwardExpirationTs: "+daysLeftAddPhone);

                Calendar start = calculateDateFromTimestamp(daysLeftAddPhone);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                if(diffDays<=15){
                    daysLeftAddPhoneText.setTextColor(ContextCompat.getColor(context,R.color.login_title));
                }

                if(diffDays>0){
                    daysLeftAddPhoneText.setText(context.getResources().getString(R.string.general_num_days_left, (int)diffDays));
                    totalStorage = totalStorage + storageAddPhone;
                    totalTransfer = totalTransfer + transferAddPhone;
                    logDebug("After phone added: storage: "+getSizeString(totalStorage)+" transfer "+getSizeString(totalTransfer));
                }
                else{
                    daysLeftAddPhoneText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
                    figuresAddPhoneLayout.setAlpha(0.5f);
                    addPhoneIcon.setAlpha(0.5f);
                    daysLeftAddPhoneText.setPadding(scaleWidthPx(8,outMetrics), scaleHeightPx(4,outMetrics),scaleWidthPx(8,outMetrics),scaleHeightPx(4,outMetrics));
                    daysLeftAddPhoneText.setText(context.getResources().getString(R.string.expired_label));
                }
            }
			else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){
				logDebug("MEGA_ACHIEVEMENT_DESKTOP_INSTALL");

				figuresInstallDesktopLayout.setVisibility(View.VISIBLE);
				zeroFiguresInstallDesktopText.setVisibility(View.GONE);

				storageInstallDesktop = details.getRewardStorageByAwardId(awardId);
				if(storageInstallDesktop>0){
					figureInstallDesktopStorage.setText(getSizeString(storageInstallDesktop));
					textInstallDesktopStorage.setVisibility(View.VISIBLE);
					textInstallDesktopStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallDesktopStorage.setVisibility(View.INVISIBLE);
					textInstallDesktopStorage.setVisibility(View.INVISIBLE);
				}

				transferInstallDesktop = details.getRewardTransferByAwardId(awardId);
				if(transferInstallDesktop>0){
					figureInstallDesktopTransfer.setText(getSizeString(transferInstallDesktop));
					figureInstallDesktopTransfer.setVisibility(View.VISIBLE);
					textInstallDesktopTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallDesktopTransfer.setVisibility(View.INVISIBLE);
					textInstallDesktopTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftInstallDesktopText.setVisibility(View.VISIBLE);
				daysLeftInstallDesktop = details.getAwardExpirationTs(i);
				logDebug("Install Desktop AwardExpirationTs: " + daysLeftInstallDesktop);

				Calendar start = calculateDateFromTimestamp(daysLeftInstallDesktop);
				Calendar end = Calendar.getInstance();
				Date startDate = start.getTime();
				Date endDate = end.getTime();
				long startTime = startDate.getTime();
				long endTime = endDate.getTime();
				long diffTime = startTime - endTime;
				long diffDays = diffTime / (1000 * 60 * 60 * 24);

				if(diffDays<=15){
					daysLeftInstallDesktopText.setTextColor(ContextCompat.getColor(context,R.color.login_title));
				}

				if(diffDays>0){
					daysLeftInstallDesktopText.setText(context.getResources().getString(R.string.general_num_days_left, (int)diffDays));
					totalStorage = totalStorage + storageInstallDesktop;
					totalTransfer = totalTransfer + transferInstallDesktop;
					logDebug("After desktop install: storage: " + getSizeString(totalStorage) + " transfer " + getSizeString(totalTransfer));
				}
				else{
                    daysLeftInstallDesktopText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
					figuresInstallDesktopLayout.setAlpha(0.5f);
					installDesktopIcon.setAlpha(0.5f);
					daysLeftInstallDesktopText.setPadding(scaleWidthPx(8,outMetrics), scaleHeightPx(4,outMetrics),scaleWidthPx(8,outMetrics),scaleHeightPx(4,outMetrics));
					daysLeftInstallDesktopText.setText(context.getResources().getString(R.string.expired_label));
				}

			}
			else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
				logDebug("MEGA_ACHIEVEMENT_WELCOME");
				storageRegistration = details.getRewardStorageByAwardId(awardId);
				if(storageRegistration>0){
					figureRegistrationStorage.setText(getSizeString(storageRegistration));
					figureRegistrationStorage.setVisibility(View.VISIBLE);
					textRegistrationStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureRegistrationStorage.setVisibility(View.INVISIBLE);
					textRegistrationStorage.setVisibility(View.INVISIBLE);
				}

				transferRegistration= details.getRewardTransferByAwardId(awardId);
				if(transferRegistration>0){
					figureRegistrationTransfer.setText(getSizeString(transferRegistration));
					figureRegistrationTransfer.setVisibility(View.VISIBLE);
					textRegistrationTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureRegistrationTransfer.setVisibility(View.INVISIBLE);
					textRegistrationTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftRegistration= details.getAwardExpirationTs(i);
				logDebug("Registration AwardExpirationTs: " + daysLeftRegistration);

				Calendar start = calculateDateFromTimestamp(daysLeftRegistration);
				Calendar end = Calendar.getInstance();
				Date startDate = start.getTime();
				Date endDate = end.getTime();
				long startTime = startDate.getTime();
				long endTime = endDate.getTime();
				long diffTime = startTime - endTime;
				long diffDays = diffTime / (1000 * 60 * 60 * 24);

				if(diffDays<=15){
					daysLeftRegistrationText.setTextColor(ContextCompat.getColor(context,R.color.login_title));
				}

				if(diffDays>0){
					daysLeftRegistrationText.setText(context.getResources().getString(R.string.general_num_days_left, (int)diffDays));
					totalStorage = totalStorage + storageRegistration;
					totalTransfer = totalTransfer + transferRegistration;
					logDebug("After desktop install: storage: " + totalStorage + " transfer " + totalTransfer);
				}
				else{
                    daysLeftRegistrationText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
					figuresRegistrationLayout.setAlpha(0.5f);
					registrationIcon.setAlpha(0.5f);
					daysLeftRegistrationText.setPadding(scaleWidthPx(8,outMetrics), scaleHeightPx(4,outMetrics),scaleWidthPx(8,outMetrics),scaleHeightPx(4,outMetrics));
					daysLeftRegistrationText.setText(context.getResources().getString(R.string.expired_label));
				}
			}
			else{
				logDebug("MEGA_ACHIEVEMENT: "+type);
			}
		}

		storageQuota = details.currentStorage();

		logDebug("My calculated totalTransfer: " + totalStorage);
		String sizeString = getSizeString(storageQuota);
		figureUnlockedRewardStorage.setText(getNumberAndUnit(sizeString)[0]);
		figureUnlockedRewardStorageUnit.setText(getNumberAndUnit(sizeString)[1]);

		transferQuota = details.currentTransfer();
        sizeString = getSizeString(transferQuota);
		logDebug("My calculated totalTransfer: " + totalTransfer);
        figureUnlockedRewardTransfer.setText(getNumberAndUnit(sizeString)[0]);
        figureUnlockedRewardTransferUnit.setText(getNumberAndUnit(sizeString)[1]);
	}

    private String[] getNumberAndUnit(String sizeString) {
        if (sizeString == null || !sizeString.contains(" ")) {
            return new String[]{"", ""};
        }
        return sizeString.split(" ");
    }

	@Override
	public void onAchievementsReceived() {
		updateUI();
	}
}
