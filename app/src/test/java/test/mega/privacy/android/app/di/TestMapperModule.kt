package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.di.MapperModule
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.data.mapper.BooleanPreferenceMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderMapper
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [MapperModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestMapperModule {

    val sortOrderIntMapper = mock<SortOrderIntMapper>()

    @Provides
    fun provideDataMapper(): DataMapper = mock()

    @Provides
    fun providePushMessageMapper(): PushMessageMapper = mock()

    @Provides
    fun provideRepeatModeMapper(): RepeatModeMapper = mock()

    @Provides
    fun provideRepeatToggleModeMapper(): RepeatToggleModeMapper = mock()

    @Provides
    fun provideSortOrderMapper(): SortOrderMapper = mock()

    @Provides
    fun provideSortOrderIntMapper(): SortOrderIntMapper = sortOrderIntMapper

    @Provides
    fun provideBooleanPreferenceMapper(): BooleanPreferenceMapper = mock()

    @Provides
    fun provideFileTypeInfoMapper(): FileTypeInfoMapper = mock()

}