package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.MapperModule
import mega.privacy.android.app.mediaplayer.mapper.ExoPlayerRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toHeader
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [MapperModule::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module
object TestMapperModule {

    val sortOrderIntMapper = mock<SortOrderIntMapper>()

    @Provides
    fun provideRepeatModeMapper(): RepeatToggleModeByExoPlayerMapper = mock()

    @Provides
    fun provideRepeatToggleModeMapper(): ExoPlayerRepeatModeMapper = mock()

    @Provides
    fun provideUIAlbumMapper(): UIAlbumMapper = mock()

    @Provides
    fun providePlaylistItemMapper(): PlaylistItemMapper = mock()

    @Provides
    fun provideHeaderMapper(): HeaderMapper = ::toHeader
}