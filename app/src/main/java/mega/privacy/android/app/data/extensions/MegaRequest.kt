package mega.privacy.android.app.data.extensions

import nz.mega.sdk.MegaRequest
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
fun MegaRequest?.isType(type: Int): Boolean {
    contract {
        returns(true) implies (this@isType != null)
    }
    return this?.type == type
}

fun MegaRequest?.hasParam(parameterType: Int): Boolean = this?.paramType == parameterType

@ExperimentalContracts
fun MegaRequest?.isTypeWithParam(type: Int, parameterType: Int): Boolean =
    isType(type) && hasParam(parameterType)
