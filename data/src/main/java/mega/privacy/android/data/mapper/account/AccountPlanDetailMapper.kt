package mega.privacy.android.data.mapper.account

import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import nz.mega.sdk.MegaAccountPlan
import javax.inject.Inject

/**
 * Account Plan Detail Mapper
 */
internal class AccountPlanDetailMapper @Inject constructor(
    private val accountTypeMapper: AccountTypeMapper,
    private val stringListMapper: StringListMapper,
) {
    /**
     * Invoke
     *
     * @param planDetail [MegaAccountPlan]
     * @return [AccountPlanDetail]
     */
    operator fun invoke(
        planDetail: MegaAccountPlan?,
    ) = planDetail?.let { plan ->
        AccountPlanDetail(
            accountType = accountTypeMapper(plan.accountLevel.toInt()),
            isProPlan = plan.isProPlan,
            expirationTime = plan.expirationTime,
            subscriptionId = plan.id,
            featuresList = stringListMapper(plan.features),
            isFreeTrial = plan.isTrial,
        )
    }
}