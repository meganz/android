package mega.privacy.android.app.di

import android.webkit.MimeTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.ChatRequestMapper
import mega.privacy.android.app.data.mapper.ContactRequestMapper
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.data.mapper.FeatureFlagMapper
import mega.privacy.android.app.data.mapper.FileTypeInfoMapper
import mega.privacy.android.app.data.mapper.MimeTypeMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.data.mapper.UserAlertMapper
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.mapper.getFileTypeInfo
import mega.privacy.android.app.data.mapper.getMimeType
import mega.privacy.android.app.data.mapper.mapMegaUserListToUserUpdate
import mega.privacy.android.app.data.mapper.toChatRequest
import mega.privacy.android.app.data.mapper.toContactRequest
import mega.privacy.android.app.data.mapper.toData
import mega.privacy.android.app.data.mapper.toFavouriteInfo
import mega.privacy.android.app.data.mapper.toFeatureFlag
import mega.privacy.android.app.data.mapper.toPushMessage
import mega.privacy.android.app.data.mapper.toUserAlert
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.mapper.toFavourite

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
}