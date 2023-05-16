package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toPlaylistItemMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatToggleModeMapper
import mega.privacy.android.app.presentation.achievements.UIMegaAchievementMapper
import mega.privacy.android.app.presentation.achievements.toUIMegaAchievement
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toFavourite
import mega.privacy.android.app.presentation.favourites.model.mapper.toHeader
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapper
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapperImpl
import mega.privacy.android.app.presentation.meeting.mapper.MeetingLastTimestampMapper
import mega.privacy.android.app.presentation.meeting.mapper.ScheduledMeetingTimestampMapper
import mega.privacy.android.app.presentation.meeting.mapper.toLastTimeFormatted
import mega.privacy.android.app.presentation.meeting.mapper.toScheduledTimeFormatted

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
class MapperModule {

    /**
     * Provide favourite mapper
     */
    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = ::toFavourite

    /**
     * Provide the RepeatModeMapper
     */
    @Provides
    fun provideRepeatModeMapper(): RepeatModeMapper = ::toRepeatToggleModeMapper

    /**
     * Provide the RepeatToggleModeMapper
     */
    @Provides
    fun provideRepeatToggleModeMapper(): RepeatToggleModeMapper = ::toRepeatModeMapper

    /**
     * Provide UIMegaAchievementMapper
     */
    @Provides
    fun provideAchievementsMapper(): UIMegaAchievementMapper = ::toUIMegaAchievement

    /**
     * Provide PlaylistItem mapper
     */
    @Provides
    fun providePlaylistItemMapper(): PlaylistItemMapper = ::toPlaylistItemMapper

    /**
     * Provide header mapper
     */
    @Provides
    fun provideHeaderMapper(): HeaderMapper = ::toHeader

    /**
     * Provide meeting last timestamp mapper
     */
    @Provides
    fun provideMeetingLastTimestampMapper(): MeetingLastTimestampMapper = ::toLastTimeFormatted

    /**
     * Provide scheduled meeting timestamp mapper
     */
    @Provides
    fun provideScheduledMeetingTimestampMapper(): ScheduledMeetingTimestampMapper =
        ::toScheduledTimeFormatted

    /**
     * Provide initial screen mapper
     */
    @Provides
    fun provideInitialScreenMapper(): InitialScreenMapper = InitialScreenMapperImpl()
}