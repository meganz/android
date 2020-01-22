package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ConfirmEmailFragmentLollipop extends Fragment implements MegaRequestListenerInterface, View.OnClickListener {

	private Context context;

	private MegaApiAndroid megaApi;

	private String emailTemp = null;
	private String passwdTemp = null;
	private String firstNameTemp = null;

	private TextInputLayout newEmailLayout;
	private AppCompatEditText newEmail;
	private ImageView errorNewEmail;
	private TextView misspelt;
	private Button resendButton;
	private Button cancelButton;

	@Override
	public void onAttach(Activity context) {
		logDebug("onAttach Activity");
		super.onAttach(context);
		this.context = context;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}

	@Override
	public void onCreate (Bundle savedInstanceState){
		logDebug("onCreate");
		super.onCreate(savedInstanceState);

		if(context==null){
			logDebug("Context is null");
			return;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		View v = inflater.inflate(R.layout.fragment_confirm_email, container, false);

		newEmailLayout = v.findViewById(R.id.confirm_email_new_email_layout);
		newEmail = v.findViewById(R.id.confirm_email_new_email);
		newEmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				quitEmailError();
			}
		});
		errorNewEmail = v.findViewById(R.id.confirm_email_new_email_error_icon);
		errorNewEmail.setVisibility(View.GONE);
		misspelt = v.findViewById(R.id.confirm_email_misspelled);
		resendButton = v.findViewById(R.id.confirm_email_new_email_resend);
		cancelButton = v.findViewById(R.id.confirm_email_cancel);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			resendButton.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
		}

		String textMispelled = String.format(getString(R.string.confirm_email_misspelled));
		try {
			textMispelled = textMispelled.replace("[A]", "<b>");
			textMispelled = textMispelled.replace("[/A]", "</b>");
		}
		catch(Exception e){
		}

		Spanned result = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			result = Html.fromHtml(textMispelled,Html.FROM_HTML_MODE_LEGACY);
		} else {
			result = Html.fromHtml(textMispelled);
		}
		misspelt.setText(result);

		newEmail.setCursorVisible(true);
		newEmail.setText(emailTemp);
		newEmail.requestFocus();

		resendButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

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

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart - " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate - " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish - " + request.getRequestString() + "_" + e.getErrorCode());

		if (isAdded()) {
			logDebug("isAdded true");
			if (e.getErrorCode() == MegaError.API_OK) {
				((LoginActivityLollipop) context).showSnackbar(getString(R.string.confirm_email_misspelled_email_sent));
			} else {
				((LoginActivityLollipop) context).showSnackbar(e.getErrorString());
			}
		}
		else{
			logDebug("isAdded false");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
		logWarning("onRequestTemporaryError - " + request.getRequestString());
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		switch (v.getId()){
			case R.id.confirm_email_new_email_resend:{
				submitForm();
				break;
			}
			case R.id.confirm_email_cancel:{
				megaApi.cancelCreateAccount(this);
				((LoginActivityLollipop)context).cancelConfirmationAccount();
				break;
			}
		}
	}

	private void submitForm() {
		if (!validateForm()) {
			return;
		}

		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(newEmail.getWindowToken(), 0);

		if (!isOnline(context)) {
			((LoginActivityLollipop) context).showSnackbar(getString(R.string.error_server_connection_problem));
			return;
		}

		String email = newEmail.getText().toString().toLowerCase(Locale.ENGLISH).trim();
		((LoginActivityLollipop) context).setEmailTemp(email);
		megaApi.sendSignupLink(email, firstNameTemp, passwdTemp, this);
	}

	private boolean validateForm() {
		String emailError = getEmailError();

		if (emailError != null && !emailError.isEmpty()) {
			newEmailLayout.setError(emailError);
			newEmailLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
			errorNewEmail.setVisibility(View.VISIBLE);
			return false;
		}

		return true;
	}

	private String getEmailError() {
		String value = newEmail.getText().toString();
		if (value.length() == 0) {
			return getString(R.string.error_enter_email);
		}
		if (!EMAIL_ADDRESS.matcher(value).matches()) {
			return getString(R.string.error_invalid_email);
		}
		return null;
	}

	private void quitEmailError() {
		newEmailLayout.setError(null);
		newEmailLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
		errorNewEmail.setVisibility(View.GONE);
	}
}
