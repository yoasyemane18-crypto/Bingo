package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.payment.PaymentMethod
import com.example.payment.telebirr.TelebirrOrder
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoPurpleVibrant
import com.example.ui.theme.BingoRedLight
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun TelebirrPaymentDialog(
  order: TelebirrOrder,
  isVerifying: Boolean,
  errorMessage: String?,
  onOpenPaymentApp: (PaymentMethod) -> Unit = {},
  onCopyDetails: (PaymentMethod) -> Unit = {},
  onVerifyReceipt: (receipt: String, method: PaymentMethod) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedMethod by remember { mutableStateOf(PaymentMethod.TELEBIRR) }
  var receiptInput by remember { mutableStateOf("") }
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, BingoGold),
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .testTag("telebirr_payment_gateway_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState)
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Header with Lock and Close Icon
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Secure",
              tint = BingoGold,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${selectedMethod.displayName} Gateway",
              fontFamily = PlayfairFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              color = BingoGoldLight,
              maxLines = 1
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = BingoTextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payment Method Selector Tabs (Telebirr, CBE, CBE Birr, E-Birr)
        Text(
          text = "Select Payment Method",
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp,
          color = BingoGoldLight,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PaymentMethod.entries.forEach { method ->
            val isSelected = selectedMethod == method
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) BingoGold else BingoNight,
              border = androidx.compose.foundation.BorderStroke(
                1.2.dp,
                if (isSelected) BingoGold else BingoBorderLight
              ),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedMethod = method
                  receiptInput = ""
                }
                .testTag("payment_method_${method.code}")
            ) {
              Box(
                modifier = Modifier.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = when (method) {
                    PaymentMethod.TELEBIRR -> "Telebirr"
                    PaymentMethod.CBE -> "CBE"
                    PaymentMethod.CBE_BIRR -> "CBE Birr"
                    PaymentMethod.E_BIRR -> "E-Birr"
                  },
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) BingoTextDark else BingoTextWhite,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Order Summary Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = BingoPurpleDeep,
          border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Game Entry · ${order.cartelaCount} Cartela(s) (All 3 Games)",
              color = BingoGoldLight,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )

            Text(
              text = "${order.totalAmountBirr} ETB",
              fontFamily = PlayfairFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 28.sp,
              color = BingoGold
            )

            Text(
              text = "Order: ${order.orderId}",
              color = BingoTextMuted,
              fontSize = 11.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Merchant Receiver Account Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = BingoSurface),
          border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Transfer to ${selectedMethod.displayName}:",
                  color = BingoTextMuted,
                  fontSize = 11.sp
                )
                Text(
                  text = "${selectedMethod.receiverName} · ${selectedMethod.receiverAccount}",
                  color = BingoTextWhite,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
              }

              Surface(
                modifier = Modifier
                  .clip(CircleShape)
                  .clickable {
                    onCopyDetails(selectedMethod)
                    Toast.makeText(
                      context,
                      "Copied ${selectedMethod.displayName} account (${selectedMethod.receiverAccount})",
                      Toast.LENGTH_SHORT
                    ).show()
                  },
                color = BingoPurpleDeep
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy number",
                  tint = BingoGold,
                  modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
                )
              }
            }

            if (selectedMethod.instruction.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = selectedMethod.instruction,
                color = BingoGoldLight.copy(alpha = 0.85f),
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Open Provider App / USSD Action Button
        Button(
          onClick = { onOpenPaymentApp(selectedMethod) },
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("launch_payment_app_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BingoPurpleVibrant)
        ) {
          Icon(
            imageVector = if (selectedMethod == PaymentMethod.CBE) Icons.Default.AccountBalance else Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Pay with ${selectedMethod.displayName} (${selectedMethod.ussdCode})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Receipt Verification Section
        val receiptPlaceholder = when (selectedMethod) {
          PaymentMethod.TELEBIRR -> "CL240899128, https://... or SMS text"
          PaymentMethod.CBE -> "FT24089..., *889# SMS or reference"
          PaymentMethod.CBE_BIRR -> "CB24089..., *847# SMS text"
          PaymentMethod.E_BIRR -> "EB24089..., *841# SMS text"
        }

        PasteableReceiptTextField(
          value = receiptInput,
          onValueChange = { receiptInput = it },
          label = "${selectedMethod.displayName} Receipt ID or SMS",
          placeholder = receiptPlaceholder,
          testTag = "telebirr_receipt_input"
        )

        if (!errorMessage.isNullOrEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = errorMessage,
            color = BingoRedLight,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Verify & Confirm Game Entry Button
        Button(
          onClick = {
            if (receiptInput.isNotBlank()) {
              onVerifyReceipt(receiptInput, selectedMethod)
            }
          },
          enabled = !isVerifying && receiptInput.isNotBlank(),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_telebirr_verification_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = BingoGold,
            contentColor = BingoTextDark,
            disabledContainerColor = BingoSurface,
            disabledContentColor = BingoTextMuted
          )
        ) {
          if (isVerifying) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = BingoTextDark,
              strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Verifying with ${selectedMethod.displayName}...",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          } else {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = BingoTextDark,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Verify & Enter Game",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
        }
      }
    }
  }
}
