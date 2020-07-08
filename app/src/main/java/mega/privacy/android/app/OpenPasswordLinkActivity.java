package mega.privacy.android.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import mega.privacy.android.app.lollipop.DecryptAlertDialog;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK;
import static mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEXS;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class OpenPasswordLinkActivity extends PinActivityLollipop
		implements MegaRequestListenerInterface, DecryptAlertDialog.DecryptDialogListener {
	private static final String TAG_DECRYPT = "decrypt";

	private MegaApiAndroid megaApi;

	private String url;
	private String key;

	private ProgressDialog statusDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		statusDialog = new ProgressDialog(this);
		statusDialog.setMessage(getString(R.string.general_loading));

		megaApi = ((MegaApplication) getApplication()).getMegaApi();

		Intent intent = getIntent();
		if (intent != null) {
			url = intent.getDataString();

			askForPasswordDialog();
		}
	}

	@Override
	public void onDestroy() {
		if (megaApi != null) {
			megaApi.removeRequestListener(this);
		}

		statusDialog.dismiss();
		super.onDestroy();
	}

	public void askForPasswordDialog() {
		logDebug("askForPasswordDialog");

		new DecryptAlertDialog.Builder()
				.setListener(this)
				.setTitle(getString(R.string.hint_set_password_protection_dialog))
				.setPosText(R.string.general_decryp)
				.setNegText(R.string.general_cancel)
				.setErrorMessage(R.string.invalid_link_password)
				.setKey(key)
				.setShownPassword(true)
				.build()
				.show(getSupportFragmentManager(), TAG_DECRYPT);
	}

	private void decrypt() {
		if (TextUtils.isEmpty(key)) {
			return;
		}

		statusDialog.show();

		megaApi.decryptPasswordProtectedLink(url, key, this);
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logDebug("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_PASSWORD_LINK) {
			statusDialog.dismiss();

			if (e.getErrorCode() == MegaError.API_OK) {

				String decryptedLink = request.getText();

				// Folder Download link
				if (matchRegexs(decryptedLink, FOLDER_LINK_REGEXS)) {
					logDebug("Folder link url");

					Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
					openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
					openFolderIntent.setData(Uri.parse(decryptedLink));
					startActivity(openFolderIntent);
					finish();
				} else if (matchRegexs(decryptedLink, FILE_LINK_REGEXS)) {
					logDebug("Open link url");

					Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
					openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
					openFileIntent.setData(Uri.parse(decryptedLink));
					startActivity(openFileIntent);
					finish();
				}
			} else {
				logError("ERROR: " + e.getErrorCode());
				askForPasswordDialog();
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onDialogPositiveClick(String key) {
		this.key = key;
		decrypt();
	}

	@Override
	public void onDialogNegativeClick() {
		finish();
	}
}