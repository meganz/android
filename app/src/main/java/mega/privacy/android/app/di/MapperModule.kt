package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.mediaplayer.mapper.PlaylistItemMapper
import mega.privacy.android.app.mediaplayer.mapper.toPlaylistItemMapper
import mega.privacy.android.app.presentation.achievements.UIMegaAchievementMapper
import mega.privacy.android.app.presentation.achievements.toUIMegaAchievement
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toFavourite
import mega.privacy.android.app.presentation.favourites.model.mapper.toHeader
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapper
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapperImpl

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
     * Provide initial screen mapper
     */
    @Provides
    fun provideInitialScreenMapper(): InitialScreenMapper = InitialScreenMapperImpl()
}