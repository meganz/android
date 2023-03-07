package mega.privacy.android.app.presentation.verification.model.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import javax.inject.Inject

/**
 * Implementation of [SMSVerificationTextMapper]
 */
class SMSVerificationTextMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SMSVerificationTextMapper {
    override fun invoke(state: SMSVerificationUIState): SMSVerificationUIState {
        val infoText = when {
            state.isUserLocked -> context.getString(R.string.verify_account_helper_locked)
            state.isAchievementsEnabled -> context.getString(
                R.string.sms_add_phone_number_dialog_msg_achievement_user,
                state.bonusStorageSMS ?: "GB"
            )
            else -> context.getString(R.string.sms_add_phone_number_dialog_msg_non_achievement_user)
        }

        val headerText = context.getString(
            when (state.isUserLocked) {
                true -> R.string.verify_account_title
                else -> R.string.add_phone_number_label
            }
        )

        val countryCodeText = when (state.isCountryCodeValid) {
            true -> "${state.selectedCountryName} (${state.selectedDialCode})"
            else -> context.getString(
                R.string.sms_region_label
            )
        }

        return state.copy(
            headerText = headerText,
            infoText = infoText,
            countryCodeText = countryCodeText,
        )
    }
}
