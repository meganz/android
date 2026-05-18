package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.qualifier.ViewedLinksSortPreference
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import javax.inject.Inject
import javax.inject.Singleton

internal const val viewedLinksSortPreferenceFileName =
    "VIEWED_LINKS_SORT_PREFERENCE_FILE"

/**
 * Persists the sort field and direction for the viewed-links list.
 */
@Singleton
internal class ViewedLinksSortPreferenceDataStore @Inject constructor(
    @ViewedLinksSortPreference private val dataStore: DataStore<Preferences>,
) {

    fun monitorSortPreference(): Flow<Pair<ViewedLinksSortField, SortDirection>> =
        dataStore.data.map { prefs ->
            val field = prefs[SORT_FIELD_KEY]
                ?.let { runCatching { ViewedLinksSortField.valueOf(it) }.getOrNull() }
                ?: DEFAULT_SORT_FIELD
            val direction = prefs[SORT_DIRECTION_KEY]
                ?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: DEFAULT_SORT_DIRECTION
            field to direction
        }

    suspend fun setSortPreference(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    ) {
        dataStore.edit { prefs ->
            prefs[SORT_FIELD_KEY] = sortField.name
            prefs[SORT_DIRECTION_KEY] = sortDirection.name
        }
    }

    companion object {
        private val SORT_FIELD_KEY = stringPreferencesKey("viewed_links_sort_field")
        private val SORT_DIRECTION_KEY = stringPreferencesKey("viewed_links_sort_direction")
        private val DEFAULT_SORT_FIELD = ViewedLinksSortField.LastAccessed
        private val DEFAULT_SORT_DIRECTION = SortDirection.Descending
    }
}
