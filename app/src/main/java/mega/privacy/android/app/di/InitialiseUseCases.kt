package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.DefaultRootNodeExists
import mega.privacy.android.domain.usecase.DefaultMonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.RootNodeExists

/**
 * Initialise use cases module
 *
 * Provides use cases used on multiple view models as part of the default initialisation checks
 *
 * @constructor Create empty Initialise use cases
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class InitialiseUseCases {

    @Binds
    abstract fun bindMonitorConnectivity(useCase: DefaultMonitorConnectivity): MonitorConnectivity

    @Binds
    abstract fun bindRootNodeExists(useCase: DefaultRootNodeExists): RootNodeExists

}