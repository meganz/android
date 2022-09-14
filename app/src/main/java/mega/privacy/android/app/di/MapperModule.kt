package mega.privacy.android.app.di

import android.webkit.MimeTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.ChatRequestMapper
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.EventMapper
import mega.privacy.android.app.data.mapper.FavouriteFolderInfoMapper
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.data.mapper.FeatureFlagMapper
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.data.mapper.ImageMapper
import mega.privacy.android.app.data.mapper.MegaChatPeerListMapper
import mega.privacy.android.app.data.mapper.MegaExceptionMapper
import mega.privacy.android.app.data.mapper.MegaTransferMapper
import mega.privacy.android.app.data.mapper.MimeTypeMapper
import mega.privacy.android.app.data.mapper.NodeUpdateMapper
import mega.privacy.android.app.data.mapper.OnlineStatusMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.data.mapper.StartScreenMapper
import mega.privacy.android.app.data.mapper.TransferEventMapper
import mega.privacy.android.app.data.mapper.UserAlertMapper
import mega.privacy.android.app.data.mapper.UserLastGreenMapper
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.mapper.VideoMapper
import mega.privacy.android.app.data.mapper.getFileTypeInfo
import mega.privacy.android.app.data.mapper.getMimeType
import mega.privacy.android.app.data.mapper.mapMegaNodeListToNodeUpdate
import mega.privacy.android.app.data.mapper.mapMegaUserListToUserUpdate
import mega.privacy.android.app.data.mapper.toChatRequest
import mega.privacy.android.app.data.mapper.toContactRequest
import mega.privacy.android.app.data.mapper.toData
import mega.privacy.android.app.data.mapper.toEvent
import mega.privacy.android.app.data.mapper.toFavouriteFolderInfo
import mega.privacy.android.app.data.mapper.toFavouriteInfo
import mega.privacy.android.app.data.mapper.toFeatureFlag
import mega.privacy.android.app.data.mapper.toImage
import mega.privacy.android.app.data.mapper.toMegaChatPeerList
import mega.privacy.android.app.data.mapper.toMegaExceptionModel
import mega.privacy.android.app.data.mapper.toOnlineStatus
import mega.privacy.android.app.data.mapper.toPushMessage
import mega.privacy.android.app.data.mapper.toTransferEventModel
import mega.privacy.android.app.data.mapper.toTransferModel
import mega.privacy.android.app.data.mapper.toUserAlert
import mega.privacy.android.app.data.mapper.toUserUserLastGreen
import mega.privacy.android.app.data.mapper.toVideo
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatToggleModeMapper
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.mapper.toFavourite
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
class MapperModule {

    @Provides
    fun provideUserUpdateMapper(): UserUpdateMapper = ::mapMegaUserListToUserUpdate

    @Provides
    fun provideFavouriteInfoMapper(): FavouriteInfoMapper = ::toFavouriteInfo

    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = ::toFavourite

    @Provides
    fun provideFeatureFlagMapper(): FeatureFlagMapper = ::toFeatureFlag

    @Provides
    fun provideDataMapper(): DataMapper = ::toData

    @Provides
    fun providePushMessageMapper(): PushMessageMapper = ::toPushMessage

    @Provides
    fun provideContactRequestMapper(): ContactRequestMapper = ::toContactRequest

    @Provides
    fun provideEventMapper(): EventMapper = ::toEvent

    @Provides
    fun provideMimeTypeMapper(): MimeTypeMapper = { extension ->
        getMimeType(extension,
            MimeTypeMap.getSingleton()
            ::getMimeTypeFromExtension)
    }

    @Provides
    fun provideUserAlertMapper(): UserAlertMapper = ::toUserAlert

    @Provides
    fun provideChatRequestMapper(): ChatRequestMapper = ::toChatRequest

    @Provides
    fun provideFileTypeInfoMapper(mimeTypeMapper: MimeTypeMapper): FileTypeInfoMapper = { node ->
        getFileTypeInfo(node, mimeTypeMapper)
    }

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

    @Provides
    fun provideImagesMapper(): ImageMapper = ::toImage

    @Provides
    fun provideVideosMapper(): VideoMapper = ::toVideo

    @Provides
    fun provideNodeUpdateMapper(): NodeUpdateMapper = ::mapMegaNodeListToNodeUpdate

    @Provides
    fun provideStartScreenMapper(): StartScreenMapper = { StartScreen(it) }

    @Provides
    fun provideUserLastGreenMapper(): UserLastGreenMapper = ::toUserUserLastGreen

    @Provides
    fun provideMegaChatPeerListMapper(): MegaChatPeerListMapper = ::toMegaChatPeerList

    @Provides
    fun provideOnlineStatusMapper(): OnlineStatusMapper = ::toOnlineStatus
}