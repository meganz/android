package mega.privacy.android.data.di

import android.webkit.MimeTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.BooleanPreferenceMapper
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.data.mapper.ContactDataMapper
import mega.privacy.android.data.mapper.ContactItemMapper
import mega.privacy.android.data.mapper.ContactRequestMapper
import mega.privacy.android.data.mapper.CurrencyMapper
import mega.privacy.android.data.mapper.EventMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.ImageMapper
import mega.privacy.android.data.mapper.MediaStoreFileTypeMapper
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.MegaAchievementMapper
import mega.privacy.android.data.mapper.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MegaShareMapper
import mega.privacy.android.data.mapper.MegaTransferMapper
import mega.privacy.android.data.mapper.MimeTypeMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OnlineStatusMapper
import mega.privacy.android.data.mapper.PaymentMethodMapper
import mega.privacy.android.data.mapper.RecentActionBucketMapper
import mega.privacy.android.data.mapper.PricingMapper
import mega.privacy.android.data.mapper.RecentActionsMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.SortOrderMapper
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.mapper.SubscriptionPlanListMapper
import mega.privacy.android.data.mapper.SubscriptionPlanMapper
import mega.privacy.android.data.mapper.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.SyncRecordTypeMapper
import mega.privacy.android.data.mapper.TransferEventMapper
import mega.privacy.android.data.mapper.UserAccountMapper
import mega.privacy.android.data.mapper.UserAlertMapper
import mega.privacy.android.data.mapper.UserLastGreenMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.VideoMapper
import mega.privacy.android.data.mapper.getFileTypeInfo
import mega.privacy.android.data.mapper.getMimeType
import mega.privacy.android.data.mapper.mapBooleanPreference
import mega.privacy.android.data.mapper.mapMegaNodeListToNodeUpdate
import mega.privacy.android.data.mapper.mapMegaUserListToUserUpdate
import mega.privacy.android.data.mapper.sortOrderToInt
import mega.privacy.android.data.mapper.storageStateToInt
import mega.privacy.android.data.mapper.toAccountType
import mega.privacy.android.data.mapper.toChatRequest
import mega.privacy.android.data.mapper.toContactData
import mega.privacy.android.data.mapper.toContactItem
import mega.privacy.android.data.mapper.toContactRequest
import mega.privacy.android.data.mapper.toEvent
import mega.privacy.android.data.mapper.toImage
import mega.privacy.android.data.mapper.toMediaStoreFileType
import mega.privacy.android.data.mapper.toMediaStoreFileTypeUri
import mega.privacy.android.data.mapper.toMegaAchievement
import mega.privacy.android.data.mapper.toMegaChatPeerList
import mega.privacy.android.data.mapper.toMegaExceptionModel
import mega.privacy.android.data.mapper.toNode
import mega.privacy.android.data.mapper.toOnlineStatus
import mega.privacy.android.data.mapper.toPaymentMethodType
import mega.privacy.android.data.mapper.toPricing
import mega.privacy.android.data.mapper.toRecentActionBucket
import mega.privacy.android.data.mapper.toRecentActionBucketList
import mega.privacy.android.data.mapper.toShareModel
import mega.privacy.android.data.mapper.toSortOrder
import mega.privacy.android.data.mapper.toStorageState
import mega.privacy.android.data.mapper.toSubscriptionPlan
import mega.privacy.android.data.mapper.toSubscriptionPlanList
import mega.privacy.android.data.mapper.toSyncRecordType
import mega.privacy.android.data.mapper.toSyncRecordTypeInt
import mega.privacy.android.data.mapper.toTransferEventModel
import mega.privacy.android.data.mapper.toTransferModel
import mega.privacy.android.data.mapper.toUserAlert
import mega.privacy.android.data.mapper.toUserSet
import mega.privacy.android.data.mapper.toUserUserLastGreen
import mega.privacy.android.data.mapper.toVideo
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
internal class MapperModule {
    /**
     * Provide account type mapper
     */
    @Provides
    fun provideAccountTypeMapper(): AccountTypeMapper = ::toAccountType

    /**
     * Provide user update mapper
     */
    @Provides
    fun provideUserUpdateMapper(): UserUpdateMapper = ::mapMegaUserListToUserUpdate

    /**
     * Provide user alert mapper
     */
    @Provides
    fun provideUserAlertMapper(): UserAlertMapper = ::toUserAlert

    /**
     * Provide chat request mapper
     */
    @Provides
    fun provideChatRequestMapper(): ChatRequestMapper = ::toChatRequest

