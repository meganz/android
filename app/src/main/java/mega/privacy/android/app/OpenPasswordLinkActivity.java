package mega.privacy.android.app;

import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK;
import static mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS;
import static mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEXS;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.matchRegexs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.listeners.PasswordLinkListener;
import mega.privacy.android.app.main.DecryptAlertDialog;
import mega.privacy.android.app.main.FileLinkActivity;
import mega.privacy.android.app.main.FolderLinkActivity;
import nz.mega.sdk.MegaError;
import timber.log.Timber;

public class OpenPasswordLinkActivity extends PasscodeActivity
        implements DecryptAlertDialog.DecryptDialogListener {
    private static final String TAG_DECRYPT = "decrypt";

    private ProgressBar progressBar;

    private String url;
    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
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
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        super.onDestroy();
    }

    public void askForPasswordDialog() {
        Timber.d("askForPasswordDialog");

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
        megaApi.decryptPasswordProtectedLink(url, key, new PasswordLinkListener(this));
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

    public void managePasswordLinkRequest(MegaError e, String decryptedLink) {
        Timber.d("onRequestFinish");
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (e.getErrorCode() == MegaError.API_OK && !isTextEmpty(decryptedLink)) {
            Intent intent = null;

            if (matchRegexs(decryptedLink, FOLDER_LINK_REGEXS)) {
                Timber.d("Folder link url");
                intent = new Intent(OpenPasswordLinkActivity.this, FolderLinkActivity.class);
                intent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
            } else if (matchRegexs(decryptedLink, FILE_LINK_REGEXS)) {
                Timber.d("Open link url");
                intent = new Intent(OpenPasswordLinkActivity.this, FileLinkActivity.class);
                intent.setAction(ACTION_OPEN_MEGA_LINK);
            }

            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setData(Uri.parse(decryptedLink));
                startActivity(intent);
                finish();
            }
        } else {
            Timber.e("ERROR: %s", e.getErrorCode());
            askForPasswordDialog();
        }
    }
}