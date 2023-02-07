package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.domain.di.ViewTypeModule
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [ViewTypeModule::class],
    components = [SingletonComponent::class],
)
object TestFileInfoModule {
    @Provides
    fun provideCheckNameCollision(): CheckNameCollision = mock()
}