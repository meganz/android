package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.di.MapperModule
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [MapperModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestMapperModule {

    @Provides
    fun provideDataMapper(): DataMapper = mock()

    @Provides
    fun providePushMessageMapper(): PushMessageMapper = mock()
}