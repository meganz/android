package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.data.repository.DefaultManagerStateRepository
import mega.privacy.android.app.domain.repository.ManagerStateRepository
import mega.privacy.android.app.domain.usecase.*
import javax.inject.Scope

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

    @Binds
    abstract fun bindGetParentHandle(useCase: DefaultGetManagerParentHandle): GetManagerParentHandle

    @Binds
    abstract fun bindSetParentHandle(useCase: DefaultSetManagerParentHandle): SetManagerParentHandle
}

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class ManagerActivityModule {

    @Binds
    abstract fun bindManagerStateRepository(repository: DefaultManagerStateRepository): ManagerStateRepository
}