package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.GetTypedNodeModule
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetDeviceType
import mega.privacy.android.domain.usecase.GetFolderType
import mega.privacy.android.domain.usecase.HasAncestor
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [GetTypedNodeModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestGetTypedNodeModule {

    @Provides
    fun provideAddNodeType() = mock<AddNodeType>()

    @Provides
    fun provideGetFolderType() = mock<GetFolderType>()

    @Provides
    fun provideGetDeviceType() = mock<GetDeviceType>()

    @Provides
    fun provideHasAncestor() = mock<HasAncestor>()
}