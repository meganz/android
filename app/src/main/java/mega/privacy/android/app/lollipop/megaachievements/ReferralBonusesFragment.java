package mega.privacy.android.app.lollipop.megaachievements;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class ReferralBonusesFragment extends Fragment implements OnClickListener, AchievementsActivity.Callback{
	ActionBar aB;

	RelativeLayout parentRelativeLayout;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	MegaReferralBonusesAdapter adapter;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;

	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) getActivity().getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		Display display = getActivity().getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = getActivity().getResources().getDisplayMetrics().density;

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		logDebug("The achievements are: " + enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_referral_bonuses, container, false);

		parentRelativeLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_relative_layout);

		recyclerView = (RecyclerView) v.findViewById(R.id.referral_bonuses_recycler_view);
		recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext(), outMetrics));
		mLayoutManager = new LinearLayoutManager(getContext());
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		return v;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Activity actionbar has been created which might be accessed by UpdateUI().
		aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
		// The root view has been created, fill it with the data when data ready
		((AchievementsActivity)getActivity()).setCallback(this);
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
		Context context = getContext();
		if (AchievementsActivity.sReferralBonuses.size() == 0 || context == null) return;

		if (adapter == null) {
			adapter = new MegaReferralBonusesAdapter(context, this, AchievementsActivity.sReferralBonuses, recyclerView);
		} else {
			adapter.setReferralBonuses(AchievementsActivity.sReferralBonuses);
		}

		recyclerView.setAdapter(adapter);
	}

	@Override
	public void onAchievementsReceived() {
		updateUI();
	}
}
