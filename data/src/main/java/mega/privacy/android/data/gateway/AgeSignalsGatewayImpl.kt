package mega.privacy.android.data.gateway

import android.content.Context
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [AgeSignalsGateway]
 *
 * Wraps the Google Play Age Signals SDK to provide a testable interface.
 * Extracts the user status from the result to avoid exposing AgeSignalsResult.
 */
@Singleton
internal class AgeSignalsGatewayImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AgeSignalsGateway {

    private val ageSignalsManager: AgeSignalsManager by lazy {
        AgeSignalsManagerFactory.create(context)
    }

    override suspend fun checkAgeSignals(): Int? =
        ageSignalsManager.checkAgeSignals(AgeSignalsRequest.builder().build()).await()?.userStatus()
}

