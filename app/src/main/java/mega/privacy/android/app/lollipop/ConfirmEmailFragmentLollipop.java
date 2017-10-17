package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class ConfirmEmailFragmentLollipop extends Fragment implements MegaRequestListenerInterface, View.OnClickListener {

	Context context;

	MegaApiAndroid megaApi;

	String emailTemp = null;
	String passwdTemp = null;
	String firstNameTemp = null;

	ImageView icon;
	EditText et_newEmail;
	Button resendButton;
	TextView awaiting;
	TextView explanation;
	TextView mispelled;


	@Override
	public void onAttach(Activity context) {
		log("onAttach Activity");
		super.onAttach(context);
		this.context = context;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		log("onCreate");
		super.onCreate(savedInstanceState);

		if(context==null){
			log("context is null");
			return;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

//		megaApi.sendSignupLink("signuplink3@yopmail.com", nameTemp, passwdTemp, this);

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_confirm_email, container, false);

		icon = (ImageView) v.findViewById(R.id.confirm_email_icon);
		awaiting = (TextView) v.findViewById(R.id.confirm_email_awaiting);
		explanation = (TextView) v.findViewById(R.id.confirm_email_explanation);
		et_newEmail = (EditText) v.findViewById(R.id.confirm_email_new_email);
		mispelled = (TextView) v.findViewById(R.id.confirm_email_misspelled);
		resendButton = (Button) v.findViewById(R.id.confirm_email_new_email_resend);

		et_newEmail.setCursorVisible(true);
		et_newEmail.setText(emailTemp);
		et_newEmail.requestFocus();

		resendButton.setOnClickListener(this);

		return v;
	}

	public void setPasswdTemp(String passwdTemp){
		this.passwdTemp = passwdTemp;
	}

	public String getPasswdTemp(){
		return this.passwdTemp;
	}

	public void setEmailTemp(String emailTemp){
		this.emailTemp = emailTemp;
	}

	public String getEmailTemp(){
		return this.emailTemp;
	}

	public void setFirstNameTemp(String firstNameTemp){
		this.firstNameTemp = firstNameTemp;
	}

	public String getFirstNameTemp(){
		return this.firstNameTemp;
	}

	public static void log(String log) {
		Util.log("ConfirmEmailFragmentLollipop", log);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart - " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate - " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish - " + request.getRequestString() + "_" + e.getErrorCode());

		if (isAdded()) {
			log("isAdded true");
			if (e.getErrorCode() == MegaError.API_OK) {
				((LoginActivityLollipop) context).showSnackbar(getString(R.string.confirm_email_misspelled_email_sent));
			} else {
				((LoginActivityLollipop) context).showSnackbar(e.getErrorString());
			}
		}
		else{
			log("isAdded false");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestTemporaryError - " + request.getRequestString());
	}

	@Override
	public void onClick(View v) {
		log("onClick");

		switch (v.getId()){
			case R.id.confirm_email_new_email_resend:{
				((LoginActivityLollipop)context).setEmailTemp(et_newEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim());
				megaApi.sendSignupLink(et_newEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim(), firstNameTemp, passwdTemp, this);
				break;
			}
		}
	}
}
