package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.EphemeralCredentialsGateway
import mega.privacy.android.data.mapper.login.EphemeralCredentialsMapper
import mega.privacy.android.data.mapper.login.EphemeralCredentialsPreferenceMapper
import mega.privacy.android.data.preferences.migration.EphemeralCredentialsMigration
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

internal class EphemeralCredentialsDataStore @Inject constructor(
    private val ephemeralCredentialsMigration: EphemeralCredentialsMigration,
    private val ephemeralCredentialsPreferenceMapper: EphemeralCredentialsPreferenceMapper,
    private val ephemeralCredentialsMapper: EphemeralCredentialsMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : EphemeralCredentialsGateway {
    private val Context.ephemeralCredentialsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = DATA_STORE_NAME,
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceMigrations = {
            listOf(ephemeralCredentialsMigration)
        }
    )

    override suspend fun save(ephemeral: EphemeralCredentials) {
        context.ephemeralCredentialsDataStore.edit { mutablePreferences ->
            ephemeralCredentialsPreferenceMapper(mutablePreferences, ephemeral)
        }
    }

    override suspend fun clear() {
        context.ephemeralCredentialsDataStore.edit { it.clear() }
    }

    override fun monitorEphemeralCredentials(): Flow<EphemeralCredentials?> =
        context.ephemeralCredentialsDataStore.data.map { preferences ->
            ephemeralCredentialsMapper(preferences)
        }

    companion object {
        private const val DATA_STORE_NAME = "ephemeral"

        val emailPreferenceKey = stringPreferencesKey("email")
        val passwordPreferenceKey = stringPreferencesKey("password")
        val sessionPreferenceKey = stringPreferencesKey("session")
        val firstNamePreferenceKey = stringPreferencesKey("firstName")
        val lastNamePreferenceKey = stringPreferencesKey("lastName")
    }
}