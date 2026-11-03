package com.example.payment.telebirr

enum class PaymentOrderStatus {
  CREATED,
  PENDING_VERIFICATION,
  COMPLETED,
  FAILED,
  EXPIRED
}

data class TelebirrOrder(
  val orderId: String,
  val username: String,
  val payerPhone: String,
  val cartelaCount: Int,
  val totalAmountBirr: Int,
  val timestamp: Long = System.currentTimeMillis(),
  val status: PaymentOrderStatus = PaymentOrderStatus.CREATED,
  val securityToken: String = "",
  val receiptReference: String? = null
)

data class TelebirrVerificationResponse(
  val isSuccess: Boolean,
  val transactionId: String,
  val verifiedAmountBirr: Int,
  val payerPhone: String? = null,
  val receiverName: String = TelebirrConfig.MERCHANT_NAME,
  val receiverAccount: String = TelebirrConfig.MERCHANT_ACCOUNT,
  val timestamp: Long = System.currentTimeMillis(),
  val message: String,
  val isFraudSuspected: Boolean = false,
  val isDuplicate: Boolean = false,
  val isWrongReceiver: Boolean = false,
  val isInvalidReceipt: Boolean = false
)

sealed interface GatewayUiState {
  object Idle : GatewayUiState
  data class OrderCreated(val order: TelebirrOrder) : GatewayUiState
  data class Verifying(val orderId: String, val receiptId: String) : GatewayUiState
  data class PaymentSuccess(val order: TelebirrOrder, val verifiedAmount: Int, val receiptId: String) : GatewayUiState
  data class PaymentError(val message: String) : GatewayUiState
}
