package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultMonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.DefaultMonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates

/**
 * Manager module
 *
 * Provides dependencies used by multiple screens in the manager package
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class ManagerViewModelModule {

    @Binds
    abstract fun bindMonitorGlobalUpdates(useCase: DefaultMonitorGlobalUpdates): MonitorGlobalUpdates

    @Binds
    abstract fun bindMonitorNodeUpdates(useCase: DefaultMonitorNodeUpdates): MonitorNodeUpdates

}