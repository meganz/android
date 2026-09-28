package mega.privacy.android.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.StreamingAead
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.data.preferences.PushesPreferencesDataStore.Companion.PUSH_TOKEN_FILE
import mega.privacy.android.data.preferences.RequestPhoneNumberPreferencesDataStore.Companion.REQUEST_PHONE_NUMBER_FILE
import mega.privacy.android.data.preferences.base.createEncrypted
import mega.privacy.android.data.preferences.cameraUploadsSettingsPreferenceDataStoreName
import mega.privacy.android.data.preferences.chatSettingsPreferenceDataStoreName
import mega.privacy.android.data.preferences.continueWhereLeftOffSortPreferenceFileName
import mega.privacy.android.data.preferences.credentialDataStoreName
import mega.privacy.android.data.preferences.mediaTimelinePreferenceFileName
import mega.privacy.android.data.preferences.migration.CameraUploadsSettingsPreferenceDataStoreMigration
import mega.privacy.android.data.preferences.migration.ChatSettingsPreferenceDataStoreMigration
import mega.privacy.android.data.preferences.migration.CredentialsPreferencesMigration
import mega.privacy.android.data.preferences.paymentPreferenceFileName
import mega.privacy.android.data.preferences.qaAccountCacheDataStoreName
import mega.privacy.android.data.preferences.security.PasscodeDatastoreV1ToV2Migration
import mega.privacy.android.data.preferences.security.passcodeDatastoreName
import mega.privacy.android.data.preferences.pinnedItemsSortPreferenceFileName
import mega.privacy.android.data.preferences.viewedLinksSortPreferenceFileName
import mega.privacy.android.data.qualifier.ContinueWhereLeftOffSortPreference
import mega.privacy.android.data.qualifier.MediaTimelinePreferenceDataStore
import mega.privacy.android.data.qualifier.PaymentPreference
import mega.privacy.android.data.qualifier.PushesPreference
import mega.privacy.android.data.qualifier.RequestPhoneNumberPreference
import mega.privacy.android.data.qualifier.PinnedItemsSortPreference
import mega.privacy.android.data.qualifier.ViewedLinksSortPreference
import mega.privacy.android.domain.monitoring.CrashReporter
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

    /**
     * Provides DataStore<Preferences> for [PUSH_TOKEN_FILE]
     */
    @Singleton
    @Provides
    @PushesPreference
    fun providePushesPreferencesDataStore(
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
                    PUSH_TOKEN_FILE
                )
            ),
            scope = CoroutineScope(ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(PUSH_TOKEN_FILE) }
        )
    }

    @Singleton
    @Provides
    @Named(passcodeDatastoreName)
    fun providePasscodeDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        streamingAead: StreamingAead?,
        v1ToV2Migration: PasscodeDatastoreV1ToV2Migration,
        crashReporter: CrashReporter,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.createEncrypted(
        migrations = listOf(v1ToV2Migration),
        scope = CoroutineScope(ioDispatcher),
        streamingAead = streamingAead,
        context = context,
        fileName = passcodeDatastoreName,
        crashReporter = crashReporter,
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
    @Named(chatSettingsPreferenceDataStoreName)
    fun provideChatSettingsPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        migration: ChatSettingsPreferenceDataStoreMigration,
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
                    chatSettingsPreferenceDataStoreName
                )
            }
        )

    @Singleton
    @Provides
    @Named(credentialDataStoreName)
    fun provideCredentialDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        migration: CredentialsPreferencesMigration,
        streamingAead: StreamingAead?,
        crashReporter: CrashReporter,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createEncrypted(
            migrations = listOf(
                migration
            ),
            scope = CoroutineScope(ioDispatcher),
            streamingAead = streamingAead,
            context = context,
            fileName = credentialDataStoreName,
            crashReporter = crashReporter,
        )
    }

    @Singleton
    @Provides
    @Named(qaAccountCacheDataStoreName)
    fun provideQAAccountCacheDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        streamingAead: StreamingAead?,
        crashReporter: CrashReporter,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createEncrypted(
            scope = CoroutineScope(ioDispatcher),
            streamingAead = streamingAead,
            context = context,
            fileName = qaAccountCacheDataStoreName,
            crashReporter = crashReporter,
        )
    }

    @Provides
    @Singleton
    @MediaTimelinePreferenceDataStore
    internal fun provideMediaTimelinePreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(mediaTimelinePreferenceFileName) }
    )

    @Provides
    @Singleton
    @PaymentPreference
    internal fun providePaymentPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(paymentPreferenceFileName) }
    )

    @Provides
    @Singleton
    @ContinueWhereLeftOffSortPreference
    internal fun provideContinueWhereLeftOffSortPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(continueWhereLeftOffSortPreferenceFileName) }
    )

    @Provides
    @Singleton
    @ViewedLinksSortPreference
    internal fun provideViewedLinksSortPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(viewedLinksSortPreferenceFileName) }
    )

    @Provides
    @Singleton
    @PinnedItemsSortPreference
    internal fun providePinnedItemsSortPreferenceDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        scope = CoroutineScope(ioDispatcher),
        produceFile = { context.preferencesDataStoreFile(pinnedItemsSortPreferenceFileName) }
    )
}
