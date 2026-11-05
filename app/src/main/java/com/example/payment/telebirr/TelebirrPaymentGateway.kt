package com.example.payment.telebirr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.BingoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TelebirrPaymentGateway(
  private val context: Context,
  private val repository: BingoRepository = BingoRepository(context)
) {

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  /**
   * Initializes a new, signed payment order for game entry
   */
  fun createGameEntryOrder(
    username: String,
    payerPhone: String,
    cartelaCount: Int
  ): TelebirrOrder {
    val orderId = TelebirrSecurity.generateOrderReference()
    val totalBirr = cartelaCount * TelebirrConfig.BIRR_PER_CARTELA
    val timestamp = System.currentTimeMillis()
    val signature = TelebirrSecurity.generateOrderSignature(orderId, username, totalBirr, timestamp)

    return TelebirrOrder(
      orderId = orderId,
      username = username,
      payerPhone = payerPhone,
      cartelaCount = cartelaCount,
      totalAmountBirr = totalBirr,
      timestamp = timestamp,
      status = PaymentOrderStatus.CREATED,
      securityToken = signature
    )
  }

  /**
   * Securely verifies a Telebirr transaction receipt.
   *
   * Verifications performed:
   * 1. Format validity: Checks for valid Telebirr transaction ID or link (CL..., TB...).
   * 2. Anti-Repeat / Duplicate Prevention: Ensures the receipt has never been redeemed in the system.
   * 3. Receiver Verification: Confirms funds were sent to designated account holder Yemane (0939383425).
   * 4. Amount Rules: Minimum 30 Birr rule (30 Birr per cartela; below 30 Birr rejected).
   * 5. Online Handshake: Queries Ethio Telecom's electronic receipt portal when reachable.
   */
  suspend fun verifyTransaction(
    receiptRawInput: String,
    expectedAmountBirr: Int,
    existingReceipts: Set<String> = emptySet()
  ): TelebirrVerificationResponse = withContext(Dispatchers.IO) {
    val cleanInput = receiptRawInput.trim()

    // 1. Transaction ID extraction & syntax validity
    val transactionId = TelebirrSecurity.extractTransactionId(cleanInput)
    if (transactionId.isNullOrEmpty()) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = "",
        verifiedAmountBirr = 0,
        payerPhone = null,
        isInvalidReceipt = true,
        message = "Declined: Invalid Telebirr receipt format. Please enter a valid transaction ID (e.g., CL240899128) or official receipt link."
      )
    }

    // 2. Anti-Repeat / Duplicate check against system records
    if (existingReceipts.contains(transactionId) || repository.isReceiptRedeemed(transactionId)) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = 0,
        payerPhone = null,
        isDuplicate = true,
        isFraudSuspected = true,
        message = "Declined: Duplicate transaction! Receipt $transactionId has already been redeemed and cannot be reused."
      )
    }

    // 3. Minimum amount validation
    if (expectedAmountBirr < TelebirrConfig.MIN_PAYMENT_BIRR) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = expectedAmountBirr,
        message = "Declined: Payment of $expectedAmountBirr Birr is below the minimum required ${TelebirrConfig.MIN_PAYMENT_BIRR} Birr (30 Birr per cartela)."
      )
    }

    // 4. Strict input text verification (Receiver Account, Account Holder, and Amount)
    val recipientAudit = TelebirrSecurity.validateRecipient(cleanInput)
    val (textPhone, textName) = TelebirrSecurity.extractReceiverDetails(cleanInput)
    val textAmount = TelebirrSecurity.extractAmount(cleanInput)

    // Check receiver phone in input text
    if (textPhone != null && textPhone != TelebirrConfig.MERCHANT_ACCOUNT) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = 0,
        payerPhone = null,
        receiverName = textName ?: "Unknown",
        receiverAccount = textPhone,
        isWrongReceiver = true,
        isFraudSuspected = true,
        message = "Declined: Receipt shows payment transferred to account $textPhone, not to merchant account ${TelebirrConfig.MERCHANT_ACCOUNT} (${TelebirrConfig.MERCHANT_NAME})."
      )
    }

    // Check receiver name in input text
    if (textName != null && !textName.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) && !textName.contains("የማነ")) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = 0,
        payerPhone = null,
        receiverName = textName,
        receiverAccount = textPhone ?: "Unknown",
        isWrongReceiver = true,
        isFraudSuspected = true,
        message = "Declined: Receipt shows payment transferred to account holder $textName, not to ${TelebirrConfig.MERCHANT_NAME} (${TelebirrConfig.MERCHANT_ACCOUNT})."
      )
    }

    // Check amount in input text against the amount the user typed
    if (textAmount != null && textAmount != expectedAmountBirr) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = textAmount,
        isAmountMismatch = true,
        message = "Declined: Amount mismatch! You entered $expectedAmountBirr Birr, but the receipt shows $textAmount Birr."
      )
    }

    // 5. Online Verification against Ethio Telecom Electronic Receipt Portal
    var onlineVerified = false
    var finalVerifiedAmount = textAmount ?: expectedAmountBirr

    try {
      val receiptUrl = "${TelebirrConfig.ETHIO_TELECOM_RECEIPT_BASE_URL}$transactionId"
      val request = Request.Builder()
        .url(receiptUrl)
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) TelebirrVerifier/2.0")
        .build()

      val response = httpClient.newCall(request).execute()
      val responseBody = response.body?.string().orEmpty()

      if (response.isSuccessful && responseBody.isNotEmpty()) {
        val onlineData = TelebirrSecurity.parseEthioTelecomReceiptHtml(responseBody)
        if (!onlineData.isValid) {
          return@withContext TelebirrVerificationResponse(
            isSuccess = false,
            transactionId = transactionId,
            verifiedAmountBirr = 0,
            isInvalidReceipt = true,
            message = "Declined: ${onlineData.failureReason ?: "Receipt $transactionId was not found in Ethio Telecom records."}"
          )
        }

        // Online account number check
        if (onlineData.creditedPartyPhone != null && onlineData.creditedPartyPhone != TelebirrConfig.MERCHANT_ACCOUNT) {
          return@withContext TelebirrVerificationResponse(
            isSuccess = false,
            transactionId = transactionId,
            verifiedAmountBirr = 0,
            receiverAccount = onlineData.creditedPartyPhone,
            receiverName = onlineData.creditedPartyName ?: "Unknown",
            isWrongReceiver = true,
            isFraudSuspected = true,
            message = "Declined: Receipt shows funds credited to account ${onlineData.creditedPartyPhone}, not to official account ${TelebirrConfig.MERCHANT_ACCOUNT} (${TelebirrConfig.MERCHANT_NAME})."
          )
        }

        // Online account holder name check
        if (onlineData.creditedPartyName != null &&
          !onlineData.creditedPartyName!!.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) &&
          !onlineData.creditedPartyName!!.contains("የማነ")
        ) {
          return@withContext TelebirrVerificationResponse(
            isSuccess = false,
            transactionId = transactionId,
            verifiedAmountBirr = 0,
            receiverAccount = onlineData.creditedPartyPhone ?: TelebirrConfig.MERCHANT_ACCOUNT,
            receiverName = onlineData.creditedPartyName ?: "Unknown",
            isWrongReceiver = true,
            isFraudSuspected = true,
            message = "Declined: Receipt shows account holder is ${onlineData.creditedPartyName}, not ${TelebirrConfig.MERCHANT_NAME} (${TelebirrConfig.MERCHANT_ACCOUNT})."
          )
        }

        // Online amount check against the amount the user typed
        if (onlineData.amountBirr != null) {
          if (onlineData.amountBirr != expectedAmountBirr) {
            return@withContext TelebirrVerificationResponse(
              isSuccess = false,
              transactionId = transactionId,
              verifiedAmountBirr = onlineData.amountBirr,
              isAmountMismatch = true,
              message = "Declined: Amount mismatch! You entered $expectedAmountBirr Birr, but the official Ethio Telecom receipt is for ${onlineData.amountBirr} Birr."
            )
          }
          finalVerifiedAmount = onlineData.amountBirr
        }

        // Verify that the receipt confirms the official merchant account or holder
        val confirmedReceiver = (onlineData.creditedPartyPhone == TelebirrConfig.MERCHANT_ACCOUNT) ||
          (onlineData.creditedPartyName != null && (onlineData.creditedPartyName!!.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) || onlineData.creditedPartyName!!.contains("የማነ"))) ||
          responseBody.contains(TelebirrConfig.MERCHANT_ACCOUNT) ||
          responseBody.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) ||
          responseBody.contains("የማነ")

        if (!confirmedReceiver && textPhone == null && textName == null) {
          return@withContext TelebirrVerificationResponse(
            isSuccess = false,
            transactionId = transactionId,
            verifiedAmountBirr = 0,
            isWrongReceiver = true,
            message = "Declined: Cannot verify that payment was transferred to account holder ${TelebirrConfig.MERCHANT_NAME} (${TelebirrConfig.MERCHANT_ACCOUNT})."
          )
        }

        onlineVerified = true
      }
    } catch (_: Exception) {
      // Network timeout or unreachable: verify against validated SMS details
    }

    // 6. If online lookup did not connect, verify that recipient was confirmed in SMS
    if (!onlineVerified) {
      val confirmedInText = (textPhone == TelebirrConfig.MERCHANT_ACCOUNT) ||
        (textName != null && (textName.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) || textName.contains("የማነ"))) ||
        cleanInput.contains(TelebirrConfig.MERCHANT_ACCOUNT) ||
        cleanInput.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) ||
        cleanInput.contains("የማነ")

      if (!confirmedInText) {
        return@withContext TelebirrVerificationResponse(
          isSuccess = false,
          transactionId = transactionId,
          verifiedAmountBirr = 0,
          isWrongReceiver = true,
          message = "Declined: Cannot confirm transfer to account holder ${TelebirrConfig.MERCHANT_NAME} (${TelebirrConfig.MERCHANT_ACCOUNT}). Please paste the full Telebirr SMS receipt."
        )
      }

      if (textAmount != null && textAmount != expectedAmountBirr) {
        return@withContext TelebirrVerificationResponse(
          isSuccess = false,
          transactionId = transactionId,
          verifiedAmountBirr = textAmount,
          isAmountMismatch = true,
          message = "Declined: Amount mismatch! You entered $expectedAmountBirr Birr, but the receipt shows $textAmount Birr."
        )
      }
    }

    val onlineNote = if (onlineVerified) " (Live Ethio Telecom verified)" else ""

    TelebirrVerificationResponse(
      isSuccess = true,
      transactionId = transactionId,
      verifiedAmountBirr = finalVerifiedAmount,
      payerPhone = null,
      receiverName = TelebirrConfig.MERCHANT_NAME,
      receiverAccount = TelebirrConfig.MERCHANT_ACCOUNT,
      message = "✔ Telebirr payment of $finalVerifiedAmount Birr to ${TelebirrConfig.MERCHANT_NAME} (${TelebirrConfig.MERCHANT_ACCOUNT}) verified successfully$onlineNote."
    )
  }

  /**
   * Attempts to launch the Telebirr application, or falls back to USSD code dialer / web link
   */
  fun launchTelebirrPayment(order: TelebirrOrder): Boolean {
    // 1. Try launching official Telebirr app by package
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(TelebirrConfig.TELEBIRR_PACKAGE_NAME)
      ?: pm.getLaunchIntentForPackage(TelebirrConfig.TELEBIRR_HUAWEI_PACKAGE)

    if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(launchIntent)
      return true
    }

    // 2. Fallback: Launch phone dialer with USSD code *127#
    return try {
      val dialUri = Uri.parse("tel:" + Uri.encode(TelebirrConfig.USSD_TRANSFER_CODE))
      val dialIntent = Intent(Intent.ACTION_DIAL, dialUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(dialIntent)
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Copies payment instructions and receiver account to clipboard for seamless manual transfer
   */
  fun copyPaymentDetails(order: TelebirrOrder) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val details = "Telebirr Payment:\n" +
      "Receiver: ${TelebirrConfig.MERCHANT_NAME}\n" +
      "Phone: ${TelebirrConfig.MERCHANT_ACCOUNT}\n" +
      "Amount: ${order.totalAmountBirr} Birr\n" +
      "Reference: ${order.orderId}"

    val clip = ClipData.newPlainText("Telebirr Payment Info", details)
    clipboard.setPrimaryClip(clip)
  }

  /**
   * Copies just the merchant phone number for fast pasting into Telebirr
   */
  fun copyMerchantNumberOnly() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Telebirr Merchant Number", TelebirrConfig.MERCHANT_ACCOUNT)
    clipboard.setPrimaryClip(clip)
  }
}
