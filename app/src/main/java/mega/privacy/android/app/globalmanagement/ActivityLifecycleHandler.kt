package mega.privacy.android.app.globalmanagement

import android.app.Activity
import android.app.Application
import android.os.Bundle
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Activity lifecycle handler
 */
@Singleton
class ActivityLifecycleHandler @Inject constructor() : Application.ActivityLifecycleCallbacks {
    // The current App Activity
    private var currentActivity: Activity? = null

    // Attributes to detect if app changes between background and foreground
    // Keep the count of number of Activities in the started state
    private var activityReferences = 0

    // Flag to indicate if the current Activity is going through configuration change like orientation switch
    private var isActivityChangingConfigurations = false

    /**
     * On activity created
     *
     * @param activity
     * @param savedInstanceState
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    /**
     * On activity started
     *
     * @param activity
     */
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            Timber.i("App enters foreground")
            if (MegaApplication.getInstance().storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning()
            }
        }
    }

    /**
     * On activity resumed
     *
     * @param activity
     */
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    /**
     * On activity paused
     *
     * @param activity
     */
    override fun onActivityPaused(activity: Activity) {
        currentActivity = null
    }

    /**
     * On activity stopped
     *
     * @param activity
     */
    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            Timber.i("App enters background")
        }
        currentActivity = null
    }

    /**
     * On activity save instance state
     *
     * @param activity
     * @param outState
     */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    /**
     * On activity destroyed
     *
     * @param activity
     */
    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Get current activity
     *
     * @return current visible activity
     */
    fun getCurrentActivity(): Activity? = currentActivity

    /**
     * Is activity visible
     */
    val isActivityVisible: Boolean
        get() = currentActivity != null
}