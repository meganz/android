package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultMonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates

@Module
@InstallIn(ViewModelComponent::class)
abstract class ManagerUseCases {

    @Binds
    abstract fun bindMonitorNodeUpdates(useCase: DefaultMonitorNodeUpdates): MonitorNodeUpdates

}