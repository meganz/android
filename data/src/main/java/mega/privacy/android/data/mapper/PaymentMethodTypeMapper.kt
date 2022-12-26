package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.PaymentMethodType
import nz.mega.sdk.MegaApiJava

/**
 * Map [Int] to [PaymentMethodType]
 */
typealias PaymentMethodTypeMapper = (@JvmSuppressWildcards Int) -> @JvmSuppressWildcards PaymentMethodType?

/**
 * Map [Int] to [PaymentMethodType]. Return value can be subclass of [PaymentMethodType]
 */
internal fun toPaymentMethodType(type: Int): PaymentMethodType? = when (type) {
    MegaApiJava.PAYMENT_METHOD_BALANCE -> PaymentMethodType.BALANCE
    MegaApiJava.PAYMENT_METHOD_PAYPAL -> PaymentMethodType.PAYPAL
    MegaApiJava.PAYMENT_METHOD_ITUNES -> PaymentMethodType.ITUNES
    MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET -> PaymentMethodType.GOOGLE_WALLET
    MegaApiJava.PAYMENT_METHOD_BITCOIN -> PaymentMethodType.BITCOIN
    MegaApiJava.PAYMENT_METHOD_UNIONPAY -> PaymentMethodType.UNIONPAY
    MegaApiJava.PAYMENT_METHOD_FORTUMO -> PaymentMethodType.FORTUMO
    MegaApiJava.PAYMENT_METHOD_STRIPE -> PaymentMethodType.STRIPE
    MegaApiJava.PAYMENT_METHOD_CREDIT_CARD -> PaymentMethodType.CREDIT_CARD
    MegaApiJava.PAYMENT_METHOD_CENTILI -> PaymentMethodType.CENTILI
    MegaApiJava.PAYMENT_METHOD_PAYSAFE_CARD -> PaymentMethodType.PAYSAFE_CARD
    MegaApiJava.PAYMENT_METHOD_ASTROPAY -> PaymentMethodType.ASTROPAY
    MegaApiJava.PAYMENT_METHOD_RESERVED -> PaymentMethodType.RESERVED
    MegaApiJava.PAYMENT_METHOD_WINDOWS_STORE -> PaymentMethodType.WINDOWS_STORE
    MegaApiJava.PAYMENT_METHOD_TPAY -> PaymentMethodType.TPAY
    MegaApiJava.PAYMENT_METHOD_DIRECT_RESELLER -> PaymentMethodType.DIRECT_RESELLER
    MegaApiJava.PAYMENT_METHOD_ECP -> PaymentMethodType.ECP
    MegaApiJava.PAYMENT_METHOD_SABADELL -> PaymentMethodType.SABADELL
    MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET -> PaymentMethodType.HUAWEI_WALLET
    MegaApiJava.PAYMENT_METHOD_STRIPE2 -> PaymentMethodType.STRIPE2
    MegaApiJava.PAYMENT_METHOD_WIRE_TRANSFER -> PaymentMethodType.WIRE_TRANSFER
    else -> null
}