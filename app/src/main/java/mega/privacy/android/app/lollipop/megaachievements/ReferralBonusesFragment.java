package mega.privacy.android.app.lollipop.megaachievements;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.listeners.GetAchievementsListener;

import static mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity.sFetcher;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class ReferralBonusesFragment extends BaseFragment implements OnClickListener
		, GetAchievementsListener.DataCallback{
	RelativeLayout parentRelativeLayout;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	MegaReferralBonusesAdapter adapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		logDebug("The achievements are: " + enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_referral_bonuses, container, false);

		parentRelativeLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_relative_layout);

		recyclerView = (RecyclerView) v.findViewById(R.id.referral_bonuses_recycler_view);
		recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context));
		mLayoutManager = new LinearLayoutManager(context);
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

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
		logDebug("onClick");
		switch (v.getId()) {
			case R.id.referral_bonuses_layout:{
				logDebug("Go to section Referral bonuses");
				break;
			}
		}
	}

	public int onBackPressed(){
		logDebug("onBackPressed");
		return 0;
	}

	private void updateUI() {
		if (context == null || sFetcher == null) return;

		ArrayList<ReferralBonus> bonuses = sFetcher.getReferralBonuses();
		if (bonuses.size() == 0) return;

		if (adapter == null) {
			adapter = new MegaReferralBonusesAdapter(context, this, bonuses, recyclerView);
		} else {
			adapter.setReferralBonuses(bonuses);
		}

		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onAchievementsReceived() {
		updateUI();
	}
}
