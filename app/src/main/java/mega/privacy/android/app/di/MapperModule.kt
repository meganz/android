package mega.privacy.android.app.di

import android.webkit.MimeTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.BooleanPreferenceMapper
import mega.privacy.android.app.data.mapper.ContactItemMapper
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.EventMapper
import mega.privacy.android.app.data.mapper.FavouriteFolderInfoMapper
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.data.mapper.ImageMapper
import mega.privacy.android.app.data.mapper.MegaChatPeerListMapper
import mega.privacy.android.app.data.mapper.MegaExceptionMapper
import mega.privacy.android.app.data.mapper.MegaShareMapper
import mega.privacy.android.app.data.mapper.MegaTransferMapper
import mega.privacy.android.app.data.mapper.MimeTypeMapper
import mega.privacy.android.app.data.mapper.NodeUpdateMapper
import mega.privacy.android.app.data.mapper.OnlineStatusMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.data.mapper.SortOrderIntMapper
import mega.privacy.android.app.data.mapper.SortOrderMapper
import mega.privacy.android.app.data.mapper.StorageStateMapper
import mega.privacy.android.app.data.mapper.TransferEventMapper
import mega.privacy.android.app.data.mapper.VideoMapper
import mega.privacy.android.app.data.mapper.getFileTypeInfo
import mega.privacy.android.app.data.mapper.getMimeType
import mega.privacy.android.app.data.mapper.mapBooleanPreference
import mega.privacy.android.app.data.mapper.mapMegaNodeListToNodeUpdate
import mega.privacy.android.app.data.mapper.toContactItem
import mega.privacy.android.app.data.mapper.toContactRequest
import mega.privacy.android.app.data.mapper.toData
import mega.privacy.android.app.data.mapper.toEvent
import mega.privacy.android.app.data.mapper.toFavouriteFolderInfo
import mega.privacy.android.app.data.mapper.toFavouriteInfo
import mega.privacy.android.app.data.mapper.toImage
import mega.privacy.android.app.data.mapper.toInt
import mega.privacy.android.app.data.mapper.toMegaChatPeerList
import mega.privacy.android.app.data.mapper.toMegaExceptionModel
import mega.privacy.android.app.data.mapper.toOnlineStatus
import mega.privacy.android.app.data.mapper.toPushMessage
import mega.privacy.android.app.data.mapper.toShareModel
import mega.privacy.android.app.data.mapper.toSortOrder
import mega.privacy.android.app.data.mapper.toStorageState
import mega.privacy.android.app.data.mapper.toTransferEventModel
import mega.privacy.android.app.data.mapper.toTransferModel
import mega.privacy.android.app.data.mapper.toVideo
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatToggleModeMapper
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.mapper.toFavourite
import mega.privacy.android.data.mapper.UserAccountMapper
import mega.privacy.android.domain.entity.UserAccount

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
class MapperModule {

    /**
     * Provide favourite info mapper
     */
    @Provides
    fun provideFavouriteInfoMapper(): FavouriteInfoMapper = ::toFavouriteInfo

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
     * Provide contact request mapper
     */
    @Provides
    fun provideContactRequestMapper(): ContactRequestMapper = ::toContactRequest

    /**
     * Provide event mapper
     */
    @Provides
    fun provideEventMapper(): EventMapper = ::toEvent

    /**
     * Provide mime type mapper
     */
    @Provides
    fun provideMimeTypeMapper(): MimeTypeMapper = { extension ->
        getMimeType(extension,
            MimeTypeMap.getSingleton()
            ::getMimeTypeFromExtension)
    }

    /**
     * Provide file type info mapper
     *
     * @param mimeTypeMapper
     */
    @Provides
    fun provideFileTypeInfoMapper(mimeTypeMapper: MimeTypeMapper): FileTypeInfoMapper = { node ->
        getFileTypeInfo(node, mimeTypeMapper)
    }

    /**
     * Provide favourite folder info mapper
     */
    @Provides
    fun provideFavouriteFolderInfoMapper(): FavouriteFolderInfoMapper = ::toFavouriteFolderInfo

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
     * Provide mega transfer mapper
     */
    @Provides
    fun provideMegaTransferMapper(): MegaTransferMapper = ::toTransferModel

    /**
     * Provide mega exception mapper
     */
    @Provides
    fun provideMegaExceptionMapper(): MegaExceptionMapper = ::toMegaExceptionModel

    /**
     * Provide transfer event mapper
     */
    @Provides
    fun provideTransferEventMapper(
        exceptionMapper: MegaExceptionMapper,
        transferMapper: MegaTransferMapper,
    ): TransferEventMapper = { event ->
        toTransferEventModel(event, transferMapper, exceptionMapper)
    }

    /**
     * Provide images mapper
     */
    @Provides
    fun provideImagesMapper(): ImageMapper = ::toImage

    /**
     * Provide videos mapper
     */
    @Provides
    fun provideVideosMapper(): VideoMapper = ::toVideo

    /**
     * Provide node update mapper
     */
    @Provides
    fun provideNodeUpdateMapper(): NodeUpdateMapper = ::mapMegaNodeListToNodeUpdate

    /**
     * Provide mega chat peer list mapper
     */
    @Provides
    fun provideMegaChatPeerListMapper(): MegaChatPeerListMapper = ::toMegaChatPeerList

    /**
     * Provide online status mapper
     */
    @Provides
    fun provideOnlineStatusMapper(): OnlineStatusMapper = ::toOnlineStatus

    /**
     * Provide sort order mapper
     */
    @Provides
    fun provideSortOrderMapper(): SortOrderMapper = ::toSortOrder

    /**
     * Provide sort order int mapper
     */
    @Provides
    fun provideSortOrderIntMapper(): SortOrderIntMapper = ::toInt

    /**
     * Provide contact item mapper
     */
    @Provides
    fun provideContactItemMapper(): ContactItemMapper = ::toContactItem

    /**
     * Provide storage state mapper
     */
    @Provides
    fun provideStorageStateMapper(): StorageStateMapper = ::toStorageState

    /**
     * Provide mega share mapper
     */
    @Provides
    fun provideMegaShareMapper(): MegaShareMapper = ::toShareModel

    /**
     * Provide user account mapper
     */
    @Provides
    fun provideUserAccountMapper(): UserAccountMapper = ::UserAccount


    /**
     * Provide boolean preference mapper
     */
    @Provides
    fun provideBooleanPreferenceMapper(): BooleanPreferenceMapper = ::mapBooleanPreference

}