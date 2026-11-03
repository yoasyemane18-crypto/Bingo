package com.example.payment.telebirr

/**
 * Configuration for Telebirr Payment Gateway Integration
 */
object TelebirrConfig {
  const val MERCHANT_NAME = "Yemane"
  const val MERCHANT_ACCOUNT = "0939383425"
  const val MERCHANT_PHONE = "0939383425"
  const val CURRENCY = "ETB"
  const val BIRR_PER_CARTELA = 30
  const val MIN_PAYMENT_BIRR = 30

  // Telebirr deep link URI schemes and Android package identifiers
  const val TELEBIRR_PACKAGE_NAME = "cn.tydic.ethiopay" // Official Telebirr package
  const val TELEBIRR_HUAWEI_PACKAGE = "com.huawei.wallet"
  const val TELEBIRR_DEEP_LINK_SCHEME = "telebirr://pay"
  const val USSD_TRANSFER_CODE = "*127#"

  // Verification Gateway URL for Ethio Telecom electronic receipts
  const val ETHIO_TELECOM_RECEIPT_BASE_URL = "https://transactioninfo.ethiotelecom.et/receipt/"

  // Cryptographic salt for transaction signature verification
  const val GATEWAY_SECURITY_SALT = "YEME_BINGO_TELEBIRR_SECURE_SALT_2026_V2"
}
