package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerHandler
import mega.privacy.android.app.service.installreferrer.InstallReferrerHandlerImpl

/**
 * InstallReferrerModule Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InstallReferrerModule {

    /**
     * Binds [InstallReferrerHandler] Implementation
     */
    @Binds
    abstract fun bindInstallReferrerHandler(implementation: InstallReferrerHandlerImpl): InstallReferrerHandler

}