package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.qualifier.ContinueWhereLeftOffSortPreference
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.SortDirection
import javax.inject.Inject
import javax.inject.Singleton

internal const val continueWhereLeftOffSortPreferenceFileName =
    "CONTINUE_WHERE_LEFT_OFF_SORT_PREFERENCE_FILE"

/**
 * Persists the sort field and direction for the Continue Where Left Off list.
 */
@Singleton
internal class ContinueWhereLeftOffSortPreferenceDataStore @Inject constructor(
    @ContinueWhereLeftOffSortPreference private val dataStore: DataStore<Preferences>,
) {

    fun monitorSortPreference(): Flow<Pair<ContinueWhereLeftOffSortField, SortDirection>> =
        dataStore.data.map { prefs ->
            val field = prefs[SORT_FIELD_KEY]
                ?.let { runCatching { ContinueWhereLeftOffSortField.valueOf(it) }.getOrNull() }
                ?: DEFAULT_SORT_FIELD
            val direction = prefs[SORT_DIRECTION_KEY]
                ?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: DEFAULT_SORT_DIRECTION
            field to direction
        }

    suspend fun setSortPreference(
        sortField: ContinueWhereLeftOffSortField,
        sortDirection: SortDirection,
    ) {
        dataStore.edit { prefs ->
            prefs[SORT_FIELD_KEY] = sortField.name
            prefs[SORT_DIRECTION_KEY] = sortDirection.name
        }
    }

    companion object {
        private val SORT_FIELD_KEY = stringPreferencesKey("cwlo_sort_field")
        private val SORT_DIRECTION_KEY = stringPreferencesKey("cwlo_sort_direction")
        private val DEFAULT_SORT_FIELD = ContinueWhereLeftOffSortField.Timestamp
        private val DEFAULT_SORT_DIRECTION = SortDirection.Descending
    }
}
