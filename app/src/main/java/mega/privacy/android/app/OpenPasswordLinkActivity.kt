package mega.privacy.android.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.DecryptAlertDialog.DecryptDialogListener
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber

class OpenPasswordLinkActivity : PasscodeActivity(), DecryptDialogListener {
    private var progressBar: ProgressBar? = null

    private var url: String? = null
    private var key: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContentView(R.layout.activity_open_password_link)
        progressBar = findViewById(R.id.progress)

        val intent = intent
        if (intent != null) {
            url = intent.dataString

            askForPasswordDialog()
        }
    }

    public override fun onDestroy() {
        progressBar?.visibility = View.GONE
        super.onDestroy()
    }

    private fun askForPasswordDialog() {
        Timber.d("askForPasswordDialog")

        DecryptAlertDialog.Builder()
            .setTitle(getString(R.string.hint_set_password_protection_dialog))
            .setPosText(R.string.general_decryp)
            .setNegText(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button)
            .setErrorMessage(R.string.invalid_link_password)
            .setKey(key)
            .setShownPassword(true)
            .build()
            .show(supportFragmentManager, TAG_DECRYPT)
    }

    private fun decrypt() {
        if (TextUtils.isEmpty(key)) {
            return
        }

        progressBar?.visibility = View.VISIBLE
        megaApi.decryptPasswordProtectedLink(
            url,
            key,
            object : OptionalMegaRequestListenerInterface() {
                override fun onRequestFinish(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError,
                ) {
                    super.onRequestFinish(api, request, error)
                    if (request.type == MegaRequest.TYPE_PASSWORD_LINK) {
                        managePasswordLinkRequest(error, request.text)
                    }
                }
            })
    }

    override fun onDialogPositiveClick(key: String?) {
        this.key = key
        decrypt()
    }

    override fun onDialogNegativeClick() {
        finish()
    }

    fun managePasswordLinkRequest(e: MegaError, decryptedLink: String?) {
        Timber.d("onRequestFinish")
        progressBar?.visibility = View.GONE

        if (e.errorCode == MegaError.API_OK && !TextUtil.isTextEmpty(decryptedLink)) {
            var intent: Intent? = null

            if (Util.matchRegexs(decryptedLink, Constants.FOLDER_LINK_REGEXS)) {
                Timber.d("Folder link url")
                intent = Intent(
                    this@OpenPasswordLinkActivity,
                    FolderLinkComposeActivity::class.java
                )
                intent.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)
            } else if (Util.matchRegexs(decryptedLink, Constants.FILE_LINK_REGEXS)) {
                Timber.d("Open link url")
                intent = Intent(
                    this@OpenPasswordLinkActivity,
                    FileLinkComposeActivity::class.java
                )
                intent.setAction(Constants.ACTION_OPEN_MEGA_LINK)
            }

            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.setData(Uri.parse(decryptedLink))
                startActivity(intent)
                finish()
            }
        } else {
            Timber.e("ERROR: %s", e.errorCode)
            askForPasswordDialog()
        }
    }

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}