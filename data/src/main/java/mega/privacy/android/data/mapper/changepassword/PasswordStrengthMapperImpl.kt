package mega.privacy.android.data.mapper.changepassword

import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Password Strength Mapper Implementation
 */
class PasswordStrengthMapperImpl @Inject constructor() : PasswordStrengthMapper {
    override fun invoke(strength: Int): PasswordStrength =
        passwordStrengthLevel[strength] ?: PasswordStrength.INVALID

    companion object {
        /**
         * The level of password strength based on the MegaApi
         */
        internal val passwordStrengthLevel = hashMapOf(
            MegaApiJava.PASSWORD_STRENGTH_VERYWEAK to PasswordStrength.VERY_WEAK,
            MegaApiJava.PASSWORD_STRENGTH_WEAK to PasswordStrength.WEAK,
            MegaApiJava.PASSWORD_STRENGTH_MEDIUM to PasswordStrength.MEDIUM,
            MegaApiJava.PASSWORD_STRENGTH_GOOD to PasswordStrength.GOOD,
            MegaApiJava.PASSWORD_STRENGTH_STRONG to PasswordStrength.STRONG
        )
    }
}