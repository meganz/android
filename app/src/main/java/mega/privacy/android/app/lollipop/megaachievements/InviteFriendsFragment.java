package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.flowlayoutmanager.Alignment;
import mega.privacy.android.app.components.flowlayoutmanager.FlowLayoutManager;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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
	ImageView toggleButtonMail;
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

	String inputString = null;

	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		((AchievementsActivity) context).setMails(mails);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");
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
		logDebug("The achievements are: " + enabledAchievements);

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

		titleCard.setText(getString(R.string.figures_achievements_text_referrals, getSizeString(referralsStorageValue), getSizeString(referralsTransferValue)));

		toggleButtonMail = (ImageView) v.findViewById(R.id.toggle_button_invite_mail);
		toggleButtonMail.setOnClickListener(this);
		editTextMail = (EditText) v.findViewById(R.id.edit_text_invite_mail);
		editTextBackground = editTextMail.getBackground().mutate().getConstantState().newDrawable();

		editTextMail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

				if (actionId == EditorInfo.IME_ACTION_DONE) {
					logDebug("first");
					String s =  v.getText().toString();
					inputString = v.getText().toString();
					if (s.isEmpty() || s.equals("null") || s.equals("")) {
						hideKeyboard((AchievementsActivity) context, 0);
					}
					else {
						boolean isValid = isValidEmail(s);
						if(isValid){
							addMail(s.trim());
							editTextMail.getText().clear();
							inputString = "";
						}
						else{
							setError();
						}
					}
					return true;
				}

				if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
					logDebug("second");
					if (mails.isEmpty()) {
						hideKeyboard((AchievementsActivity) context, 0);
					}
					else {
						((AchievementsActivity)context).inviteFriends(mails);
						editTextMail.getText().clear();
						mails.clear();
						adapter.setNames(mails);
						adapter.notifyDataSetChanged();
					}
					return true;
				}

				return false;
			}
		});

		editTextMail.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editTextMail.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				refreshKeyboard();
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
						inputString = s.toString();
						char last = s.charAt(s.length()-1);
						if(last == ' '){
							temp = temp.trim();
							boolean isValid = isValidEmail(temp);
							if(isValid){
								addMail(temp.trim());
								editTextMail.getText().clear();
								inputString = "";
							}
							else{
								setError();
							}
						}
						else{
							logDebug("Last character is: " + last);
						}
					}
					else {
						inputString = "";
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

		if (((AchievementsActivity) context).getMails() != null){
			mails = ((AchievementsActivity) context).getMails();
			adapter.setNames(mails);
		}

		if(mails.isEmpty()){
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.invite_button_deactivated));
			inviteButton.setOnClickListener(null);
		}
		else{
			inviteButton.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));
			inviteButton.setOnClickListener(this);
		}

		return v;
	}

	public void refreshKeyboard() {

		String s = inputString;
		int imeOptions = editTextMail.getImeOptions();

		if (s != null) {
			if (s.length() == 0 && !mails.isEmpty()){
				editTextMail.setImeOptions(EditorInfo.IME_ACTION_SEND);
			}
			else {
				editTextMail.setImeOptions(EditorInfo.IME_ACTION_DONE);
			}
		}
		else if (!mails.isEmpty()) {
			editTextMail.setImeOptions(EditorInfo.IME_ACTION_SEND);
		}
		else {
			editTextMail.setImeOptions(EditorInfo.IME_ACTION_DONE);
		}

		int imeOptionsNew = editTextMail.getImeOptions();
		if (imeOptions != imeOptionsNew) {
			View view = ((AchievementsActivity) context).getCurrentFocus();
			if (view != null) {
				InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.restartInput(view);
			}
		}
	}

	private void setError(){
		logDebug("setError");
		emailErrorLayout.setVisibility(View.VISIBLE);
		PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
		Drawable background = editTextBackground.mutate().getConstantState().newDrawable();
		background.setColorFilter(porterDuffColorFilter);
		editTextMail.setBackground(background);
	}

	private void quitError(){
		if(emailErrorLayout.getVisibility() != View.GONE){
			logDebug("quitError");
			emailErrorLayout.setVisibility(View.GONE);
			editTextMail.setBackground(editTextBackground);
		}
	}

	public final static boolean isValidEmail(CharSequence target) {
		if (target == null) {
			return false;
		} else {
			return EMAIL_ADDRESS.matcher(target).matches();
		}
	}

	public void deleteMail(String mailToDelete){
		logDebug("Mail to delete: " + mailToDelete);
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
		logDebug("positionToRemove: " + positionToRemove);
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
		logDebug("positionToDelete: " + positionToDelete);

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
		refreshKeyboard();
	}

	public void addMail(String mail){
		logDebug("mail: " + mail);
		if (mails.contains(mail)){
			((AchievementsActivity) context).showSnackbar(context.getString(R.string.contact_not_added));
		}
		else {
			mails.add(mail);
		}
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
		refreshKeyboard();
	}

	@Override
	public void onAttach(Activity activity) {
		logDebug("onAttach");
		super.onAttach(activity);
		context = activity;
		aB = ((AppCompatActivity)activity).getSupportActionBar();
	}

	@Override
	public void onAttach(Context context) {
		logDebug("onAttach context");
		super.onAttach(context);
		this.context = context;
		aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");
		switch (v.getId()) {

			case R.id.invite_button:{
				logDebug("Invite friends");
				((AchievementsActivity)context).inviteFriends(mails);
				editTextMail.getText().clear();
				mails.clear();
				adapter.setNames(mails);
				adapter.notifyDataSetChanged();
				break;
			}
			case R.id.toggle_button_invite_mail: {
				Intent intent = new Intent(context, AddContactActivityLollipop.class);
				intent.putExtra("contactType", CONTACT_TYPE_DEVICE);
				intent.putExtra("fromAchievements", true);
				intent.putStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS, mails);
				((AchievementsActivity)context).startActivityForResult(intent, REQUEST_CODE_GET_CONTACTS);
				break;
			}
		}
	}

	public int onBackPressed(){
		logDebug("onBackPressed");

		((AchievementsActivity) context).showFragment(ACHIEVEMENTS_FRAGMENT, -1);
		return 0;
	}
}
