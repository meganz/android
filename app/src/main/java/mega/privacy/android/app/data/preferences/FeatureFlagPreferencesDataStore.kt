package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.domain.entity.FeatureFlag
import javax.inject.Inject

private val Context.featureFlagDataStore: DataStore<Preferences> by preferencesDataStore(name = "FEATURE_FLAG_PREFERENCES")

/**
 * Implementation of @FeatureFlagPreferencesGateway for interaction with Preferences DataStore
 *
 * @param context: Application Context
 */
class FeatureFlagPreferencesDataStore
@Inject constructor(@ApplicationContext val context: Context) : FeatureFlagPreferencesGateway {

    /**
     * Sets feature value to true or false
     *
     * @param featureName : name of the feature
     * @param isEnabled: Boolean value
     */
    override suspend fun setFeature(featureName: String, isEnabled: Boolean) {
        val name = booleanPreferencesKey(featureName)
        context.featureFlagDataStore.edit { mutablePreferences ->
            mutablePreferences[name] = isEnabled
        }
    }

    /**
     * Gets all features
     *
     * @return Flow of Map of feature name & boolean value
     */
    override suspend fun getAllFeatures(): Flow<Map<String, Boolean>> {
        return flow {
            context.featureFlagDataStore.data.collect {
                it.asMap().keys.forEach { key ->
                    emit(mutableMapOf(Pair(key.toString(), it[key] as Boolean)))
                }
            }
        }
    }
}