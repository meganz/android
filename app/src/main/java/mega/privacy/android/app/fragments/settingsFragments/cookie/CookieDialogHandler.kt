package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.app.featuretoggle.ABTestFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.utils.ContextUtils.isValid
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.settings.cookie.CookieDialog
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.setting.BroadcastCookieSettingsSavedUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieDialogUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Class to handle cookie dialog.
 *
 * @property updateCookieSettingsUseCase                Use Case to update cookie settings.
 * @property broadcastCookieSettingsSavedUseCase        Use Case to broadcast cookie settings saved.
 * @property updateCrashAndPerformanceReportersUseCase  Use Case to update crash and performance reporters.
 * @property getCookieDialogUseCase                     Use Case to get cookie dialog type.
 * @property applicationScope                           Scope for the Coroutine launched by the Use Case.
 * @property ioDispatcher                              Dispatcher for the Coroutine launched by the Use Case to perform background operations.
 * @property mainDispatcher                            Dispatcher for the Coroutine launched by the Use Case to perform operations on the main thread.
 */
class CookieDialogHandler @Inject constructor(
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val broadcastCookieSettingsSavedUseCase: BroadcastCookieSettingsSavedUseCase,
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase,
    private val getCookieDialogUseCase: GetCookieDialogUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) {

    private var dialog: AlertDialog? = null
    private var isCookieDialogWithAds = false
    private var getCookieDialogJob: Job? = null

    /**
     * Show cookie dialog if needed.
     *
     * @param context   View context for the Dialog to be shown.
     */
    fun showDialogIfNeeded(context: Context) {

        checkDialogSettings { state ->
            when (state.dialogType) {
                CookieDialogType.GenericCookieDialog -> createGenericCookieDialog(
                    context = context,
                    url = state.url,
                )

                CookieDialogType.CookieDialogWithAds -> createCookieDialogWithAds(
                    context = context,
                    url = state.url,
                )

                else -> dialog?.dismiss()
            }
        }
    }

    private fun checkDialogSettings(action: (CookieDialog) -> Unit) {
        getCookieDialogJob?.cancel()
        getCookieDialogJob = applicationScope.launch(ioDispatcher) {
            runCatching {
                val cookieDialog = getCookieDialogUseCase(
                    AppFeatures.InAppAdvertisement,
                    ABTestFeatures.ads,
                    ABTestFeatures.adse
                )
                withContext(mainDispatcher) {
                    action(cookieDialog)
                }
            }.onFailure {
                Timber.e("failed to check cookie dialog settings: $it")
            }
        }
    }

    private fun createGenericCookieDialog(context: Context, url: String?) {
        isCookieDialogWithAds = false
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
                            .replace("[A]", "<a href='$url'>")
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
    private fun createCookieDialogWithAds(context: Context, url: String?) {
        isCookieDialogWithAds = true
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
                            .replace("[A]", "<a href='$url'>")
                            .replace("[/A]", "</a>")
                            .toSpannedHtmlText()

                    findViewById<TextView>(R.id.message)?.apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        text = message
                        setOnClickListener {
                            if (url == null) Util.showSnackbar(
                                context,
                                context.getString(R.string.general_something_went_wrong_error)
                            )
                        }
                    }
                }
            }.also { it.show() }
    }

    private fun acceptAllCookies() {
        applicationScope.launch {
            runCatching {
                // If the user accepts all cookies, we will enable all the cookies,
                // including the Ads cookies else, enable all the cookies except the Ads cookies
                val enabledCookies = if (isCookieDialogWithAds) {
                    CookieType.entries.toSet()
                } else {
                    CookieType.entries.toSet() - CookieType.ADS_CHECK - CookieType.ADVERTISEMENT
                }
                updateCookieSettingsUseCase(enabledCookies)
                broadcastCookieSettingsSavedUseCase(enabledCookies)
                updateCrashAndPerformanceReportersUseCase()
            }.onFailure { Timber.e("failed to accept all cookies: $it") }
        }
    }

    /**
     * Show dialog when view is resumed.
     */
    fun onResume() {
        if (dialog?.isShowing == true) {
            checkDialogSettings { state ->
                if (state.dialogType == CookieDialogType.None) {
                    dialog?.dismiss()
                }
            }
        }
    }

    /**
     * Dismiss dialog when view is destroyed.
     */
    fun onDestroy() {
        getCookieDialogJob?.cancel()
        dialog?.dismiss()
        dialog = null
    }
}
