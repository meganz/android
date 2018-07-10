package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.flowlayoutmanager.Alignment;
import mega.privacy.android.app.components.flowlayoutmanager.FlowLayoutManager;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class InviteFriendsFragment extends Fragment implements OnClickListener{
	
	public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150; //in pixels

	Context context;
	ActionBar aB;
	int height;

	RelativeLayout parentRelativeLayout;
	RecyclerView recyclerView;

	LinearLayoutManager mLayoutManager_2;
	FlowLayoutManager mLayoutManager;
	MegaInviteFriendsAdapter adapter;
	EditText editTextMail;
	LinearLayout linearLayoutCard;
	Button inviteButton;
	private RelativeLayout emailErrorLayout;
	private Drawable editTextBackground;

	TextView titleCard;

	ArrayList<String> mails;
	
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

		height = outMetrics.heightPixels;
		int width = outMetrics.widthPixels;

		boolean enabledAchievements = megaApi.isAchievementsEnabled();
		log("The achievements are: "+enabledAchievements);

		View v = inflater.inflate(R.layout.fragment_invite_friends, container, false);
		recyclerView = (RecyclerView) v.findViewById(R.id.invite_friends_recycler_view);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN){

			mLayoutManager_2 = new LinearLayoutManager(context);
			recyclerView.setLayoutManager(mLayoutManager_2);

		}else{

			mLayoutManager = new FlowLayoutManager().setAlignment(Alignment.LEFT);
			mLayoutManager.setAutoMeasureEnabled(true);
			recyclerView.setLayoutManager(mLayoutManager);
		}

		recyclerView.setItemAnimator(new DefaultItemAnimator());

		inviteButton = (Button)v.findViewById(R.id.invite_button);
		inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));

		titleCard = (TextView) v.findViewById(R.id.title_card_invite_fragment);

		long referralsStorageValue = ((AchievementsActivity)context).megaAchievements.getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);
		long referralsTransferValue = ((AchievementsActivity)context).megaAchievements.getClassTransfer(MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE);

		titleCard.setText(getString(R.string.figures_achievements_text_referrals, Util.getSizeString(referralsStorageValue), Util.getSizeString(referralsTransferValue)));

		editTextMail = (EditText) v.findViewById(R.id.edit_text_invite_mail);
		editTextBackground = editTextMail.getBackground().mutate().getConstantState().newDrawable();

		editTextMail.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start,
										  int count, int after) {
				quitError();
			}

			public void onTextChanged(CharSequence s, int start,
									  int before, int count) {
				if (s != null) {
					if (s.length() > 0) {
						String temp = s.toString();
						char last = s.charAt(s.length()-1);
						if(last == ' '){
							temp = temp.trim();
							boolean isValid = isValidEmail(temp);
							if(isValid){
								addMail(temp.trim());
								editTextMail.getText().clear();
							}
							else{
								setError();
							}
						}
						else{
							log("Last character is: "+last);
						}
					}
				}
			}
		});

		emailErrorLayout = (RelativeLayout) v.findViewById(R.id.invite_friends_email_error);
		emailErrorLayout.setVisibility(View.GONE);

		mails = new ArrayList<>();


		if (adapter == null){
			adapter = new MegaInviteFriendsAdapter(context, this, mails, recyclerView);

		}
		recyclerView.setAdapter(adapter);

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		return v;
	}

	private void setError(){
		log("setError");
		emailErrorLayout.setVisibility(View.VISIBLE);
		PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
		Drawable background = editTextBackground.mutate().getConstantState().newDrawable();
		background.setColorFilter(porterDuffColorFilter);
		editTextMail.setBackground(background);
	}

	private void quitError(){
		if(emailErrorLayout.getVisibility() != View.GONE){
			log("quitError");
			emailErrorLayout.setVisibility(View.GONE);
			editTextMail.setBackground(editTextBackground);
		}
	}

	public final static boolean isValidEmail(CharSequence target) {
		if (target == null) {
			return false;
		} else {
			return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
		}
	}

	public void deleteMail(String mailToDelete){
		log("deleteMail: "+mailToDelete);
		int positionToRemove=-1;
		for(int i=0;i<mails.size();i++){
			if(mailToDelete.equals(mails.get(i))){
				positionToRemove = i;
				break;
			}
		}
		if(positionToRemove!=-1){
			mails.remove(positionToRemove);
			adapter.setNames(mails);
			adapter.notifyDataSetChanged();
		}
		log("deleteMail: positionToRemove: "+positionToRemove);
		if(mails.isEmpty()){
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
			inviteButton.setOnClickListener(null);
		}
		else{
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));
			inviteButton.setOnClickListener(this);
		}
	}

	public void deleteMail(int positionToDelete){
		log("deleteMail: "+positionToDelete);

		if(positionToDelete!=-1){
			mails.remove(positionToDelete);
			adapter.setNames(mails);
			adapter.notifyDataSetChanged();
		}

		if(mails.isEmpty()){
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
			inviteButton.setOnClickListener(null);
		}
		else{
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));
			inviteButton.setOnClickListener(this);
		}
	}

	public void addMail(String mail){
		log("addMail: "+mail);
		mails.add(mail);
		adapter.setNames(mails);

		if(mails.isEmpty()){
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
			inviteButton.setOnClickListener(null);
		}
		else{
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));
			inviteButton.setOnClickListener(this);
		}

		recyclerView.setVisibility(View.VISIBLE);



	}

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

			case R.id.invite_button:{
				log("Invite friends");
				((AchievementsActivity)context).inviteFriends(mails);
				editTextMail.getText().clear();
				mails.clear();
				adapter.setNames(mails);
				adapter.notifyDataSetChanged();
				break;
			}
		}
	}

	public int onBackPressed(){
		log("onBackPressed");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		((AchievementsActivity) context).showFragment(Constants.ACHIEVEMENTS_FRAGMENT, -1);
		return 0;
	}

	public static void log(String log) {
		Util.log("InviteFriendsFragment", log);
	}

}
