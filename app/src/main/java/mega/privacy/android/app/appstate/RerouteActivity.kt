package mega.privacy.android.app.appstate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RerouteActivity : ComponentActivity() {

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val destination = runCatching { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) }
                .map { isSingleActivityEnabled ->
                    Timber.d("SingleActivity feature flag is enabled: $isSingleActivityEnabled")
                    if (isSingleActivityEnabled) MegaActivity::class.java else LoginActivity::class.java
                }
                .getOrDefault(LoginActivity::class.java)

            startActivity(Intent(this@RerouteActivity, destination).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}