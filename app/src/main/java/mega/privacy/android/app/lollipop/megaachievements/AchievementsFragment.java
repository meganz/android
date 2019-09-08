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
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class AchievementsFragment extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	private Context context;
	private ActionBar aB;

	private LinearLayout parentLinearLayout;

	private DisplayMetrics outMetrics;
	float density;

	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;

	private CardView inviteFriendsCard;

	private RelativeLayout referralBonusesLayout;

	private RelativeLayout registrationLayout;
	private RelativeLayout installAppLayout;
	private RelativeLayout installDesktopLayout;
    private RelativeLayout addPhoneLayout;

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

	private TextView titleCardInvite;

	private ImageView installAppIcon;
	private ImageView installDesktopIcon;
	private ImageView registrationIcon;
	private ImageView addPhoneIcon;
	private ImageView referralBonusIcon;

	private TextView figureUnlockedRewardStorage;
	private long storageQuota = 0;
	private TextView figureUnlockedRewardTransfer;

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
	private long storageBaseQuota;
	private long transferBaseQuota;

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

		if(megaChatApi==null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		log("The achievements are: "+enabledAchievements);

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
			titleReferralBonuses.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
		}else{
			titleReferralBonuses.setMaxWidth(Util.scaleWidthPx(190, outMetrics));
		}

		figuresReferralBonusesLayout = (LinearLayout) v.findViewById(R.id.figures_referral_bonuses_layout);
		figuresReferralBonusesLayout.setVisibility(View.GONE);

		zeroFiguresReferralBonusesText = (TextView) v.findViewById(R.id.zero_figures_referral_bonuses_text);

        registrationLayout = (RelativeLayout) v.findViewById(R.id.registration_layout);
        registrationLayout.setOnClickListener(this);

		titleRegistration = (TextView) v.findViewById(R.id.title_registration);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleRegistration.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
		}else{
			titleRegistration.setMaxWidth(Util.scaleWidthPx(190, outMetrics));
		}

		figuresRegistrationLayout = (LinearLayout) v.findViewById(R.id.figures_registration_layout);

        installAppLayout = (RelativeLayout) v.findViewById(R.id.install_app_layout);
        installAppLayout.setOnClickListener(this);

		titleInstallApp = (TextView) v.findViewById(R.id.title_install_app);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleInstallApp.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
		}else{
			titleInstallApp.setMaxWidth(Util.scaleWidthPx(190, outMetrics));
		}

		figuresInstallAppLayout = (LinearLayout) v.findViewById(R.id.figures_install_app_layout);
		figuresInstallAppLayout.setVisibility(View.GONE);
		zeroFiguresInstallAppText = (TextView) v.findViewById(R.id.zero_figures_install_app_text);

        addPhoneLayout = v.findViewById(R.id.add_phone_layout);
        addPhoneLayout.setOnClickListener(this);
        titleAddPhone = v.findViewById(R.id.title_add_phone);
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            titleAddPhone.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
        }else{
            titleAddPhone.setMaxWidth(Util.scaleWidthPx(190, outMetrics));
        }
        figuresAddPhoneLayout = v.findViewById(R.id.figures_add_phone_layout);
        figuresAddPhoneLayout.setVisibility(View.GONE);
        zeroFiguresAddPhoneText = v.findViewById(R.id.zero_figures_add_phone_text);

        installDesktopLayout = (RelativeLayout) v.findViewById(R.id.install_desktop_layout);
        installDesktopLayout.setOnClickListener(this);

		titleInstallDesktop = (TextView) v.findViewById(R.id.title_install_desktop);
		if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			titleInstallDesktop.setMaxWidth(Util.scaleWidthPx(250, outMetrics));
		}else{
			titleInstallDesktop.setMaxWidth(Util.scaleWidthPx(190, outMetrics));
		}

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
		figureUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_text);

		figureUnlockedRewardStorage.setText(Util.getSizeString(0));
		figureUnlockedRewardTransfer.setText(Util.getSizeString(0));

		textUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title);

		figureReferralBonusesStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_referral);
		figureReferralBonusesTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_referral);

		figureReferralBonusesStorage.setText(Util.getSizeString(0));
		figureReferralBonusesTransfer.setText(Util.getSizeString(0));

		textReferralBonusesStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_referral);
		textReferralBonusesStorage.setText(storageQuotaString);
		textReferralBonusesTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_referral);
		textReferralBonusesTransfer.setText(transferQuotaString);

		figureBaseQuotaStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_base_quota);
		figureBaseQuotaTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_base_quota);

		figureBaseQuotaStorage.setText(Util.getSizeString(0));
		figureBaseQuotaTransfer.setText(Util.getSizeString(0));
		figureBaseQuotaTransfer.setVisibility(View.INVISIBLE);

		textBaseQuotaStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_base_quota);
		textBaseQuotaStorage.setText(storageQuotaString);
		textBaseQuotaTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_base_quota);
		textBaseQuotaTransfer.setText(transferQuotaString);
		textBaseQuotaTransfer.setVisibility(View.INVISIBLE);

		figureInstallAppStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_app);
		figureInstallAppTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_install_app);

		figureInstallAppStorage.setText(Util.getSizeString(0));
		figureInstallAppTransfer.setText(Util.getSizeString(0));

		textInstallAppStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_app);
		textInstallAppStorage.setText(storageQuotaString);
		textInstallAppTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_install_app);
		textInstallAppTransfer.setText(transferQuotaString);
		daysLeftInstallAppText = (TextView) v.findViewById(R.id.days_left_text_install_app);
		daysLeftInstallAppText.setText(("..."));

        figureAddPhoneStorage = v.findViewById(R.id.figure_unlocked_storage_text_add_phone);
        figureAddPhoneTransfer = v.findViewById(R.id.figure_unlocked_transfer_text_add_phone);

        figureAddPhoneStorage.setText(Util.getSizeString(0));
        figureAddPhoneTransfer.setText(Util.getSizeString(0));

        textAddPhoneStorage = v.findViewById(R.id.unlocked_storage_title_add_phone);
        textAddPhoneStorage.setText(storageQuotaString);
        textAddPhoneTransfer = v.findViewById(R.id.unlocked_transfer_title_add_phone);
        textAddPhoneTransfer.setText(transferQuotaString);
        daysLeftAddPhoneText = v.findViewById(R.id.days_left_text_add_phone);
        daysLeftAddPhoneText.setText(("..."));

		figureRegistrationStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_registration);
		figureRegistrationTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_registration);

		figureRegistrationStorage.setText(Util.getSizeString(0));
		figureRegistrationTransfer.setText(Util.getSizeString(0));

		textRegistrationStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_registration);
		textRegistrationStorage.setText(storageQuotaString);
		textRegistrationTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_registration);
		textRegistrationTransfer.setText(transferQuotaString);
		daysLeftRegistrationText = (TextView) v.findViewById(R.id.days_left_text_registration);
		daysLeftRegistrationText.setText(("..."));

		figureInstallDesktopStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_desktop);
		figureInstallDesktopTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_install_desktop);

		figureInstallDesktopStorage.setText(Util.getSizeString(0));
		figureInstallDesktopTransfer.setText(Util.getSizeString(0));

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

