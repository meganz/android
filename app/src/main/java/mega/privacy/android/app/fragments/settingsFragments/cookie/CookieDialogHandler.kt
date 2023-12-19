package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.app.utils.ContextUtils.isValid
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.setting.CheckCookieBannerEnabledUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Class to handle cookie dialog.
 * It will show cookie dialog when cookie banner enabled and cookie settings is empty.
 * It will dismiss cookie dialog when cookie banner disabled or cookie settings is not empty.
 *
 * @property getCookieSettingsUseCase                       Get cookie settings use case.
 * @property updateCookieSettingsUseCase                    Update cookie settings use case.
 * @property checkCookieBannerEnabledUseCase                Check cookie banner enabled use case.
 * @property updateCrashAndPerformanceReportersUseCase      Update crash and performance reporters use case.
 */
class CookieDialogHandler @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val checkCookieBannerEnabledUseCase: CheckCookieBannerEnabledUseCase,
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) {

    private var dialog: AlertDialog? = null

    /**
     * Show cookie dialog if needed.
     *
     * @param context   View context for the Dialog to be shown.
     * @param recreate  Dismiss current dialog and create a new instance.
     */
    @JvmOverloads
    fun showDialogIfNeeded(context: Context, recreate: Boolean = false) {
        if (recreate) dialog?.dismiss()

        checkDialogSettings { showDialog ->
            if (showDialog) {
                createDialog(context)
            } else {
                dialog?.dismiss()
            }
        }
    }

    private fun checkDialogSettings(action: (Boolean) -> Unit) {
        applicationScope.launch(ioDispatcher) {
            runCatching {
                // Check cookieBannerEnabled SDK boolean flag
                val isCookieBannerEnabled = checkCookieBannerEnabledUseCase()
                // Check existing cookie settings
                val cookieSettings = getCookieSettingsUseCase()
                withContext(mainDispatcher) {
                    val showDialog = isCookieBannerEnabled && cookieSettings.isEmpty()
                    action.invoke(showDialog)
                }
            }.onFailure {
                Timber.e("failed to check cookie banner settings: $it")
            }
        }
    }

    private fun createDialog(context: Context) {
        if (dialog?.isShowing == true || !context.isValid()) return

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(R.layout.dialog_cookie_alert)
            .setPositiveButton(context.getString(R.string.preference_cookies_accept)) { _, _ ->
                acceptAllCookies()
            }
            .setNegativeButton(context.getString(R.string.settings_about_cookie_settings)) { _, _ ->
                context.startActivity(Intent(context, CookiePreferencesActivity::class.java))
            }
            .create()
            .apply {
                setOnShowListener {
                    val message =
                        context.getString(R.string.dialog_cookie_alert_message)
                            .replace("[A]", "<a href='https://mega.nz/cookie'>")
                            .replace("[/A]", "</a>")
                            .toSpannedHtmlText()

                    findViewById<TextView>(R.id.message)?.apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = message
                    }
                }
            }.also { it.show() }
    }

    /**
     * function to create a Cookie dialog where Ads cookies will be mentioned specifically
     * this dialog will be shown when the user will be part of Advertisement experiment and will see the external Ads
     */
    private fun createCookieDialogWithAds(context: Context) {
        if (dialog?.isShowing == true || !context.isValid()) return

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(R.layout.dialog_cookie_alert)
            .setPositiveButton(context.getString(R.string.preference_cookies_accept)) { _, _ ->
                acceptAllCookies()
            }
            .setNegativeButton(context.getString(R.string.settings_about_cookie_settings)) { _, _ ->
                context.startActivity(Intent(context, CookiePreferencesActivity::class.java))
            }
            .create()
            .apply {
                setOnShowListener {
                    val message =
                        context.getString(R.string.dialog_ads_cookie_alert_message)
                            .replace("[A]", "<a href='https://mega.nz/cookie'>")
                            .replace("[/A]", "</a>")
                            .toSpannedHtmlText()

                    findViewById<TextView>(R.id.message)?.apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = message
                    }
                }
            }.also { it.show() }
    }

    private fun acceptAllCookies() {
        applicationScope.launch {
            runCatching {
                updateCookieSettingsUseCase(CookieType.entries.toSet())
                updateCrashAndPerformanceReportersUseCase()
            }.onFailure { Timber.e("failed to accept all cookies: $it") }
        }
    }

    /**
     * Show dialog when view is resumed.
     */
    fun onResume() {
        if (dialog?.isShowing == true) {
            checkDialogSettings { showDialog ->
                if (!showDialog) dialog?.dismiss()
            }
        }
    }

    /**
     * Dismiss dialog when view is destroyed.
     */
    fun onDestroy() {
        dialog?.dismiss()
        dialog = null
    }
}
