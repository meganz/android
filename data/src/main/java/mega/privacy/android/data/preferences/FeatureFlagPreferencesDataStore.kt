package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import java.io.IOException
import javax.inject.Inject

private val Context.featureFlagDataStore: DataStore<Preferences> by preferencesDataStore(name = "FEATURE_FLAG_PREFERENCES")

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
}