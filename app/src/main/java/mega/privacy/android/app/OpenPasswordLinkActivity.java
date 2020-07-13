package mega.privacy.android.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import mega.privacy.android.app.listeners.BaseListener;
import mega.privacy.android.app.lollipop.DecryptAlertDialog;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
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
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class OpenPasswordLinkActivity extends PinActivityLollipop
		implements DecryptAlertDialog.DecryptDialogListener {
	private static final String TAG_DECRYPT = "decrypt";

	private ProgressBar progressBar;

	private String url;
	private String key;

	private MegaRequestListenerInterface decryptPasswordProtectedLinkListener =
			new BaseListener(this) {
				@Override
				public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
					logDebug("onRequestFinish: " + request.getRequestString());

					if (progressBar != null) {
						progressBar.setVisibility(View.GONE);
					}
					if (request.getType() == MegaRequest.TYPE_PASSWORD_LINK) {
						if (e.getErrorCode() == MegaError.API_OK) {

							String decryptedLink = request.getText();

							// Folder Download link
							if (matchRegexs(decryptedLink, FOLDER_LINK_REGEXS)) {
								logDebug("Folder link url");

								Intent openFolderIntent = new Intent(OpenPasswordLinkActivity.this,
										FolderLinkActivityLollipop.class);
								openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
								openFolderIntent.setData(Uri.parse(decryptedLink));
								startActivity(openFolderIntent);
								finish();
							} else if (matchRegexs(decryptedLink, FILE_LINK_REGEXS)) {
								logDebug("Open link url");

								Intent openFileIntent = new Intent(OpenPasswordLinkActivity.this,
										FileLinkActivityLollipop.class);
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
			};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logDebug("onCreate");
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		setContentView(R.layout.activity_open_password_link);
		progressBar = findViewById(R.id.progress);

		Intent intent = getIntent();
		if (intent != null) {
			url = intent.getDataString();

			askForPasswordDialog();
		}
	}

	@Override
	public void onDestroy() {
		if (megaApi != null) {
			megaApi.removeRequestListener(decryptPasswordProtectedLinkListener);
		}

		progressBar.setVisibility(View.GONE);
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

		progressBar.setVisibility(View.VISIBLE);
		megaApi.decryptPasswordProtectedLink(url, key, decryptPasswordProtectedLinkListener);
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