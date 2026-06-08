package mega.privacy.android.feature.documentscanner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import javax.inject.Inject

private const val SCANNER_PREFERENCES_FILE = "continuous_document_scanner"

private val Context.scannerPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SCANNER_PREFERENCES_FILE,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Jetpack DataStore-backed [ScannerPreferencesRepository].
 *
 * Stores a single boolean today (cellular-data consent for the model download).
 * The DataStore file is private to the doc-scanner feature so it can be moved
 * or migrated without coordinating with other features.
 */
internal class DefaultScannerPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScannerPreferencesRepository {

    override suspend fun hasGrantedCellularConsent(): Boolean =
        context.scannerPreferencesDataStore.data
            .map { it[KEY_CELLULAR_CONSENT] ?: false }
            .first()

    override suspend fun grantCellularConsent() {
        context.scannerPreferencesDataStore.edit { it[KEY_CELLULAR_CONSENT] = true }
    }

    private companion object {
        val KEY_CELLULAR_CONSENT = booleanPreferencesKey("cellular_consent")
    }
}
