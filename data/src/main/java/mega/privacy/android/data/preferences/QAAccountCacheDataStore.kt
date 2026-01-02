package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.QAAccountCacheGateway
import mega.privacy.android.domain.entity.user.UserCredentials
import javax.inject.Inject
import javax.inject.Named

internal const val qaAccountCacheDataStoreName = "QA_ACCOUNT_CACHE"

/**
 * Stores multiple account credentials for QA testing
 */
internal class QAAccountCacheDataStore @Inject constructor(
    @Named(qaAccountCacheDataStoreName) private val dataStore: DataStore<Preferences>,
) : QAAccountCacheGateway {

    private val cachedAccountsKey = stringPreferencesKey(CACHED_ACCOUNTS_KEY)

    override suspend fun saveAccount(credentials: UserCredentials) {
        if (credentials.email.isNullOrBlank() || credentials.session.isNullOrBlank()) {
            return
        }

        dataStore.edit { preferences ->
            val currentAccounts = getAllCachedAccountsFromPreferences(preferences)
            val updatedAccounts = if (currentAccounts.any { it.email == credentials.email }) {
                // Update existing account
                currentAccounts.map { if (it.email == credentials.email) credentials else it }
            } else {
                // Add new account
                currentAccounts + credentials
            }
            preferences[cachedAccountsKey] = Gson().toJson(updatedAccounts)
        }
    }

    override suspend fun getAllCachedAccounts(): List<UserCredentials> {
        return runCatching {
            dataStore.data.map { preferences ->
                getAllCachedAccountsFromPreferences(preferences)
            }.first()
        }.getOrElse { emptyList() }
    }

    override suspend fun removeAccount(email: String?) {
        if (email.isNullOrBlank()) {
            return
        }

        dataStore.edit { preferences ->
            val currentAccounts = getAllCachedAccountsFromPreferences(preferences)
            val updatedAccounts = currentAccounts.filter { it.email != email }
            preferences[cachedAccountsKey] = Gson().toJson(updatedAccounts)
            // Also remove last login time and remark
            val lastLoginKey = longPreferencesKey("${LAST_LOGIN_TIME_PREFIX}${email}")
            preferences.remove(lastLoginKey)
            val remarkKey = stringPreferencesKey("${REMARK_PREFIX}${email}")
            preferences.remove(remarkKey)
        }
    }

    override suspend fun clearAllAccounts() {
        dataStore.edit { preferences ->
            preferences.remove(cachedAccountsKey)
            // Also clear all last login times and remarks
            val keysToRemove = preferences.asMap().keys.filter {
                it.name.startsWith(LAST_LOGIN_TIME_PREFIX) || it.name.startsWith(REMARK_PREFIX)
            }
            keysToRemove.forEach { preferences.remove(it) }
        }
    }

    override suspend fun updateLastLoginTime(email: String?, timestamp: Long) {
        if (email.isNullOrBlank()) {
            return
        }

        dataStore.edit { preferences ->
            val key = longPreferencesKey("${LAST_LOGIN_TIME_PREFIX}${email}")
            preferences[key] = timestamp
        }
    }

    override suspend fun getLastLoginTime(email: String?): Long? {
        if (email.isNullOrBlank()) {
            return null
        }

        return runCatching {
            dataStore.data.map { preferences ->
                val key = longPreferencesKey("${LAST_LOGIN_TIME_PREFIX}${email}")
                preferences[key]
            }.first()
        }.getOrNull()
    }

    override suspend fun saveRemark(email: String?, remark: String?) {
        if (email.isNullOrBlank()) {
            return
        }

        dataStore.edit { preferences ->
            val key = stringPreferencesKey("${REMARK_PREFIX}${email}")
            if (remark.isNullOrBlank()) {
                preferences.remove(key)
            } else {
                preferences[key] = remark
            }
        }
    }

    override suspend fun getRemark(email: String?): String? {
        if (email.isNullOrBlank()) {
            return null
        }

        return runCatching {
            dataStore.data.map { preferences ->
                val key = stringPreferencesKey("${REMARK_PREFIX}${email}")
                preferences[key]
            }.first()
        }.getOrNull()
    }

    private fun getAllCachedAccountsFromPreferences(preferences: Preferences): List<UserCredentials> {
        val json = preferences[cachedAccountsKey] ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<UserCredentials>>() {}.type
            Gson().fromJson<List<UserCredentials>>(json, type) ?: emptyList()
        }.getOrElse { emptyList() }
    }

    companion object {
        private const val CACHED_ACCOUNTS_KEY = "cached_accounts"
        private const val LAST_LOGIN_TIME_PREFIX = "last_login_time_"
        private const val REMARK_PREFIX = "remark_"
    }
}
