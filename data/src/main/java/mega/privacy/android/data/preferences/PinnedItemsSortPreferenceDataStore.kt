package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.qualifier.PinnedItemsSortPreference
import mega.privacy.android.domain.entity.home.PinnedHomeItemsSortField
import mega.privacy.android.domain.entity.node.SortDirection
import javax.inject.Inject
import javax.inject.Singleton

internal const val pinnedItemsSortPreferenceFileName =
    "PINNED_ITEMS_SORT_PREFERENCE_FILE"

/**
 * Persists the sort field and direction for the pinned home items View-all list.
 */
@Singleton
internal class PinnedItemsSortPreferenceDataStore @Inject constructor(
    @PinnedItemsSortPreference private val dataStore: DataStore<Preferences>,
) {

    fun monitorSortPreference(): Flow<Pair<PinnedHomeItemsSortField, SortDirection>> =
        dataStore.data.map { prefs ->
            val field = prefs[SORT_FIELD_KEY]
                ?.let { runCatching { PinnedHomeItemsSortField.valueOf(it) }.getOrNull() }
                ?: DEFAULT_SORT_FIELD
            val direction = prefs[SORT_DIRECTION_KEY]
                ?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: DEFAULT_SORT_DIRECTION
            field to direction
        }

    suspend fun setSortPreference(
        sortField: PinnedHomeItemsSortField,
        sortDirection: SortDirection,
    ) {
        dataStore.edit { prefs ->
            prefs[SORT_FIELD_KEY] = sortField.name
            prefs[SORT_DIRECTION_KEY] = sortDirection.name
        }
    }

    companion object {
        private val SORT_FIELD_KEY = stringPreferencesKey("pinned_items_sort_field")
        private val SORT_DIRECTION_KEY = stringPreferencesKey("pinned_items_sort_direction")
        private val DEFAULT_SORT_FIELD = PinnedHomeItemsSortField.Custom
        private val DEFAULT_SORT_DIRECTION = SortDirection.Ascending
    }
}