//	public static AchievementsFragment newInstance() {
//		log("newInstance");
//		AchievementsFragment fragment = new AchievementsFragment();
//		return fragment;
//	}

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

			case R.id.referral_bonuses_layout:{
				log("Go to section Referral bonuses");
				if(transferReferrals>0||storageReferrals>0){
					((AchievementsActivity)context).showFragment(Constants.BONUSES_FRAGMENT, -1);
				}
				else{
					((AchievementsActivity)context).showFragment(Constants.INVITE_FRIENDS_FRAGMENT, -1);
				}
				break;
			}
			case R.id.install_app_layout:{
				log("Go to info app install");
				((AchievementsActivity)context).showFragment(Constants.INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
				break;
			}
            case R.id.add_phone_layout:{
                log("Go to info add phone");
                ((AchievementsActivity)context).showFragment(Constants.INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
                break;
            }
			case R.id.registration_layout:{
				log("Go to info registration");
				((AchievementsActivity)context).showFragment(Constants.INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
				break;
			}
			case R.id.install_desktop_layout:{
				log("Go to info desktop install");
				((AchievementsActivity)context).showFragment(Constants.INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
				break;
			}
			case R.id.card_view_invite_friends:
			case R.id.invite_button:{
				log("Invite friends");
				((AchievementsActivity)context).showFragment(Constants.INVITE_FRIENDS_FRAGMENT, -1);
				break;
			}
		}
	}

	public void updateValues(){
		log("updateValues");

		long totalStorage = 0;
		long totalTransfer = 0;

		storageReferrals = ((AchievementsActivity)context).megaAchievements.currentStorageReferrals();
		totalStorage = totalStorage + storageReferrals;
		transferReferrals = ((AchievementsActivity)context).megaAchievements.currentTransferReferrals();
		totalTransfer = totalTransfer + transferReferrals;

		log("After referrals: storage: "+Util.getSizeString(totalStorage)+" transfer "+Util.getSizeString(totalTransfer));

		long referralsStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long referralsTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long installAppStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
		long installAppTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
        long addPhoneStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
        long addPhoneTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
		long installDesktopStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
        long installDesktopTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
        log("installDesktopStorageValue: " + installDesktopStorageValue + " installDesktopTransferValue: " + installDesktopTransferValue);
        log("addPhoneStorageValue: " + addPhoneStorageValue + " addPhoneTransferValue: " + addPhoneTransferValue);
        titleCardInvite.setText(getString(R.string.figures_achievements_text_referrals, Util.getSizeString(referralsStorageValue), Util.getSizeString(referralsTransferValue)));

		if(transferReferrals>0||storageReferrals>0){

			figureReferralBonusesTransfer.setText(Util.getSizeString(transferReferrals));
			figureReferralBonusesStorage.setText(Util.getSizeString(storageReferrals));

			figuresReferralBonusesLayout.setVisibility(View.VISIBLE);
			zeroFiguresReferralBonusesText.setVisibility(View.GONE);

			log("Check if referrals are expired");
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
				log("All the referrals are expired");
				figuresReferralBonusesLayout.setAlpha(0.5f);
				referralBonusIcon.setAlpha(0.5f);
			}

		}
		else{
			figuresReferralBonusesLayout.setVisibility(View.GONE);
			zeroFiguresReferralBonusesText.setText(getString(R.string.figures_achievements_text_referrals, Util.getSizeString(referralsStorageValue), Util.getSizeString(referralsTransferValue)));
			zeroFiguresReferralBonusesText.setVisibility(View.VISIBLE);
		}

		zeroFiguresInstallAppText.setText(getString(R.string.figures_achievements_text, Util.getSizeString(installAppStorageValue), Util.getSizeString(installAppTransferValue)));
		zeroFiguresAddPhoneText.setText(getString(R.string.figures_achievements_text, Util.getSizeString(addPhoneStorageValue), Util.getSizeString(addPhoneTransferValue)));
		zeroFiguresInstallDesktopText.setText(getString(R.string.figures_achievements_text, Util.getSizeString(installDesktopStorageValue), Util.getSizeString(installDesktopTransferValue)));

		long count = ((AchievementsActivity)context).megaAchievements.getAwardsCount();

		for(int i=0; i<count; i++){
			int type = ((AchievementsActivity)context).megaAchievements.getAwardClass(i);

			int awardId = ((AchievementsActivity)context).megaAchievements.getAwardId(i);

			int rewardId = ((AchievementsActivity)context).megaAchievements.getRewardAwardId(awardId);
			log("AWARD ID: "+awardId+" REWARD id: "+rewardId);
			log("type: " + type + " AWARD ID: "+awardId+" REWARD id: "+rewardId);

			if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL){
				log("MEGA_ACHIEVEMENT_MOBILE_INSTALL");

				figuresInstallAppLayout.setVisibility(View.VISIBLE);
				zeroFiguresInstallAppText.setVisibility(View.GONE);

				storageInstallApp = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				if(storageInstallApp>0){
					figureInstallAppStorage.setText(Util.getSizeString(storageInstallApp));
					figureInstallAppStorage.setVisibility(View.VISIBLE);
					textInstallAppStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallAppStorage.setVisibility(View.INVISIBLE);
					textInstallAppStorage.setVisibility(View.INVISIBLE);
				}

				transferInstallApp = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
				if(transferInstallApp>0){
					figureInstallAppTransfer.setText(Util.getSizeString(transferInstallApp));
					figureInstallAppTransfer.setVisibility(View.VISIBLE);
					textInstallAppTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallAppTransfer.setVisibility(View.INVISIBLE);
					textInstallAppTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftInstallAppText.setVisibility(View.VISIBLE);
				daysLeftInstallApp = ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
				log("Install App AwardExpirationTs: "+daysLeftInstallApp);

				Calendar start = Util.calculateDateFromTimestamp(daysLeftInstallApp);
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
					log("After mobile install: storage: "+Util.getSizeString(totalStorage)+" transfer "+Util.getSizeString(totalTransfer));
				}
				else{
					daysLeftInstallAppText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
					figuresInstallAppLayout.setAlpha(0.5f);
					installAppIcon.setAlpha(0.5f);
					daysLeftInstallAppText.setPadding(Util.scaleWidthPx(8,outMetrics), Util.scaleHeightPx(4,outMetrics),Util.scaleWidthPx(8,outMetrics),Util.scaleHeightPx(4,outMetrics));
					daysLeftInstallAppText.setText(context.getResources().getString(R.string.expired_achievement));
				}

			}else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
                log("MEGA_ACHIEVEMENT_ADD_PHONE");

                figuresAddPhoneLayout.setVisibility(View.VISIBLE);
                zeroFiguresAddPhoneText.setVisibility(View.GONE);

                storageAddPhone = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
                if(storageAddPhone>0){
                    figureAddPhoneStorage.setText(Util.getSizeString(storageAddPhone));
                    figureAddPhoneStorage.setVisibility(View.VISIBLE);
                    textAddPhoneStorage.setVisibility(View.VISIBLE);
                }
                else{
                    figureAddPhoneStorage.setVisibility(View.INVISIBLE);
                    textAddPhoneStorage.setVisibility(View.INVISIBLE);
                }

                transferAddPhone = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
                if(transferAddPhone>0){
                    figureAddPhoneTransfer.setText(Util.getSizeString(transferAddPhone));
                    figureAddPhoneTransfer.setVisibility(View.VISIBLE);
                    textAddPhoneTransfer.setVisibility(View.VISIBLE);
                }
                else{
                    figureAddPhoneTransfer.setVisibility(View.INVISIBLE);
                    textAddPhoneTransfer.setVisibility(View.INVISIBLE);
                }

                daysLeftAddPhoneText.setVisibility(View.VISIBLE);
                daysLeftAddPhone = ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
                log("Add phone AwardExpirationTs: "+daysLeftAddPhone);

                Calendar start = Util.calculateDateFromTimestamp(daysLeftAddPhone);
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
                    log("After phone added: storage: "+Util.getSizeString(totalStorage)+" transfer "+Util.getSizeString(totalTransfer));
                }
                else{
                    daysLeftAddPhoneText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
                    figuresAddPhoneLayout.setAlpha(0.5f);
                    addPhoneIcon.setAlpha(0.5f);
                    daysLeftAddPhoneText.setPadding(Util.scaleWidthPx(8,outMetrics), Util.scaleHeightPx(4,outMetrics),Util.scaleWidthPx(8,outMetrics),Util.scaleHeightPx(4,outMetrics));
                    daysLeftAddPhoneText.setText(context.getResources().getString(R.string.expired_achievement));
                }
            }
			else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL){
				log("MEGA_ACHIEVEMENT_DESKTOP_INSTALL");

				figuresInstallDesktopLayout.setVisibility(View.VISIBLE);
				zeroFiguresInstallDesktopText.setVisibility(View.GONE);

				storageInstallDesktop = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				if(storageInstallDesktop>0){
					figureInstallDesktopStorage.setText(Util.getSizeString(storageInstallDesktop));
					textInstallDesktopStorage.setVisibility(View.VISIBLE);
					textInstallDesktopStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallDesktopStorage.setVisibility(View.INVISIBLE);
					textInstallDesktopStorage.setVisibility(View.INVISIBLE);
				}

				transferInstallDesktop = ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
				if(transferInstallDesktop>0){
					figureInstallDesktopTransfer.setText(Util.getSizeString(transferInstallDesktop));
					figureInstallDesktopTransfer.setVisibility(View.VISIBLE);
					textInstallDesktopTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureInstallDesktopTransfer.setVisibility(View.INVISIBLE);
					textInstallDesktopTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftInstallDesktopText.setVisibility(View.VISIBLE);
				daysLeftInstallDesktop = ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
				log("Install Desktop AwardExpirationTs: "+daysLeftInstallDesktop);

				Calendar start = Util.calculateDateFromTimestamp(daysLeftInstallDesktop);
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
					log("After desktop install: storage: "+Util.getSizeString(totalStorage)+" transfer "+Util.getSizeString(totalTransfer));
				}
				else{
                    daysLeftInstallDesktopText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
					figuresInstallDesktopLayout.setAlpha(0.5f);
					installDesktopIcon.setAlpha(0.5f);
					daysLeftInstallDesktopText.setPadding(Util.scaleWidthPx(8,outMetrics), Util.scaleHeightPx(4,outMetrics),Util.scaleWidthPx(8,outMetrics),Util.scaleHeightPx(4,outMetrics));
					daysLeftInstallDesktopText.setText(context.getResources().getString(R.string.expired_achievement));
				}

			}
			else if(type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME){
				log("MEGA_ACHIEVEMENT_WELCOME");
				storageRegistration = ((AchievementsActivity)context).megaAchievements.getRewardStorageByAwardId(awardId);
				if(storageRegistration>0){
					figureRegistrationStorage.setText(Util.getSizeString(storageRegistration));
					figureRegistrationStorage.setVisibility(View.VISIBLE);
					textRegistrationStorage.setVisibility(View.VISIBLE);
				}
				else{
					figureRegistrationStorage.setVisibility(View.INVISIBLE);
					textRegistrationStorage.setVisibility(View.INVISIBLE);
				}

				transferRegistration= ((AchievementsActivity)context).megaAchievements.getRewardTransferByAwardId(awardId);
				if(transferRegistration>0){
					figureRegistrationTransfer.setText(Util.getSizeString(transferRegistration));
					figureRegistrationTransfer.setVisibility(View.VISIBLE);
					textRegistrationTransfer.setVisibility(View.VISIBLE);
				}
				else{
					figureRegistrationTransfer.setVisibility(View.INVISIBLE);
					textRegistrationTransfer.setVisibility(View.INVISIBLE);
				}

				daysLeftRegistration= ((AchievementsActivity)context).megaAchievements.getAwardExpirationTs(i);
				log("Registration AwardExpirationTs: "+daysLeftRegistration);

				Calendar start = Util.calculateDateFromTimestamp(daysLeftRegistration);
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
					log("After desktop install: storage: "+totalStorage+" transfer "+totalTransfer);
				}
				else{
                    daysLeftRegistrationText.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
					figuresRegistrationLayout.setAlpha(0.5f);
					registrationIcon.setAlpha(0.5f);
					daysLeftRegistrationText.setPadding(Util.scaleWidthPx(8,outMetrics), Util.scaleHeightPx(4,outMetrics),Util.scaleWidthPx(8,outMetrics),Util.scaleHeightPx(4,outMetrics));
					daysLeftRegistrationText.setText(context.getResources().getString(R.string.expired_achievement));
				}
			}
			else{
				log("MEGA_ACHIEVEMENT: "+type);
			}
		}

		storageQuota = ((AchievementsActivity)context).megaAchievements.currentStorage();

		log("My calculated totalTransfer: "+totalStorage);
		figureUnlockedRewardStorage.setText(Util.getSizeString(storageQuota));

		transferQuota = ((AchievementsActivity)context).megaAchievements.currentTransfer();

		log("My calculated totalTransfer: "+totalTransfer);
		figureUnlockedRewardTransfer.setText(Util.getSizeString(transferQuota));
	}

	public static void log(String log) {
		Util.log("AchievementsFragment", log);
	}
}
