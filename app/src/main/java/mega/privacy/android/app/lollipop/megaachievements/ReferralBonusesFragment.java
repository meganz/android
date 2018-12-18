package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class ReferralBonusesFragment extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;

	RelativeLayout parentRelativeLayout;
	RecyclerView recyclerView;
	LinearLayoutManager mLayoutManager;
	MegaReferralBonusesAdapter adapter;
	
	DisplayMetrics outMetrics;
	float density;

	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

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

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density = ((Activity) context).getResources().getDisplayMetrics().density;

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		log("The achievements are: "+enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_referral_bonuses, container, false);

		parentRelativeLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_relative_layout);

		recyclerView = (RecyclerView) v.findViewById(R.id.referral_bonuses_recycler_view);
		recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
		mLayoutManager = new LinearLayoutManager(context);
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		if (adapter == null){
			adapter = new MegaReferralBonusesAdapter(context, this, ((AchievementsActivity)context).referralBonuses, recyclerView);
		}
		else{
			adapter.setReferralBonuses(((AchievementsActivity)context).referralBonuses);
		}


		recyclerView.setAdapter(adapter);
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

				break;
			}
			case R.id.card_view_invite_friends:{
				log("Invite friends");

				break;
			}

		}
	}

	public int onBackPressed(){
		log("onBackPressed");
		return 0;
	}

	public static void log(String log) {
		Util.log("ReferralBonusesFragment", log);
	}

}
