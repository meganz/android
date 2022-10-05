package mega.privacy.android.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.data.mapper.ContactDataMapper
import mega.privacy.android.data.mapper.StartScreenMapper
import mega.privacy.android.data.mapper.UserAlertMapper
import mega.privacy.android.data.mapper.UserLastGreenMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.mapMegaUserListToUserUpdate
import mega.privacy.android.data.mapper.toAccountType
import mega.privacy.android.data.mapper.toChatRequest
import mega.privacy.android.data.mapper.toContactData
import mega.privacy.android.data.mapper.toUserAlert
import mega.privacy.android.data.mapper.toUserUserLastGreen
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
}