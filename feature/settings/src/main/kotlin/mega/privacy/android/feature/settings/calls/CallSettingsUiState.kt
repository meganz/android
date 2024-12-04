package mega.privacy.android.feature.settings.calls

/**
 * @param isSoundNotificationActive whether the sound notifications are active or not, null value indicates it's not known yet.
 */
data class CallSettingsUiState(
    val isSoundNotificationActive: Boolean? = null,
)