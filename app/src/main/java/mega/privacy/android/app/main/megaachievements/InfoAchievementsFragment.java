package mega.privacy.android.app.main.megaachievements;

import static mega.privacy.android.app.main.megaachievements.AchievementsActivity.sFetcher;
import static mega.privacy.android.app.utils.Util.calculateDateFromTimestamp;
import static mega.privacy.android.app.utils.Util.getSizeString;

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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import timber.log.Timber;

public class InfoAchievementsFragment extends BaseFragment implements GetAchievementsListener.DataCallback {
    ActionBar actionBar;

    ImageView icon;
    ImageView checkIcon;
    TextView title;
    TextView sectionTitle;
    TextView firstParagraph;
    TextView secondParagraph;

    int achievementType = -1;
    int awardId = -1;
    int rewardId = -1;
    long diffDays;
    int indexAward;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        View v = inflater.inflate(R.layout.fragment_info_achievements, container, false);

        icon = v.findViewById(R.id.icon_info_achievements);
        checkIcon = v.findViewById(R.id.icon_achievement_completed);
        title = v.findViewById(R.id.title_info_achievements);
        sectionTitle = v.findViewById(R.id.how_works_title);
        firstParagraph = v.findViewById(R.id.info_achievements_how_works_first_p);
        secondParagraph = v.findViewById(R.id.info_achievements_how_works_second_p);

        final Bundle arguments = getArguments();
        if (arguments == null) {
            Timber.w("Arguments are null. No achievement type.");
            return v;
        }
        achievementType = arguments.getInt("achievementType");

        if (Util.isDarkMode(context)) {
            int backgroundColor = ColorUtils.getColorForElevation(context, 1f);
            v.findViewById(R.id.title_layout).setBackgroundColor(backgroundColor);
            v.findViewById(R.id.how_it_works_layout).setBackgroundColor(backgroundColor);
        }

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
        actionBar = ((AppCompatActivity) mActivity).getSupportActionBar();

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

        actionBar.setTitle(title.toUpperCase(Locale.getDefault()));
    }

    private void updateUI() {
        if (sFetcher == null) return;
        MegaAchievementsDetails details = sFetcher.getAchievementsDetails();

        if (details == null || context == null) return;

        long count = details.getAwardsCount();
        for (int i = 0; i < count; i++) {
            int type = details.getAwardClass(i);

            if (type == achievementType) {
                awardId = details.getAwardId(i);

                rewardId = details.getRewardAwardId(awardId);
                Timber.d("AWARD ID: %d REWARD id: %d", awardId, rewardId);

                long daysLeft = details.getAwardExpirationTs(i);

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
            } else {
                Timber.w("No match for achievement award!");
            }
        }

        if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL) {
            long installAppStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            long installAppTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_MOBILE_INSTALL);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mobile_big));

            if (awardId == -1) {
                Timber.w("No award for this achievement");
                checkIcon.setVisibility(View.GONE);
                title.setText(StringResourcesUtils.getString(R.string.figures_achievements_text, getSizeString(installAppStorageValue)));
                title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                sectionTitle.setVisibility(View.VISIBLE);
                firstParagraph.setText(StringResourcesUtils.getString(R.string.paragraph_info_achievement_install_mobile_app, getSizeString(installAppStorageValue)));
                secondParagraph.setVisibility(View.GONE);
            } else {
                if (diffDays <= 15) {
                    title.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
                    title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
                } else {
                    title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
                }

                if (diffDays > 0) {
                    title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
                } else {
                    title.setText(context.getResources().getString(R.string.expired_label));
                }

                long storageAppInstall = details.getRewardStorageByAwardId(awardId);
                long transferAppInstall = details.getRewardTransferByAwardId(awardId);
                firstParagraph.setText(StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_install_mobile_app, getSizeString(storageAppInstall)));
                secondParagraph.setVisibility(View.GONE);
            }
        } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE) {
            long addPhoneStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
            long addPhoneTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.il_verify_phone_drawer));

            if (awardId == -1) {
                Timber.d("No award for this achievement");
                checkIcon.setVisibility(View.GONE);
                title.setText(StringResourcesUtils.getString(R.string.figures_achievements_text, getSizeString(addPhoneStorageValue)));
                title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                sectionTitle.setVisibility(View.VISIBLE);
                firstParagraph.setText(StringResourcesUtils.getString(R.string.paragraph_info_achievement_add_phone, getSizeString(addPhoneStorageValue)));
                secondParagraph.setVisibility(View.GONE);
            } else {
                if (diffDays <= 15) {
                    title.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
                    title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
                } else {
                    title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
                }

                if (diffDays > 0) {
                    title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
                } else {
                    title.setText(context.getResources().getString(R.string.expired_label));
                }

                long storageAddPhone = details.getRewardStorageByAwardId(awardId);
                long transferAddPhone = details.getRewardTransferByAwardId(awardId);
                firstParagraph.setText(StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_add_phone, getSizeString(storageAddPhone)));
                secondParagraph.setVisibility(View.GONE);
            }
        } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL) {
            long installDesktopStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
            long installDesktopTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_DESKTOP_INSTALL);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_install_mega_big));

            if (awardId == -1) {
                Timber.w("No award for this achievement");
                checkIcon.setVisibility(View.GONE);
                title.setText(StringResourcesUtils.getString(R.string.figures_achievements_text, getSizeString(installDesktopStorageValue)));
                title.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                sectionTitle.setVisibility(View.VISIBLE);
                firstParagraph.setText(StringResourcesUtils.getString(R.string.paragraph_info_achievement_install_desktop, getSizeString(installDesktopStorageValue)));
                secondParagraph.setVisibility(View.GONE);
            } else {
                if (diffDays <= 15) {
                    title.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
                    title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
                } else {
                    title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
                }

                if (diffDays > 0) {
                    title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
                } else {
                    title.setText(context.getResources().getString(R.string.expired_label));
                }

                long storageDesktopInstall = details.getRewardStorageByAwardId(awardId);
                long transferDesktopInstall = details.getRewardTransferByAwardId(awardId);
                firstParagraph.setText(StringResourcesUtils.getString(R.string.result_paragraph_info_achievement_install_desktop, getSizeString(storageDesktopInstall)));
                secondParagraph.setVisibility(View.GONE);

            }
        } else if (achievementType == MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME) {
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_registration_big));
//            long registrationStorageValue = achievementsActivity.megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);
//            long registrationTransferValue = achievementsActivity.megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_WELCOME);

            if (diffDays <= 15) {
                title.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
                title.setBackground(ContextCompat.getDrawable(context, R.drawable.expired_border));
            } else {
                title.setBackground(ContextCompat.getDrawable(context, R.drawable.bonus_ts_border));
            }

            if (diffDays > 0) {
                title.setText(getString(R.string.expiration_date_for_achievements, diffDays));
            } else {
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
