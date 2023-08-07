package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import mega.privacy.android.app.middlelayer.inappupdate.InAppUpdateHandler
import mega.privacy.android.app.service.inappupdate.InAppUpdateHandlerImpl

/**
 * InAppUpdate Module
 */
@Module
@InstallIn(ActivityComponent::class)
abstract class InAppUpdateModule {
    /**
     * Provide [InAppUpdateHandler] Implementation
     */
    @Binds
    abstract fun bindInAppUpdateHandler(implementation: InAppUpdateHandlerImpl): InAppUpdateHandler

}
