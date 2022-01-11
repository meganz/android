package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaPreferences
import nz.mega.sdk.MegaRequestListenerInterface

interface SettingsRepository {
    fun getAttributes(): MegaAttributes?
    fun getPreferences(): MegaPreferences?
    fun setPasscodeLockEnabled(enabled: Boolean)
    suspend fun fetchContactLinksOption(): Boolean
    fun getStartScreen(): Int
    fun shouldHideRecentActivity(): Boolean
    fun isChatLoggingEnabled(): Boolean
    fun isLoggingEnabled(): Boolean
    fun setAutoAcceptQR(accept: Boolean)
}