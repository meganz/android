package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class AchievementsFragment extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;

	LinearLayout parentLinearLayout;

	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	CardView inviteFriendsCard;

	RelativeLayout referralBonusesLayout;

    RelativeLayout registrationLayout;
    RelativeLayout installAppLayout;
    RelativeLayout installDesktopLayout;

	LinearLayout figuresInstallAppLayout;
	TextView zeroFiguresInstallAppText;

	LinearLayout figuresReferralBonusesLayout;
	TextView zeroFiguresReferralBonusesText;

	LinearLayout figuresRegistrationLayout;

	LinearLayout figuresInstallDesktopLayout;
	TextView zeroFiguresInstallDesktopText;

	TextView titleCardInvite;

	ImageView installAppIcon;
	ImageView installDesktopIcon;
	ImageView registrationIcon;
	ImageView referralBonusIcon;

	TextView figureUnlockedRewardStorage;
	long storageQuota = 0;
	TextView figureUnlockedRewardTransfer;
	long transferQuota = 0;
	TextView textUnlockedRewardTransfer;

	TextView titleReferralBonuses;
	TextView figureReferralBonusesStorage;
	TextView figureReferralBonusesTransfer;
	long storageReferrals;
	long transferReferrals;
	TextView textReferralBonusesStorage;
	TextView textReferralBonusesTransfer;

	TextView figureBaseQuotaStorage;
	TextView figureBaseQuotaTransfer;
	long storageBaseQuota;
	long transferBaseQuota;
	TextView textBaseQuotaStorage;
	TextView textBaseQuotaTransfer;

	TextView titleInstallApp;
	TextView figureInstallAppStorage;
	TextView figureInstallAppTransfer;
	long storageInstallApp;
	long transferInstallApp;
	long daysLeftInstallApp;

	TextView textInstallAppStorage;
	TextView textInstallAppTransfer;
	TextView daysLeftInstallAppText;

	TextView titleRegistration;
	TextView figureRegistrationStorage;
	TextView figureRegistrationTransfer;
	long storageRegistration;
	long transferRegistration;
	long daysLeftRegistration;

	TextView textRegistrationStorage;
	TextView textRegistrationTransfer;
	TextView daysLeftRegistrationText;

	TextView titleInstallDesktop;
	TextView figureInstallDesktopStorage;
	TextView figureInstallDesktopTransfer;
	long storageInstallDesktop;
	long transferInstallDesktop;
	long daysLeftInstallDesktop;

	TextView textInstallDesktopStorage;
	TextView textInstallDesktopTransfer;
	TextView daysLeftInstallDesktopText;

	Button inviteFriendsButton;

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

		if(megaChatApi==null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		logDebug("The achievements are: " + enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_achievements, container, false);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.main_linear_layout_achievements);

		titleCardInvite = (TextView) (TextView) v.findViewById(R.id.title_card);

		inviteFriendsCard = (CardView) v.findViewById(R.id.card_view_invite_friends);
		inviteFriendsButton = (Button) v.findViewById(R.id.invite_button);
		inviteFriendsCard.setOnClickListener(this);
		inviteFriendsButton.setOnClickListener(this);

		referralBonusesLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_layout);
		referralBonusesLayout.setOnClickListener(this);

		titleReferralBonuses = (TextView) v.findViewById(R.id.title_referral_bonuses);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleReferralBonuses.setMaxWidth(scaleWidthPx(250, outMetrics));
		}else{
			titleReferralBonuses.setMaxWidth(scaleWidthPx(190, outMetrics));
		}

		figuresReferralBonusesLayout = (LinearLayout) v.findViewById(R.id.figures_referral_bonuses_layout);
		figuresReferralBonusesLayout.setVisibility(View.GONE);

		zeroFiguresReferralBonusesText = (TextView) v.findViewById(R.id.zero_figures_referral_bonuses_text);

        registrationLayout = (RelativeLayout) v.findViewById(R.id.registration_layout);
        registrationLayout.setOnClickListener(this);

		titleRegistration = (TextView) v.findViewById(R.id.title_registration);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleRegistration.setMaxWidth(scaleWidthPx(250, outMetrics));
		}else{
			titleRegistration.setMaxWidth(scaleWidthPx(190, outMetrics));
		}

		figuresRegistrationLayout = (LinearLayout) v.findViewById(R.id.figures_registration_layout);

        installAppLayout = (RelativeLayout) v.findViewById(R.id.install_app_layout);
        installAppLayout.setOnClickListener(this);

		titleInstallApp = (TextView) v.findViewById(R.id.title_install_app);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleInstallApp.setMaxWidth(scaleWidthPx(250, outMetrics));
		}else{
			titleInstallApp.setMaxWidth(scaleWidthPx(190, outMetrics));
		}

		figuresInstallAppLayout = (LinearLayout) v.findViewById(R.id.figures_install_app_layout);
		figuresInstallAppLayout.setVisibility(View.GONE);

		zeroFiguresInstallAppText = (TextView) v.findViewById(R.id.zero_figures_install_app_text);

        installDesktopLayout = (RelativeLayout) v.findViewById(R.id.install_desktop_layout);
        installDesktopLayout.setOnClickListener(this);

		titleInstallDesktop = (TextView) v.findViewById(R.id.title_install_desktop);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleInstallDesktop.setMaxWidth(scaleWidthPx(250, outMetrics));
		}else{
			titleInstallDesktop.setMaxWidth(scaleWidthPx(190, outMetrics));
		}

		figuresInstallDesktopLayout = (LinearLayout) v.findViewById(R.id.figures_install_desktop_layout);
		figuresInstallDesktopLayout.setVisibility(View.GONE);

		zeroFiguresInstallDesktopText = (TextView) v.findViewById(R.id.zero_figures_install_desktop_text);

		installAppIcon = (ImageView) v.findViewById(R.id.install_app_icon);
		installDesktopIcon = (ImageView) v.findViewById(R.id.install_desktop_icon);
		registrationIcon = (ImageView) v.findViewById(R.id.registration_icon);
		referralBonusIcon = (ImageView) v.findViewById(R.id.referral_bonuses_icon);

		String transferQuotaString = getString(R.string.label_transfer_quota_achievements);
		transferQuotaString = transferQuotaString.toLowerCase(Locale.getDefault());

		String storageQuotaString = getString(R.string.unlocked_storage_title);
		storageQuotaString = storageQuotaString.toLowerCase(Locale.getDefault());

		figureUnlockedRewardStorage = (TextView) v.findViewById(R.id.unlocked_storage_text);
		figureUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_text);

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

		figureUnlockedRewardStorage.setText("...");

		figureUnlockedRewardTransfer.setText("...");

		return v;
	}