    /**
     * Provide start screen mapper
     */
    @Provides
    fun provideStartScreenMapper(): StartScreenMapper = { StartScreen(it) }

    /**
     * Provide user last green mapper
     */
    @Provides
    fun provideUserLastGreenMapper(): UserLastGreenMapper = ::toUserUserLastGreen

    /**
     * Provide contact data mapper
     */
    @Provides
    fun provideContactDataMapper(): ContactDataMapper = ::toContactData


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
     * Provide event mapper
     */
    @Provides
    fun provideEventMapper(): EventMapper = ::toEvent

    /**
     * Provide sort order mapper
     */
    @Provides
    fun provideSortOrderMapper(): SortOrderMapper = ::toSortOrder

    /**
     * Provide sort order int mapper
     */
    @Provides
    fun provideSortOrderIntMapper(): SortOrderIntMapper = ::sortOrderToInt

    /**
     * Provide sync record type mapper
     */
    @Provides
    fun provideSyncRecordTypeMapper(): SyncRecordTypeMapper = ::toSyncRecordType

    /**
     * Provide sync record type int mapper
     */
    @Provides
    fun provideSyncRecordTypeIntMapper(): SyncRecordTypeIntMapper = ::toSyncRecordTypeInt

    /**
     * Provide media store file type mapper
     */
    @Provides
    fun provideMediaStoreFileTypeMapper(): MediaStoreFileTypeMapper = ::toMediaStoreFileType

    /**
     * Provide media store file type uri mapper
     */
    @Provides
    fun provideMediaStoreFileTypeUriMapper(): MediaStoreFileTypeUriMapper =
        ::toMediaStoreFileTypeUri

    /**
     * Provide storage state mapper
     */
    @Provides
    fun provideStorageStateMapper(): StorageStateMapper = ::toStorageState

    /**
     * Provide [StorageState] to [Int] mapper
     */
    @Provides
    fun provideStorageStateIntMapper(): StorageStateIntMapper = ::storageStateToInt

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
     * Provide contact item mapper
     */
    @Provides
    fun provideContactItemMapper(): ContactItemMapper = ::toContactItem

    /**
     * Provide mega share mapper
     */
    @Provides
    fun provideMegaShareMapper(): MegaShareMapper = ::toShareModel

    /**
     * Provide boolean preference mapper
     */
    @Provides
    fun provideBooleanPreferenceMapper(): BooleanPreferenceMapper = ::mapBooleanPreference

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
     * Provide contact request mapper
     */
    @Provides
    fun provideContactRequestMapper(): ContactRequestMapper = ::toContactRequest

    /**
     * Provide mime type mapper
     */
    @Provides
    fun provideMimeTypeMapper(): MimeTypeMapper = { extension ->
        getMimeType(
            extension,
            MimeTypeMap.getSingleton()
            ::getMimeTypeFromExtension
        )
    }


    /**
     * Provide favourite info mapper
     */
    @Provides
    fun provideFavouriteInfoMapper(): NodeMapper = ::toNode


    /**
     * Provide user account mapper
     */
    @Provides
    fun provideUserAccountMapper(): UserAccountMapper =
        ::UserAccount

    /**
     * Provide subscription plan mapper
     */
    @Provides
    fun provideSubscriptionPlanMapper(): SubscriptionPlanMapper = ::toSubscriptionPlan

    /**
     * Provide pricing mapper
     */
    @Provides
    fun providePricingMapper(): PricingMapper = ::toPricing

    /**
     * Provide currency mapper
     */
    @Provides
    fun provideCurrencyMapper(): CurrencyMapper = ::Currency

    /**
     * Provide paymentMethod type mapper
     */
    @Provides
    fun providePaymentMethodTypeMapper(): PaymentMethodMapper = ::toPaymentMethodType

    /**
     * Provide subscription plan list mapper
     */
    @Provides
    fun provideSubscriptionPlanListMapper(): SubscriptionPlanListMapper =
        ::toSubscriptionPlanList

    /**
     * Provide mega achievement mapper
     */
    @Provides
    fun provideMegaAchievementMapper(): MegaAchievementMapper = ::toMegaAchievement

    /**
     * Provide [RecentActionsMapper] mapper
     */
    @Provides
    fun provideRecentActionsMapper(): RecentActionsMapper = ::toRecentActionBucketList

    /**
     * Provide [UserSetMapper] mapper
     */
    @Provides
    fun provideUserSetMapper(): UserSetMapper = ::toUserSet
    /**
     * Provide [RecentActionBucketMapper] mapper
     */
    @Provides
    fun provideRecentActionBucketMapper(): RecentActionBucketMapper = ::toRecentActionBucket

}
