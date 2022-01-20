package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
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
    fun setChatLoggingEnabled(enabled: Boolean)
    fun isLoggingEnabled(): Boolean
    fun setLoggingEnabled(enabled: Boolean)
    suspend fun setAutoAcceptQR(accept: Boolean): Boolean
    fun monitorStartScreen(): Flow<Int>
    fun monitorHideRecentActivity(): Flow<Boolean>
}