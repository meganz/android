package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import nz.mega.sdk.MegaNode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [GetNodeModule::class],
    components = [ViewModelComponent::class, SingletonComponent::class]
)
@Module
object TestGetNodeModule {

    @Provides
    fun provideGetChildrenNode() = mock<GetChildrenNode> {
        onBlocking { invoke(any(), null) }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetNodeByHandle() = mock<GetNodeByHandle> {
        onBlocking { invoke(any()) }.thenReturn(MegaNode())
    }
}
