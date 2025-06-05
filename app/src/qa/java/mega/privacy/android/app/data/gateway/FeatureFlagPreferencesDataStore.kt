package mega.privacy.android.app.data.gateway

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import java.io.IOException
import javax.inject.Inject

/**
 * Preferences DataStore name for feature flags
 */
const val FEATURE_FLAG_PREFERENCES = "FEATURE_FLAG_PREFERENCES"

private val Context.featureFlagDataStore: DataStore<Preferences> by preferencesDataStore(
    name = FEATURE_FLAG_PREFERENCES,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Implementation of [FeatureFlagPreferencesGateway] for interaction with Preferences DataStore
 * and [FeatureFlagValueProvider]
 *
 * @param context: Application Context
 */
class FeatureFlagPreferencesDataStore
@Inject constructor(@ApplicationContext val context: Context) : FeatureFlagPreferencesGateway,
    FeatureFlagValueProvider {

    override suspend fun setFeature(featureName: String, isEnabled: Boolean) {
        val name = booleanPreferencesKey(featureName)
        context.featureFlagDataStore.edit { mutablePreferences ->
            mutablePreferences[name] = isEnabled
        }
    }

    override fun getAllFeatures() = context.featureFlagDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override suspend fun isEnabled(feature: Feature) = context.featureFlagDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[booleanPreferencesKey(feature.name)]
        }.firstOrNull()

    override suspend fun setFeatureFlagForQuickSettingsTile(featureName: String?) {
        context.featureFlagDataStore.edit { mutablePreferences ->
            if (featureName == null) {
                mutablePreferences.remove(key)
            } else {
                mutablePreferences[key] = featureName
            }
        }
    }

    override fun getCurrentFeatureFlagForQuickSettingsTile() =
        context.featureFlagDataStore.data.map { it[key] }

    private companion object {
        const val KEY_QUICK_SETTINGS = "featureFlagForQuickSettingsTile"
        val key = stringPreferencesKey(KEY_QUICK_SETTINGS)
    }

    override val priority = FeatureFlagValuePriority.RuntimeOverride

}