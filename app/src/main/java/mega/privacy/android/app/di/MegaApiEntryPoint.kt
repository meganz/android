package mega.privacy.android.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid

/**
 * Entry point to resolve the SDK singletons in classes that cannot use constructor
 * or field injection (legacy adapters, listeners and hand-instantiated Java classes).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MegaApiEntryPoint {

    /**
     * The account [MegaApiAndroid] instance.
     */
    @MegaApi
    fun megaApi(): MegaApiAndroid

    /**
     * The folder-link [MegaApiAndroid] instance.
     */
    @MegaApiFolder
    fun megaApiFolder(): MegaApiAndroid

    /**
     * The [MegaChatApiAndroid] instance.
     */
    fun megaChatApi(): MegaChatApiAndroid
}
