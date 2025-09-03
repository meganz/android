package mega.privacy.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.data.preferences.RequestPhoneNumberPreferencesDataStore.Companion.REQUEST_PHONE_NUMBER_FILE
import mega.privacy.android.data.preferences.base.createEncrypted
import mega.privacy.android.data.preferences.cameraUploadsSettingsPreferenceDataStoreName
import mega.privacy.android.data.preferences.credentialDataStoreName
import mega.privacy.android.data.preferences.migration.CameraUploadsSettingsPreferenceDataStoreMigration
import mega.privacy.android.data.preferences.migration.CredentialsPreferencesMigration
import mega.privacy.android.data.preferences.psa.psaPreferenceDataStoreName
import mega.privacy.android.data.preferences.security.PasscodeDatastoreMigration
import mega.privacy.android.data.preferences.security.passcodeDatastoreName
import mega.privacy.android.data.preferences.transfersPreferencesDataStoreName
import mega.privacy.android.data.qualifier.RequestPhoneNumberPreference
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Named
import javax.inject.Singleton

/**
 * DataStore Module of DataStore<Preferences>
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    /**
     * Provides DataStore<Preferences> for [REQUEST_PHONE_NUMBER_FILE]
     */
    @Singleton
    @Provides
    @RequestPhoneNumberPreference
    fun provideRequestPhoneNumberPreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(
                SharedPreferencesMigration(
                    context,
                    REQUEST_PHONE_NUMBER_FILE
                )
            ),
            scope = CoroutineScope(ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(REQUEST_PHONE_NUMBER_FILE) }
        )
    }

    @Singleton
    @Provides
    @Named(passcodeDatastoreName)
    fun providePasscodeDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        passcodeDatastoreMigration: PasscodeDatastoreMigration,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(
                passcodeDatastoreMigration
            ),
            scope = CoroutineScope(ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(passcodeDatastoreName) }
        )

    @Singleton
    @Provides
    @Named(cameraUploadsSettingsPreferenceDataStoreName)
    fun provideCameraUploadsSettingsPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        migration: CameraUploadsSettingsPreferenceDataStoreMigration,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(
                migration
            ),
            scope = CoroutineScope(ioDispatcher),
            produceFile = {
                context.preferencesDataStoreFile(
                    cameraUploadsSettingsPreferenceDataStoreName
                )
            }
        )

    @Singleton
    @Provides
    @Named(psaPreferenceDataStoreName)
    fun providePsaPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(psaPreferenceDataStoreName) }
    )

    @Singleton
    @Provides
    @Named(credentialDataStoreName)
    fun provideCredentialDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        migration: CredentialsPreferencesMigration,
        masterKey: MasterKey?,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createEncrypted(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(
                migration
            ),
            scope = CoroutineScope(ioDispatcher),
            masterKey = masterKey,
            context = context,
            fileName = credentialDataStoreName
        )
    }

    @Singleton
    @Provides
    @Named(transfersPreferencesDataStoreName)
    fun provideTransfersPreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            scope = CoroutineScope(ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(transfersPreferencesDataStoreName) }
        )
}
