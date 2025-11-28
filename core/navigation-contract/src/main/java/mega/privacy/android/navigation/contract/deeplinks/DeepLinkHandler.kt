package mega.privacy.android.navigation.contract.deeplinks

import android.net.Uri
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/**
 * Deep link handler interface to be implemented by feature modules.
 * @property snackbarEventQueue to send messages to the snackbar queue
 */
abstract class DeepLinkHandler(
    protected val snackbarEventQueue: SnackbarEventQueue,
) {
    /**
     * Get the NavKeys from the given Uri and [RegexPatternType] if it is a valid deep link for this feature and login state match the NavKey requirements.
     * A list of NavKeys can be added if the Uri points to a detail destination that needs the parent to be in the backStack
     *
     * @param uri The Uri to check
     * @param regexPatternType
     * @param isLoggedIn
     * @return The NavKeys if the Uri is valid, empty list if it's valid but can't be shown (login required for instance), null if it's not a valid link for this feature
     */
    open suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? {
        val navKeys = getNavKeys(uri, regexPatternType)
        return when {
            navKeys.isNullOrEmpty() -> navKeys

            isLoggedIn && navKeys.any { it is NoSessionNavKey.Mandatory } -> {
                // If the user is logged in and at least one nav keys is a mandatory no session nav key check the logged out message to decide if the message should be shown instead of navigating to the destinations
                loggedOutRequiredMessage(navKeys, regexPatternType)?.let { message ->
                    snackbarEventQueue.queueMessage(message)
                    emptyList()
                }
            }

            !isLoggedIn && navKeys.any { it !is NoSessionNavKey } -> {
                // If the user is logged out and at least one nav keys is not a no session nav key check the logged in message to decide if the message should be shown instead of navigating to the destinations
                loggedInRequiredMessage(navKeys, regexPatternType)?.let { message ->
                    snackbarEventQueue.queueMessage(message)
                    emptyList()
                }
            }

            else -> navKeys
        } ?: navKeys
    }

    /**
     * String resource for the message to be shown in case of logged in is required and it's not logged in.
     * The parameters can be used to get the specific message for this deep link.
     * @param navKeys the destinations computed for the deep link that could trigger the logged in required message, at leas one is not a [NoSessionNavKey]
     * @param regexPatternType the computed regex pattern type for the deep link
     * @return the string resource id of the message to show trough [SnackbarEventQueue] if it's not null. Default message to inform that user needs to be logged in to perform this action.
     */
    @StringRes
    open fun loggedInRequiredMessage(
        navKeys: List<NavKey>,
        regexPatternType: RegexPatternType?,
    ): Int? = sharedR.string.general_alert_not_logged_in

    /**
     * String resource for the message to be shown in case of logged out is required and it's logged in.
     * The parameters can be used to get the specific message for this deep link.
     * @param navKeys the destinations computed for the deep link that could trigger the logged ou required message, at leas one is a [NoSessionNavKey.Mandatory]
     * @param regexPatternType the computed regex pattern type for the deep link
     * @return the string resource id of the message to show trough [SnackbarEventQueue] if it's not null. Default to null so app will open unless it's overridden.
     */
    @StringRes
    open fun loggedOutRequiredMessage(
        navKeys: List<NavKey>,
        regexPatternType: RegexPatternType?,
    ): Int? = null

    /**
     * Get the NavKeys from the given Uri and [RegexPatternType] if it is a valid deep link for this feature.
     * A list of NavKeys can be added if the Uri points to a detail destination that needs the parent to be in the backStack
     *
     * @param uri The Uri to check
     * @param regexPatternType
     * @return The NavKeys if the Uri is valid, null otherwise
     */
    abstract suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>?


    /**
     * Priority of this deep link handlers. Lower values will be handled first. Use big value if you want to handle it last if no other handler can handle it.
     *
     */
    open val priority get() = 10

    /**
     * Runs the [block] code to get a [List<NavKey>)] in a runCatching block. In case any throwable is cached an empty list is returned and the exception logged.
     * @param block to produce the expected [List<NavKey>)]
     * @return the [List<NavKey>)] produced by [block] if no exceptions are captured, or an empty list otherwise
     */
    protected suspend fun catchWithEmptyListAndLog(block: suspend (() -> List<NavKey>)): List<NavKey> =
        runCatching { block() }.onFailure { Timber.e(it) }.getOrDefault(emptyList())
}