package mega.privacy.android.app.fcm

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager for FCM related operations
 */
class FcmManager @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val firebaseMessaging: FirebaseMessaging,
    private val firebaseAnalytics: FirebaseAnalytics
) {

    /**
     * Topics format: "mega_android_{debug|release|qa}_{all_users|free_users|paid_users}"
     */
    private val topicPrefix = "mega_android_${BuildConfig.BUILD_TYPE}"

    /**
     * Subscribe to the account type specific FCM topic based on user's account type
     * Topic format: "mega_android_{debug|release|qa}_{free_users|paid_users}"
     *
     * @param accountType
     */
    fun subscribeToAccountTypeTopic(accountType: AccountType) {
        val freeUsersTopic = "${topicPrefix}_free_users"
        val paidUsersTopic = "${topicPrefix}_paid_users"
        if (accountType.isPaid || accountType.isBusinessAccount) {
            subscribeToTopic(paidUsersTopic)
            unsubscribeFromTopic(freeUsersTopic)
        } else {
            subscribeToTopic(freeUsersTopic)
            unsubscribeFromTopic(paidUsersTopic)
        }
    }

    /**
     * Subscribe to the all users FCM topic
     * Topic format: "mega_android_{debug|release|qa}_all_users"
     */
    fun subscribeToAllUsersTopic() {
        val allUsersTopic = "${topicPrefix}_all_users"
        subscribeToTopic(allUsersTopic)
    }

    /**
     * Subscribe to a specific FCM topic
     */
    fun subscribeToTopic(topic: String) {
        applicationScope.launch {
            runCatching {
                firebaseMessaging.subscribeToTopic(topic).await()
            }.onFailure {
                Timber.e(it, "Failed to subscribe to FCM topic: $topic")
            }
        }
    }

    /**
     * Unsubscribe from a specific FCM topic
     */
    fun unsubscribeFromTopic(topic: String) {
        applicationScope.launch {
            try {
                firebaseMessaging.unsubscribeFromTopic(topic).await()
                Timber.d("Unsubscribed from FCM topic: $topic")
            } catch (e: Exception) {
                Timber.e(e, "Failed to unsubscribe from FCM topic: $topic")
            }
        }
    }

    /**
     * Set user property "account_type" in Firebase Analytics based on user's account type
     *
     * @param accountType
     */
    fun setAccountTypeUserProperty(accountType: AccountType) {
        val accountTypeName = if (accountType.isPaid || accountType.isBusinessAccount) {
            "paid"
        } else {
            "free"
        }
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
        firebaseAnalytics.setUserProperty("account_type", accountTypeName)
    }
}