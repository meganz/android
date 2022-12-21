package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.data.mapper.toData
import mega.privacy.android.app.data.mapper.toPushMessage
import mega.privacy.android.app.data.mapper.toSkuMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatToggleModeMapper
import mega.privacy.android.app.presentation.achievements.UIMegaAchievementMapper
import mega.privacy.android.app.presentation.achievements.toUIMegaAchievement
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toFavourite
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.app.presentation.photos.albums.model.mapper.toUIAlbum
import mega.privacy.android.data.mapper.SkuMapper

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
     * Provide data mapper
     */
    @Provides
    fun provideDataMapper(): DataMapper = ::toData

    /**
     * Provide push message mapper
     */
    @Provides
    fun providePushMessageMapper(): PushMessageMapper = ::toPushMessage

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
     * Provide SKU mapper
     */
    @Provides
    fun provideSkuMapper(): SkuMapper = ::toSkuMapper

    /**
     * Provide UIAlbum mapper
     */
    @Provides
    fun provideUIAlbumMapper(): UIAlbumMapper = ::toUIAlbum

    /**
     * Provide UIMegaAchievementMapper
     */
    @Provides
    fun provideAchievementsMapper(): UIMegaAchievementMapper = ::toUIMegaAchievement
}