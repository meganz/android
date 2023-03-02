package mega.privacy.android.data.mapper.changepassword

import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * Mapper for password strength
 */
fun interface PasswordStrengthMapper {
    /**
     * Invoke Password Strength Mapper
     * @param strength as password strength based on MegaApi
     */
    operator fun invoke(strength: Int): PasswordStrength
}