//	public static AchievementsFragment newInstance() {
//		log("newInstance");
//		AchievementsFragment fragment = new AchievementsFragment();
//		return fragment;
//	}

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

	@Override
	public void onClick(View v) {
		logDebug("onClick");
		switch (v.getId()) {

			case R.id.referral_bonuses_layout:{
				logDebug("Go to section Referral bonuses");
				if(transferReferrals>0||storageReferrals>0){
					((AchievementsActivity)context).showFragment(BONUSES_FRAGMENT, -1);
				}
				else{
					((AchievementsActivity)context).showFragment(INVITE_FRIENDS_FRAGMENT, -1);
				}
				break;
			}
			case R.id.install_app_layout:{
				logDebug("Go to info app install");
				((AchievementsActivity)context).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
				break;
			}
			case R.id.registration_layout:{
				logDebug("Go to info registration");
				((AchievementsActivity)context).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
				break;
			}
			case R.id.install_desktop_layout:{
				logDebug("Go to info desktop install");
				((AchievementsActivity)context).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
				break;
			}
			case R.id.card_view_invite_friends:
			case R.id.invite_button:{
				logDebug("Invite friends");
				((AchievementsActivity)context).showFragment(INVITE_FRIENDS_FRAGMENT, -1);
				break;
			}
		}
	}

	public void updateValues(){
		logDebug("updateValues");

		long totalStorage = 0;
		long totalTransfer = 0;

		storageReferrals = ((AchievementsActivity)context).megaAchievements.currentStorageReferrals();
		totalStorage = totalStorage + storageReferrals;
		transferReferrals = ((AchievementsActivity)context).megaAchievements.currentTransferReferrals();
		totalTransfer = totalTransfer + transferReferrals;

		logDebug("After referrals: storage: " + getSizeString(totalStorage) + " transfer " + getSizeString(totalTransfer));

		long referralsStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long referralsTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long installAppStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
		long installAppTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
		long installDesktopStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
		long installDesktopTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);

		titleCardInvite.setText(getString(R.string.figures_achievements_text_referrals, getSizeString(referralsStorageValue), getSizeString(referralsTransferValue)));

		if(transferReferrals>0||storageReferrals>0){

			figureReferralBonusesTransfer.setText(getSizeString(transferReferrals));
			figureReferralBonusesStorage.setText(getSizeString(storageReferrals));

			figuresReferralBonusesLayout.setVisibility(View.VISIBLE);
			zeroFiguresReferralBonusesText.setVisibility(View.GONE);

			logDebug("Check if referrals are expired");
			int expiredNumber = 0;
			if(((AchievementsActivity)context).referralBonuses!=null){
				for(int i=0;i<((AchievementsActivity)context).referralBonuses.size();i++){
					ReferralBonus referralBonus = ((AchievementsActivity)context).referralBonuses.get(i);
					if(referralBonus.getDaysLeft()<0){
						expiredNumber++;
					}
				}
			}

			if(expiredNumber>=((AchievementsActivity)context).referralBonuses.size()-1){
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
		zeroFiguresInstallDesktopText.setText(getString(R.string.figures_achievements_text, getSizeString(installDesktopStorageValue), getSizeString(installDesktopTransferValue)));

		long count = ((AchievementsActivity)context).megaAchievements.getAwardsCount();

		for(int i=0; i<count; i++){
			int type = ((AchievementsActivity)context).megaAchievements.getAwardClass(i);

			int awardId = ((AchievementsActivity)context).megaAchievements.getAwardId(i);

			int rewardId = ((AchievementsActivity)context).megaAchievements.getRewardAwardId(awardId);
			logDebug("AWARD ID: " + awardId + " REWARD id: " + rewardId);

			if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL){
				logDebug("MEGA_ACHIEVEMENT_MOBILE_INSTALL");

				figuresInstallAppLayout.setVisibility(View.VISIBLE);
				zeroFiguresInstallAppText.setVisibility(View.GONE);

				storageInstallApp = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				if(storageInstallApp>0){
					figureInstallAppStorage.setText(getSizeString(storageInstallApp));
					figureInstallAppStorage.setVisibility(View.VISIBLE);
					textInstallAppStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallAppStorage.setVisibility(View.INVISIBLE);
					textInstallAppStorage.setVisibility(View.INVISIBLE);
				}

				transferInstallApp = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
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
				daysLeftInstallApp = ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
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
					daysLeftInstallAppText.setText(context.getResources().getString(R.string.expired_achievement));
				}

			}
			else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){
				logDebug("MEGA_ACHIEVEMENT_DESKTOP_INSTALL");

				figuresInstallDesktopLayout.setVisibility(View.VISIBLE);
				zeroFiguresInstallDesktopText.setVisibility(View.GONE);

				storageInstallDesktop = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				if(storageInstallDesktop>0){
					figureInstallDesktopStorage.setText(getSizeString(storageInstallDesktop));
					textInstallDesktopStorage.setVisibility(View.VISIBLE);
					textInstallDesktopStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallDesktopStorage.setVisibility(View.INVISIBLE);
					textInstallDesktopStorage.setVisibility(View.INVISIBLE);
				}

				transferInstallDesktop = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
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
				daysLeftInstallDesktop = ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
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
					daysLeftInstallDesktopText.setText(context.getResources().getString(R.string.expired_achievement));
				}

			}
			else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
				logDebug("MEGA_ACHIEVEMENT_WELCOME");
				storageRegistration = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				if(storageRegistration>0){
					figureRegistrationStorage.setText(getSizeString(storageRegistration));
					figureRegistrationStorage.setVisibility(View.VISIBLE);
					textRegistrationStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureRegistrationStorage.setVisibility(View.INVISIBLE);
					textRegistrationStorage.setVisibility(View.INVISIBLE);
				}

				transferRegistration= ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
				if(transferRegistration>0){
					figureRegistrationTransfer.setText(getSizeString(transferRegistration));
					figureRegistrationTransfer.setVisibility(View.VISIBLE);
					textRegistrationTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureRegistrationTransfer.setVisibility(View.INVISIBLE);
					textRegistrationTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftRegistration= ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
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
					daysLeftRegistrationText.setText(context.getResources().getString(R.string.expired_achievement));
				}
			}
			else{
				logDebug("MEGA_ACHIEVEMENT: "+type);
			}
		}

		storageQuota = ((AchievementsActivity)context).megaAchievements.currentStorage();

		logDebug("My calculated totalTransfer: " + totalStorage);
		figureUnlockedRewardStorage.setText(getSizeString(storageQuota));

		transferQuota = ((AchievementsActivity)context).megaAchievements.currentTransfer();

		logDebug("My calculated totalTransfer: " + totalTransfer);
		figureUnlockedRewardTransfer.setText(getSizeString(transferQuota));
	}
}
