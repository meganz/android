package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.domain.di.ViewTypeModule
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.mockito.kotlin.mock

/**
 * Provide testing dependencies for View Type tests
 */
@Module
@TestInstallIn(
    replaces = [ViewTypeModule::class],
    components = [SingletonComponent::class],
)
object TestViewTypeModule {

    @Provides
    fun provideMonitorViewType() = mock<MonitorViewType>()

    @Provides
    fun provideSetViewType() = mock<SetViewType>()
}