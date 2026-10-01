package mega.privacy.android.domain.entity.account

/**
 * Account detail
 *
 * @property storageDetail
 * @property sessionDetail
 * @property transferDetail
 * @property levelDetail
 */
data class AccountDetail(
    val storageDetail: AccountStorageDetail? = null,
    val sessionDetail: AccountSessionDetail? = null,
    val transferDetail: AccountTransferDetail? = null,
    val levelDetail: AccountLevelDetail? = null,
)