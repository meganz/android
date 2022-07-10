package mega.privacy.android.app.main.megaachievements;

import static mega.privacy.android.app.data.extensions.SizeStringKt.getBigFormattedStorageString;
import static mega.privacy.android.app.main.megaachievements.AchievementsActivity.INVALID_TYPE;
import static mega.privacy.android.app.main.megaachievements.AchievementsActivity.sFetcher;
import static mega.privacy.android.app.utils.Constants.BONUSES_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.INFO_ACHIEVEMENTS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.INVITE_FRIENDS_FRAGMENT;
import static mega.privacy.android.app.utils.Util.calculateDateFromTimestamp;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

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
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.MegaApi;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

@AndroidEntryPoint
public class AchievementsFragment extends Fragment implements OnClickListener
        , GetAchievementsListener.DataCallback {

    @Inject
    @MegaApi
    MegaApiAndroid megaApi;

    private RelativeLayout registrationLayout;
    private LinearLayout separatorRegistration;
    private RelativeLayout figuresInstallAppLayout;
    private TextView zeroFiguresInstallAppText;

    private RelativeLayout figuresReferralBonusesLayout;
    private TextView zeroFiguresReferralBonusesText;

    private RelativeLayout figuresRegistrationLayout;

    private RelativeLayout figuresInstallDesktopLayout;
    private TextView zeroFiguresInstallDesktopText;

    private RelativeLayout figuresAddPhoneLayout;
    private TextView zeroFiguresAddPhoneText;

    private ImageView installAppIcon;
    private ImageView installDesktopIcon;
    private ImageView registrationIcon;
    private ImageView addPhoneIcon;
    private ImageView referralBonusIcon;

    private TextView figureUnlockedRewardStorage;

    private TextView figureReferralBonusesStorage;

    private long storageReferrals;
    private long transferReferrals;


    private TextView figureInstallAppStorage;

    private TextView textInstallAppStorage;
    private TextView daysLeftInstallAppText;

    private TextView figureAddPhoneStorage;

    private TextView textAddPhoneStorage;

    private TextView daysLeftAddPhoneText;

    private TextView figureRegistrationStorage;

    private TextView textRegistrationStorage;

    private TextView daysLeftRegistrationText;

    private TextView figureInstallDesktopStorage;

    private TextView textInstallDesktopStorage;

    private TextView daysLeftInstallDesktopText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        boolean enabledAchievements = megaApi.isAchievementsEnabled();
        Timber.d("The achievements are: %s", enabledAchievements);

        View v = inflater.inflate(R.layout.fragment_achievements, container, false);

        Button inviteFriendsButton = (Button) v.findViewById(R.id.invite_button);
        inviteFriendsButton.setOnClickListener(this);

        RelativeLayout referralBonusesLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_layout);
        referralBonusesLayout.setOnClickListener(this);

        TextView titleReferralBonuses = (TextView) v.findViewById(R.id.title_referral_bonuses);

        boolean isPortrait = Util.isScreenInPortrait(MegaApplication.getInstance());

        titleReferralBonuses.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, getResources().getDisplayMetrics()));

        figuresReferralBonusesLayout = (RelativeLayout) v.findViewById(R.id.figures_referral_bonuses_layout);
        figuresReferralBonusesLayout.setVisibility(View.GONE);

        zeroFiguresReferralBonusesText = (TextView) v.findViewById(R.id.zero_figures_referral_bonuses_text);

        separatorRegistration = (LinearLayout) v.findViewById(R.id.separator_registration);
        registrationLayout = (RelativeLayout) v.findViewById(R.id.registration_layout);
        registrationLayout.setOnClickListener(this);

        TextView titleRegistration = (TextView) v.findViewById(R.id.title_registration);
        titleRegistration.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, getResources().getDisplayMetrics()));

        figuresRegistrationLayout = (RelativeLayout) v.findViewById(R.id.figures_registration_layout);

        RelativeLayout installAppLayout = (RelativeLayout) v.findViewById(R.id.install_app_layout);
        installAppLayout.setOnClickListener(this);

        TextView titleInstallApp = (TextView) v.findViewById(R.id.title_install_app);
        titleInstallApp.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, getResources().getDisplayMetrics()));

        figuresInstallAppLayout = (RelativeLayout) v.findViewById(R.id.figures_install_app_layout);
        figuresInstallAppLayout.setVisibility(View.GONE);
        zeroFiguresInstallAppText = (TextView) v.findViewById(R.id.zero_figures_install_app_text);

        RelativeLayout addPhoneLayout = v.findViewById(R.id.add_phone_layout);
        if (megaApi.smsAllowedState() == 2) {
            addPhoneLayout.setOnClickListener(this);
            TextView titleAddPhone = v.findViewById(R.id.title_add_phone);
            titleAddPhone.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, getResources().getDisplayMetrics()));
        } else {
            v.findViewById(R.id.separator_add_phone).setVisibility(View.GONE);
            addPhoneLayout.setVisibility(View.GONE);
        }
        figuresAddPhoneLayout = v.findViewById(R.id.figures_add_phone_layout);
        figuresAddPhoneLayout.setVisibility(View.GONE);
        zeroFiguresAddPhoneText = v.findViewById(R.id.zero_figures_add_phone_text);

        RelativeLayout installDesktopLayout = (RelativeLayout) v.findViewById(R.id.install_desktop_layout);
        installDesktopLayout.setOnClickListener(this);

        TextView titleInstallDesktop = (TextView) v.findViewById(R.id.title_install_desktop);
        titleInstallDesktop.setMaxWidth(scaleWidthPx(isPortrait ? 190 : 250, getResources().getDisplayMetrics()));

        figuresInstallDesktopLayout = (RelativeLayout) v.findViewById(R.id.figures_install_desktop_layout);
        figuresInstallDesktopLayout.setVisibility(View.GONE);

        zeroFiguresInstallDesktopText = (TextView) v.findViewById(R.id.zero_figures_install_desktop_text);

        installAppIcon = (ImageView) v.findViewById(R.id.install_app_icon);
        addPhoneIcon = v.findViewById(R.id.add_phone_icon);
        installDesktopIcon = (ImageView) v.findViewById(R.id.install_desktop_icon);
        registrationIcon = (ImageView) v.findViewById(R.id.registration_icon);
        referralBonusIcon = (ImageView) v.findViewById(R.id.referral_bonuses_icon);

        String storageQuotaString = getString(R.string.unlocked_storage_title);
        storageQuotaString = storageQuotaString.toLowerCase(Locale.getDefault());

        String storageSpaceString = getString(R.string.storage_space);
        storageSpaceString = storageSpaceString.toLowerCase(Locale.getDefault());

        figureUnlockedRewardStorage = (TextView) v.findViewById(R.id.unlocked_storage);
        figureUnlockedRewardStorage.setText(getBigFormattedStorageString(0));

        figureReferralBonusesStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_referral);
        figureReferralBonusesStorage.setText(getSizeString(0));

        TextView textReferralBonusesStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_referral);
        textReferralBonusesStorage.setText(storageSpaceString);

        figureInstallAppStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_app);

        figureInstallAppStorage.setText(getSizeString(0));

        textInstallAppStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_app);
        textInstallAppStorage.setText(storageSpaceString);
        daysLeftInstallAppText = (TextView) v.findViewById(R.id.days_left_text_install_app);
        daysLeftInstallAppText.setText(("..."));

        figureAddPhoneStorage = v.findViewById(R.id.figure_unlocked_storage_text_add_phone);

        figureAddPhoneStorage.setText(getSizeString(0));

        textAddPhoneStorage = v.findViewById(R.id.unlocked_storage_title_add_phone);
        textAddPhoneStorage.setText(storageSpaceString);

        daysLeftAddPhoneText = v.findViewById(R.id.days_left_text_add_phone);
        daysLeftAddPhoneText.setText(("..."));

        figureRegistrationStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_registration);
        figureRegistrationStorage.setText(getSizeString(0));

        textRegistrationStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_registration);
        textRegistrationStorage.setText(storageSpaceString);

        daysLeftRegistrationText = (TextView) v.findViewById(R.id.days_left_text_registration);
        daysLeftRegistrationText.setText(("..."));

        figureInstallDesktopStorage = (TextView) v.findViewById(R.id.figure_unlocked_storage_text_install_desktop);
        figureInstallDesktopStorage.setText(getSizeString(0));

        textInstallDesktopStorage = (TextView) v.findViewById(R.id.unlocked_storage_title_install_desktop);
        textInstallDesktopStorage.setText(storageSpaceString);

        daysLeftInstallDesktopText = (TextView) v.findViewById(R.id.days_left_text_install_desktop);
        daysLeftInstallDesktopText.setText(("..."));

        daysLeftInstallDesktopText.setVisibility(View.INVISIBLE);
        daysLeftInstallAppText.setVisibility(View.INVISIBLE);
        daysLeftAddPhoneText.setVisibility(View.INVISIBLE);

        figureUnlockedRewardStorage.setText("...");

        if (Util.isDarkMode(requireContext())) {
            int backgroundColor = ColorUtils.getColorForElevation(requireContext(), 1f);
            v.findViewById(R.id.unlocked_rewards_layout).setBackgroundColor(backgroundColor);
            v.findViewById(R.id.card_view_2).setBackgroundColor(backgroundColor);
        }

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(StringResourcesUtils.getString(R.string.achievements_title));
        }

        // The root view has been created, fill it with the data when data ready
        if (sFetcher != null) {
            sFetcher.setDataCallback(this);
        }
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");

        switch (v.getId()) {
            case R.id.referral_bonuses_layout: {
                Timber.d("Go to section Referral bonuses");
                ((AchievementsActivity) requireActivity()).showFragment((transferReferrals > 0 || storageReferrals > 0)
                        ? BONUSES_FRAGMENT : INVITE_FRIENDS_FRAGMENT, INVALID_TYPE);
                break;
            }
            case R.id.install_app_layout: {
                Timber.d("Go to info app install");
                ((AchievementsActivity) requireActivity()).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
                break;
            }
            case R.id.add_phone_layout: {
                Timber.d("Go to info add phone");
                ((AchievementsActivity) requireActivity()).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
                break;
            }
            case R.id.registration_layout: {
                Timber.d("Go to info registration");
                ((AchievementsActivity) requireActivity()).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
                break;
            }
            case R.id.install_desktop_layout: {
                Timber.d("Go to info desktop install");
                ((AchievementsActivity) requireActivity()).showFragment(INFO_ACHIEVEMENTS_FRAGMENT, MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
                break;
            }
            case R.id.invite_button: {
                Timber.d("Invite friends");
                ((AchievementsActivity) requireActivity()).showFragment(INVITE_FRIENDS_FRAGMENT, -1);
                break;
            }
        }
    }

    private void updateUI() {
        Timber.d("updateValues");
        if (sFetcher == null) return;

        MegaAchievementsDetails details = sFetcher.getAchievementsDetails();
        ArrayList<ReferralBonus> bonuses = sFetcher.getReferralBonuses();

        if (details == null || bonuses == null) {
            return;
        }

        long totalStorage = 0;
        long totalTransfer = 0;

        storageReferrals = details.currentStorageReferrals();
        totalStorage = totalStorage + storageReferrals;
        transferReferrals = details.currentTransferReferrals();
        totalTransfer = totalTransfer + transferReferrals;

        Timber.d("After referrals: storage: %s transfer %s", getSizeString(totalStorage), getSizeString(totalTransfer));

        long referralsStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
        long installAppStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
        long addPhoneStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
        long installDesktopStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);

        if (transferReferrals > 0 || storageReferrals > 0) {

            figureReferralBonusesStorage.setText(getSizeString(storageReferrals));

            figuresReferralBonusesLayout.setVisibility(View.VISIBLE);
            zeroFiguresReferralBonusesText.setVisibility(View.GONE);

            Timber.d("Check if referrals are expired");
            int expiredNumber = 0;

            for (int i = 0; i < bonuses.size(); i++) {
                ReferralBonus referralBonus = bonuses.get(i);
                if (referralBonus.getDaysLeft() < 0) {
                    expiredNumber++;
                }
            }

            if (expiredNumber >= bonuses.size() - 1) {
                Timber.d("All the referrals are expired");
                figuresReferralBonusesLayout.setAlpha(0.5f);
                referralBonusIcon.setAlpha(0.5f);
            }

        } else {
            figuresReferralBonusesLayout.setVisibility(View.GONE);
            zeroFiguresReferralBonusesText.setText(StringResourcesUtils.getString(R.string.figures_achievements_text_referrals, getSizeString(referralsStorageValue)));
            zeroFiguresReferralBonusesText.setVisibility(View.VISIBLE);
        }

        zeroFiguresInstallAppText.setText(StringResourcesUtils.getString(R.string.figures_achievements_text, getSizeString(installAppStorageValue)));
        zeroFiguresAddPhoneText.setText(StringResourcesUtils.getString(R.string.figures_achievements_text, getSizeString(addPhoneStorageValue)));
        zeroFiguresInstallDesktopText.setText(StringResourcesUtils.getString(R.string.figures_achievements_text, getSizeString(installDesktopStorageValue)));

        long count = details.getAwardsCount();

        for (int i = 0; i < count; i++) {
            int type = details.getAwardClass(i);

            int awardId = details.getAwardId(i);

            int rewardId = details.getRewardAwardId(awardId);
            Timber.d("AWARD ID: %d REWARD id: %d", awardId, rewardId);
            Timber.d("type: %d AWARD ID: %d REWARD id: %d", type, awardId, rewardId);

            if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL) {
                Timber.d("MEGA_ACHIEVEMENT_MOBILE_INSTALL");

                figuresInstallAppLayout.setVisibility(View.VISIBLE);
                zeroFiguresInstallAppText.setVisibility(View.GONE);

                long storageInstallApp = details.getRewardStorageByAwardId(awardId);
                if (storageInstallApp > 0) {
                    figureInstallAppStorage.setText(getSizeString(storageInstallApp));
                    figureInstallAppStorage.setVisibility(View.VISIBLE);
                    textInstallAppStorage.setVisibility(View.VISIBLE);
                } else {
                    figureInstallAppStorage.setVisibility(View.INVISIBLE);
                    textInstallAppStorage.setVisibility(View.INVISIBLE);
                }

                long transferInstallApp = details.getRewardTransferByAwardId(awardId);

                daysLeftInstallAppText.setVisibility(View.VISIBLE);
                long daysLeftInstallApp = details.getAwardExpirationTs(i);
                Timber.d("Install App AwardExpirationTs: %s", daysLeftInstallApp);

                Calendar start = calculateDateFromTimestamp(daysLeftInstallApp);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                if (diffDays <= 15) {
                    daysLeftInstallAppText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_600_red_400));
                }

                if (diffDays > 0) {
                    daysLeftInstallAppText.setText(StringResourcesUtils.getString(R.string.general_num_days_left, (int) diffDays));
                    totalStorage = totalStorage + storageInstallApp;
                    totalTransfer = totalTransfer + transferInstallApp;
                    Timber.d("After mobile install: storage: %s transfer %s", getSizeString(totalStorage), getSizeString(totalTransfer));
                } else {
                    daysLeftInstallAppText.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.expired_border));
                    figuresInstallAppLayout.setAlpha(0.5f);
                    installAppIcon.setAlpha(0.5f);
                    daysLeftInstallAppText.setPadding(scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()), scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()));
                    daysLeftInstallAppText.setText(StringResourcesUtils.getString(R.string.expired_label));
                }

            } else if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
                Timber.d("MEGA_ACHIEVEMENT_ADD_PHONE");

                figuresAddPhoneLayout.setVisibility(View.VISIBLE);
                zeroFiguresAddPhoneText.setVisibility(View.GONE);

                long storageAddPhone = details.getRewardStorageByAwardId(awardId);
                if (storageAddPhone > 0) {
                    figureAddPhoneStorage.setText(getSizeString(storageAddPhone));
                    figureAddPhoneStorage.setVisibility(View.VISIBLE);
                    textAddPhoneStorage.setVisibility(View.VISIBLE);
                } else {
                    figureAddPhoneStorage.setVisibility(View.INVISIBLE);
                    textAddPhoneStorage.setVisibility(View.INVISIBLE);
                }

                long transferAddPhone = details.getRewardTransferByAwardId(awardId);

                daysLeftAddPhoneText.setVisibility(View.VISIBLE);
                long daysLeftAddPhone = details.getAwardExpirationTs(i);
                Timber.d("Add phone AwardExpirationTs: %s", daysLeftAddPhone);

                Calendar start = calculateDateFromTimestamp(daysLeftAddPhone);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                if (diffDays <= 15) {
                    daysLeftAddPhoneText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_600_red_400));
                }

                if (diffDays > 0) {
                    daysLeftAddPhoneText.setText(StringResourcesUtils.getString(R.string.general_num_days_left, (int) diffDays));
                    totalStorage = totalStorage + storageAddPhone;
                    totalTransfer = totalTransfer + transferAddPhone;
                    Timber.d("After phone added: storage: %s transfer %s", getSizeString(totalStorage), getSizeString(totalTransfer));
                } else {
                    daysLeftAddPhoneText.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.expired_border));
                    figuresAddPhoneLayout.setAlpha(0.5f);
                    addPhoneIcon.setAlpha(0.5f);
                    daysLeftAddPhoneText.setPadding(scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()), scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()));
                    daysLeftAddPhoneText.setText(StringResourcesUtils.getString(R.string.expired_label));
                }
            } else if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL) {
                Timber.d("MEGA_ACHIEVEMENT_DESKTOP_INSTALL");

                figuresInstallDesktopLayout.setVisibility(View.VISIBLE);
                zeroFiguresInstallDesktopText.setVisibility(View.GONE);

                long storageInstallDesktop = details.getRewardStorageByAwardId(awardId);
                if (storageInstallDesktop > 0) {
                    figureInstallDesktopStorage.setText(getSizeString(storageInstallDesktop));
                    textInstallDesktopStorage.setVisibility(View.VISIBLE);
                    textInstallDesktopStorage.setVisibility(View.VISIBLE);
                } else {
                    figureInstallDesktopStorage.setVisibility(View.INVISIBLE);
                    textInstallDesktopStorage.setVisibility(View.INVISIBLE);
                }

                long transferInstallDesktop = details.getRewardTransferByAwardId(awardId);

                daysLeftInstallDesktopText.setVisibility(View.VISIBLE);
                long daysLeftInstallDesktop = details.getAwardExpirationTs(i);
                Timber.d("Install Desktop AwardExpirationTs: %s", daysLeftInstallDesktop);

                Calendar start = calculateDateFromTimestamp(daysLeftInstallDesktop);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                if (diffDays <= 15) {
                    daysLeftInstallDesktopText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_600_red_400));
                }

                if (diffDays > 0) {
                    daysLeftInstallDesktopText.setText(StringResourcesUtils.getString(R.string.general_num_days_left, (int) diffDays));
                    totalStorage = totalStorage + storageInstallDesktop;
                    totalTransfer = totalTransfer + transferInstallDesktop;
                    Timber.d("After desktop install: storage: %s transfer %s", getSizeString(totalStorage), getSizeString(totalTransfer));
                } else {
                    daysLeftInstallDesktopText.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.expired_border));
                    figuresInstallDesktopLayout.setAlpha(0.5f);
                    installDesktopIcon.setAlpha(0.5f);
                    daysLeftInstallDesktopText.setPadding(scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()), scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()));
                    daysLeftInstallDesktopText.setText(StringResourcesUtils.getString(R.string.expired_label));
                }

            } else if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME) {
                Timber.d("MEGA_ACHIEVEMENT_WELCOME");

                registrationLayout.setVisibility(View.VISIBLE);
                separatorRegistration.setVisibility(View.VISIBLE);
                long storageRegistration = details.getRewardStorageByAwardId(awardId);

                if (storageRegistration > 0) {
                    figureRegistrationStorage.setText(getSizeString(storageRegistration));
                    figureRegistrationStorage.setVisibility(View.VISIBLE);
                    textRegistrationStorage.setVisibility(View.VISIBLE);
                } else {
                    figureRegistrationStorage.setVisibility(View.INVISIBLE);
                    textRegistrationStorage.setVisibility(View.INVISIBLE);
                }

                long transferRegistration = details.getRewardTransferByAwardId(awardId);

                long daysLeftRegistration = details.getAwardExpirationTs(i);
                Timber.d("Registration AwardExpirationTs: %s", daysLeftRegistration);

                Calendar start = calculateDateFromTimestamp(daysLeftRegistration);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                if (diffDays <= 15) {
                    daysLeftRegistrationText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_600_red_400));
                }

                if (diffDays > 0) {
                    daysLeftRegistrationText.setText(StringResourcesUtils.getString(R.string.general_num_days_left, (int) diffDays));
                    totalStorage = totalStorage + storageRegistration;
                    totalTransfer = totalTransfer + transferRegistration;
                    Timber.d("After desktop install: storage: %d transfer %d", totalStorage, totalTransfer);
                } else {
                    daysLeftRegistrationText.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.expired_border));
                    figuresRegistrationLayout.setAlpha(0.5f);
                    registrationIcon.setAlpha(0.5f);
                    daysLeftRegistrationText.setPadding(scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()), scaleWidthPx(8, getResources().getDisplayMetrics()), scaleHeightPx(4, getResources().getDisplayMetrics()));
                    daysLeftRegistrationText.setText(StringResourcesUtils.getString(R.string.expired_label));
                }
            } else {
                Timber.d("MEGA_ACHIEVEMENT: %s", type);
            }
        }

        long storageQuota = details.currentStorage();

        Timber.d("My calculated totalTransfer: %s", totalStorage);
        figureUnlockedRewardStorage.setText(getBigFormattedStorageString(storageQuota));

        Timber.d("My calculated totalTransfer: %s", totalTransfer);
    }

    @Override
    public void onAchievementsReceived() {
        updateUI();
    }
}
