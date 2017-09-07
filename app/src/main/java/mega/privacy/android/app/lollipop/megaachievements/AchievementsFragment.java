package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

public class AchievementsFragment extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;

	LinearLayout parentLinearLayout;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	TextView figureUnlockedRewardStorage;
	TextView figureUnlockedRewardTransfer;
	TextView textUnlockedRewardTransfer;

	TextView figureReferralBonusesStorage;
	TextView figureReferralBonusesTransfer;
	TextView textReferralBonusesStorage;
	TextView textReferralBonusesTransfer;

	TextView figureInstallAppStorage;
	TextView figureInstallAppTransfer;
	TextView textInstallAppStorage;
	TextView textInstallAppTransfer;
	TextView daysLeftInstallAppTransfer;

	TextView figureRegistrationStorage;
	TextView figureRegistrationTransfer;
	TextView textRegistrationStorage;
	TextView textRegistrationTransfer;
	TextView daysLeftRegistrationTransfer;

	TextView figureInstallDesktopStorage;
	TextView figureInstallDesktopTransfer;
	TextView textInstallDesktopStorage;
	TextView textInstallDesktopTransfer;
	TextView daysLeftInstallDesktopTransfer;

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

		View v = inflater.inflate(R.layout.fragment_achievements, container, false);

		parentLinearLayout = (LinearLayout) v.findViewById(R.id.main_linear_layout_achievements);

		String transferQuotaString = getString(R.string.transfer_quota);
		transferQuotaString = transferQuotaString.toLowerCase(Locale.getDefault());

		String storageQuotaString = getString(R.string.unlocked_storage_title);
		storageQuotaString = storageQuotaString.toLowerCase(Locale.getDefault());

		figureUnlockedRewardStorage = (TextView) v.findViewById(R.id.unlocked_storage_text);
		TextView figureUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_text);
		TextView textUnlockedRewardTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title);

		TextView figureReferralBonusesStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_referral);
		TextView figureReferralBonusesTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_referral);
		TextView textReferralBonusesStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_referral);
		textReferralBonusesStorage.setText(storageQuotaString);
		TextView textReferralBonusesTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_referral);
		textReferralBonusesTransfer.setText(transferQuotaString);

		TextView figureInstallAppStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_app);
		TextView figureInstallAppTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_install_app);
		TextView textInstallAppStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_app);
		textInstallAppStorage.setText(storageQuotaString);
		TextView textInstallAppTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_install_app);
		textInstallAppTransfer.setText(transferQuotaString);
		TextView daysLeftInstallAppTransfer = (TextView) v.findViewById(R.id.days_left_text_install_app);
		daysLeftInstallAppTransfer.setText(("6 days left"));

		TextView figureRegistrationStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_registration);
		TextView figureRegistrationTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_registration);
		TextView textRegistrationStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_registration);
		textRegistrationStorage.setText(storageQuotaString);
		TextView textRegistrationTransfer = (TextView) v.findViewById(R.id.unlocked_transfer_title_registration);
		textRegistrationTransfer.setText(transferQuotaString);
		TextView daysLeftRegistrationTransfer = (TextView) v.findViewById(R.id.days_left_text_registration);
		daysLeftRegistrationTransfer.setText(("40 days left"));

		TextView figureInstallDesktopStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_desktop);
		TextView figureInstallDesktopTransfer = (TextView) v.findViewById(R.id.figure_unlocked_transfer_text_install_desktop);
		TextView textInstallDesktopStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_desktop);
		textInstallDesktopStorage.setText(storageQuotaString);
		TextView textInstallDesktopTransfer= (TextView) v.findViewById(R.id.unlocked_transfer_title_install_desktop);
		textInstallDesktopTransfer.setText(transferQuotaString);
		TextView daysLeftInstallDesktopTransfer = (TextView) v.findViewById(R.id.days_left_text_install_desktop);
		daysLeftInstallDesktopTransfer.setText(("15 days left"));

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

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
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch (v.getId()) {

			case R.id.logout_button:{
				log("Logout button");
				AccountController aC = new AccountController(context);
				aC.logout(context, megaApi, megaChatApi,false);
				break;
			}

		}
	}

	
	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

//		if(exportMKLayout.getVisibility()==View.VISIBLE){
//			log("Master Key layout is VISIBLE");
//			hideMKLayout();
//			return 1;
//		}

		return 0;
	}



	public static void log(String log) {
		Util.log("AchievementsFragment", log);
	}


}
