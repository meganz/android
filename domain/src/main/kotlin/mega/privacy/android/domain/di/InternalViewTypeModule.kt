package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.ViewTypeRepository
import mega.privacy.android.domain.usecase.viewtype.DefaultMonitorViewType
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType

/**
 * Internal module class to provide all Use Cases for [ViewTypeModule]
 */
@Module
@DisableInstallInCheck
internal abstract class InternalViewTypeModule {

    @Binds
    abstract fun bindMonitorViewType(implementation: DefaultMonitorViewType): MonitorViewType

    companion object {
        @Provides
        fun provideSetViewType(viewTypeRepository: ViewTypeRepository): SetViewType =
            SetViewType(viewTypeRepository::setViewType)
    }
}