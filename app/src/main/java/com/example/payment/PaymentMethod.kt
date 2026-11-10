package com.example.payment

/**
 * Supported payment methods for Ethiopian mobile and bank payments
 */
enum class PaymentMethod(
  val code: String,
  val displayName: String,
  val ussdCode: String,
  val receiverAccount: String = "0939383425",
  val receiverName: String = "Yemane",
  val accountLabel: String = "Account / Phone Number",
  val instruction: String = ""
) {
  TELEBIRR(
    code = "telebirr",
    displayName = "Telebirr",
    ussdCode = "*127#",
    receiverAccount = "0939383425",
    receiverName = "Yemane",
    accountLabel = "Telebirr Mobile Number",
    instruction = "Transfer to Telebirr merchant 0939383425 (Yemane) via app or *127#"
  ),
  CBE(
    code = "cbe",
    displayName = "CBE",
    ussdCode = "*889#",
    receiverAccount = "0939383425",
    receiverName = "Yemane",
    accountLabel = "CBE Account / Phone",
    instruction = "Transfer via CBE Mobile Banking (*889#) to account holder Yemane (0939383425)"
  ),
  CBE_BIRR(
    code = "cbe_birr",
    displayName = "CBE Birr",
    ussdCode = "*847#",
    receiverAccount = "0939383425",
    receiverName = "Yemane",
    accountLabel = "CBE Birr Phone Number",
    instruction = "Transfer via CBE Birr (*847#) to mobile number 0939383425 (Yemane)"
  ),
  E_BIRR(
    code = "e_birr",
    displayName = "E-Birr",
    ussdCode = "*841#",
    receiverAccount = "0939383425",
    receiverName = "Yemane",
    accountLabel = "E-Birr Account / Phone Number",
    instruction = "Transfer via E-Birr app or USSD (*841#) to account holder Yemane (0939383425)"
  );

  companion object {
    fun fromCode(code: String?): PaymentMethod {
      if (code.isNullOrBlank()) return TELEBIRR
      return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
        ?: if (code.contains("cbe_birr", ignoreCase = true)) CBE_BIRR
        else if (code.contains("cbe", ignoreCase = true)) CBE
        else if (code.contains("e_birr", ignoreCase = true) || code.contains("ebirr", ignoreCase = true)) E_BIRR
        else TELEBIRR
    }
  }
}
