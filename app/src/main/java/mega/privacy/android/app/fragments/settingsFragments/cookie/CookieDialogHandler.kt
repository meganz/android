package mega.privacy.android.app.fragments.settingsFragments.cookie

import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.CheckCookieBannerEnabledUseCase
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.UpdateCookieSettingsUseCase
import mega.privacy.android.app.utils.ContextUtils.isValid
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import timber.log.Timber
import javax.inject.Inject

/**
 * Cookie dialog handler class to manage Cookie Dialog visibility based on view's lifecycle.
 */
class CookieDialogHandler @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val checkCookieBannerEnabledUseCase: CheckCookieBannerEnabledUseCase,
) : LifecycleEventObserver {

    private val rxSubscriptions = CompositeDisposable()
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

    /**
     * Check SDK flag and existing cookie settings.
     *
     * @param action    Action to be invoked with the Boolean result
     */
    private fun checkDialogSettings(action: (Boolean) -> Unit) {
        rxSubscriptions.clear()

        checkCookieBannerEnabledUseCase.check()
            .concatMap { getCookieSettingsUseCase.shouldShowDialog() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { showDialog ->
                    action.invoke(showDialog)
                },
                onError = Timber::e
            )
            .addTo(rxSubscriptions)
    }

    private fun createDialog(context: Context) {
        if (dialog?.isShowing == true || !context.isValid()) return

        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setView(R.layout.dialog_cookie_alert)
            .setPositiveButton(context.getString(R.string.preference_cookies_accept)) { _, _ ->
                acceptAllCookies(context)
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
                acceptAllCookies(context)
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

    private fun acceptAllCookies(context: Context) {
        updateCookieSettingsUseCase.acceptAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    if (context.isValid()) {
                        (context.applicationContext as MegaApplication).checkEnabledCookies()
                    }
                },
                onError = Timber::e
            )
            .addTo(rxSubscriptions)
    }

    fun onResume() {
        if (dialog?.isShowing == true) {
            checkDialogSettings { showDialog ->
                if (!showDialog) dialog?.dismiss()
            }
        }
    }

    fun onDestroy() {
        rxSubscriptions.clear()
        dialog?.dismiss()
        dialog = null
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> return
        }
    }
}
