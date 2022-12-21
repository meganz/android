package mega.privacy.android.app.main.megaachievements;

import static mega.privacy.android.app.utils.Constants.ACHIEVEMENTS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_GET_CONTACTS;
import static mega.privacy.android.app.utils.Util.getSizeString;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.app.main.InviteContactActivity;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.data.qualifier.MegaApi;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

@AndroidEntryPoint
public class InviteFriendsFragment extends Fragment implements OnClickListener
        , GetAchievementsListener.DataCallback {
    Button inviteContactsBtn;
    TextView titleCard;

    @Inject
    @MegaApi
    MegaApiAndroid megaApi;

    @Inject
    GetAchievementsListener getAchievementsListener;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        boolean enabledAchievements = megaApi.isAchievementsEnabled();
        Timber.d("The achievements are: %s", enabledAchievements);

        View v = inflater.inflate(R.layout.fragment_invite_friends, container, false);
        inviteContactsBtn = v.findViewById(R.id.invite_contacts_button);
        inviteContactsBtn.setOnClickListener(this);
        titleCard = (TextView) v.findViewById(R.id.title_card_invite_fragment);

        if (Util.isDarkMode(requireContext())) {
            int backgroundColor = ColorUtils.getColorForElevation(requireContext(), 1f);
            v.findViewById(R.id.invite_contacts_layout).setBackgroundColor(backgroundColor);
            v.findViewById(R.id.how_it_works_layout).setBackgroundColor(backgroundColor);
        }

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Activity actionbar has been created which might be accessed by UpdateUI().
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(StringResourcesUtils.getString(R.string.title_referral_bonuses));
        }

        // The root view has been created, fill it with the data when data ready
        getAchievementsListener.setDataCallback(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.invite_contacts_button) {
            Timber.d("To InviteContactActivity.");
            Intent intent = new Intent(requireContext(), InviteContactActivity.class);
            intent.putExtra(InviteContactActivity.KEY_FROM, true);
            requireActivity().startActivityForResult(intent, REQUEST_CODE_GET_CONTACTS);
        }
    }

    public int onBackPressed() {
        Timber.d("onBackPressed");

        ((AchievementsActivity) requireActivity()).showFragment(ACHIEVEMENTS_FRAGMENT);
        return 0;
    }

    private void updateUI() {
        MegaAchievementsDetails details = getAchievementsListener.getAchievementsDetails();
        if (details == null || getContext() == null) return;

        long referralsStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);

        titleCard.setText(getString(R.string.figures_achievements_text_referrals, getSizeString(referralsStorageValue)));
    }

    @Override
    public void onAchievementsReceived() {
        updateUI();
    }
}
