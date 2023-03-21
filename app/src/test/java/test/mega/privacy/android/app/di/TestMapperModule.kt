package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.MapperModule
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.presentation.achievements.UIMegaAchievementMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toHeader
import mega.privacy.android.app.presentation.meeting.mapper.MeetingLastTimestampMapper
import mega.privacy.android.app.presentation.meeting.mapper.ScheduledMeetingTimestampMapper
import mega.privacy.android.app.presentation.meeting.mapper.toLastTimeFormatted
import mega.privacy.android.app.presentation.meeting.mapper.toScheduledTimeFormatted
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
    fun provideRepeatModeMapper(): RepeatModeMapper = mock()

    @Provides
    fun provideRepeatToggleModeMapper(): RepeatToggleModeMapper = mock()

    @Provides
    fun provideUIAlbumMapper(): UIAlbumMapper = mock()

    @Provides
    fun providePlaylistItemMapper(): PlaylistItemMapper = mock()

    @Provides
    fun provideAchievementsMapper(): UIMegaAchievementMapper = mock()

    @Provides
    fun provideHeaderMapper(): HeaderMapper = ::toHeader

    @Provides
    fun provideMeetingLastTimestampMapper(): MeetingLastTimestampMapper =
        ::toLastTimeFormatted

    @Provides
    fun provideScheduledMeetingTimestampMapper(): ScheduledMeetingTimestampMapper =
        ::toScheduledTimeFormatted
}