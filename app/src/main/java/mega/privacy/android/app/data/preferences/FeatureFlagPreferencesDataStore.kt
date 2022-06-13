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

class FeatureFlagPreferencesDataStore
@Inject constructor(@ApplicationContext val context: Context) : FeatureFlagPreferencesGateway {

    override suspend fun setFeature(featureName: String, isEnabled: Boolean) {
        val name = booleanPreferencesKey(featureName)
        context.featureFlagDataStore.edit { mutablePreferences ->
            mutablePreferences[name] = isEnabled
        }
    }

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