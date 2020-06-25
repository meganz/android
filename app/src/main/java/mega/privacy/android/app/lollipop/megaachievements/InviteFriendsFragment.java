package mega.privacy.android.app.lollipop.megaachievements;

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

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import nz.mega.sdk.MegaAchievementsDetails;

import static mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.getSizeString;

public class InviteFriendsFragment extends BaseFragment implements OnClickListener
		, GetAchievementsListener.DataCallback{
	Button inviteContactsBtn;
	TextView titleCard;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		logDebug("The achievements are: " + enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_invite_friends, container, false);
        inviteContactsBtn = v.findViewById(R.id.invite_contacts_button);
        inviteContactsBtn.setOnClickListener(this);
		titleCard = (TextView) v.findViewById(R.id.title_card_invite_fragment);

		return v;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Activity actionbar has been created which might be accessed by UpdateUI().
		if (mActivity != null) {
			ActionBar actionBar = ((AppCompatActivity) mActivity).getSupportActionBar();
			if (actionBar != null) {
				actionBar.setTitle(getString(R.string.title_referral_bonuses));
			}
		}

		// The root view has been created, fill it with the data when data ready
		if (sFetcher != null) {
			sFetcher.setDataCallback(this);
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.invite_contacts_button) {
		    logDebug("To InviteContactActivity.");
            Intent intent = new Intent(getContext(), InviteContactActivity.class);
            intent.putExtra(InviteContactActivity.KEY_FROM, true);
            if (mActivity != null) {
				mActivity.startActivityForResult(intent, REQUEST_CODE_GET_CONTACTS);
			}
        }
	}

	public int onBackPressed(){
		logDebug("onBackPressed");

		if (mActivity != null) {
			((AchievementsActivity)mActivity).showFragment(ACHIEVEMENTS_FRAGMENT, INVALID_TYPE);
		}
		return 0;
	}

	private void updateUI() {
		if (sFetcher == null) return;
		MegaAchievementsDetails details = sFetcher.getAchievementsDetails();
		if (details == null || getContext() == null) return;

		long referralsStorageValue = details.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long referralsTransferValue = details.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);

		titleCard.setText(getString(R.string.figures_achievements_text_referrals, getSizeString(referralsStorageValue), getSizeString(referralsTransferValue)));
	}

	@Override
	public void onAchievementsReceived() {
		updateUI();
	}
}
