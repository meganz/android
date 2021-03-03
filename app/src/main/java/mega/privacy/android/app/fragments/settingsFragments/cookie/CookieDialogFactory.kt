package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.UpdateCookieSettingsUseCase
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import javax.inject.Inject

class CookieDialogFactory @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase
) {

    fun showDialogIfNeeded(context: Context) {
        if (MegaApplication.isCookieBannerEnabled()) {
            getCookieSettingsUseCase.shouldShowDialog()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { showDialog ->
                        if (showDialog) createDialog(context)
                    },
                    onError = { error ->
                        LogUtil.logError(error.message)
                    }
                )
        }
    }

    private fun createDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setCancelable(false)
            .setView(R.layout.dialog_cookie_alert)
            .setPositiveButton(R.string.preference_cookies_accept) { _, _ ->
                updateCookieSettingsUseCase.acceptAll()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onComplete = {
                            (context.applicationContext as MegaApplication).checkEnabledCookies()
                        },
                        onError = { error ->
                            LogUtil.logError(error.message)
                        }
                    )
            }
            .setNegativeButton(R.string.settings_about_cookie_settings) { _, _ ->
                context.startActivity(Intent(context, CookiePreferencesActivity::class.java))
            }
            .create()

        dialog.setOnShowListener {
            val message = context.getString(R.string.dialog_cookie_alert_message)
                .replace("[A]", "<a href='https://mega.nz/cookie'>")
                .replace("[/A]", "</a>")
                .toSpannedHtmlText()

            dialog.findViewById<TextView>(R.id.message)?.apply {
                movementMethod = LinkMovementMethod.getInstance()
                text = message
            }
        }

        dialog.show()
    }
}
