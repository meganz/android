package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.di.manager.ManagerUseCases
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.MonitorUserAlertUpdates
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ManagerUseCases::class],
    components = [ViewModelComponent::class, ServiceComponent::class]
)
@Module(includes = [TestGetNodeModule::class])
object TestManagerUseCases {

    @Provides
    fun provideHasBackupsChildren() = mock<HasBackupsChildren> {
        on { invoke() }.thenReturn(false)
    }

    @Provides
    fun provideMonitorUserAlertUpdates() = mock<MonitorUserAlertUpdates> {
        on { invoke() }.thenReturn(flowOf(emptyList()))
    }
}
