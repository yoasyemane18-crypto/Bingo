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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.payment.telebirr.TelebirrConfig
import com.example.payment.telebirr.TelebirrOrder
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldDark
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoGreen
import com.example.ui.theme.BingoGreenLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoPurpleVibrant
import com.example.ui.theme.BingoRedLight
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoSurfaceLight
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun TelebirrPaymentDialog(
  order: TelebirrOrder,
  isVerifying: Boolean,
  errorMessage: String?,
  onOpenTelebirrApp: () -> Unit,
  onCopyDetails: () -> Unit,
  onVerifyReceipt: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var receiptInput by remember { mutableStateOf("") }
  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, BingoGold),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .testTag("telebirr_payment_gateway_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Header with Close icon
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Secure",
              tint = BingoGold,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Telebirr Secure Gateway",
              fontFamily = PlayfairFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = BingoGoldLight
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
              .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Game Entry · ${order.cartelaCount} Cartela(s)",
              color = BingoGoldLight,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )

            Text(
              text = "${order.totalAmountBirr} ETB",
              fontFamily = PlayfairFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 32.sp,
              color = BingoGold
            )

            Text(
              text = "Order: ${order.orderId}",
              color = BingoTextMuted,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Merchant Receiver Account Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = BingoSurface),
          border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Transfer to Telebirr Account:",
                color = BingoTextMuted,
                fontSize = 12.sp
              )
              Text(
                text = "${TelebirrConfig.MERCHANT_NAME} · ${TelebirrConfig.MERCHANT_ACCOUNT}",
                color = BingoTextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }

            Surface(
              modifier = Modifier
                .clip(CircleShape)
                .clickable {
                  onCopyDetails()
                  Toast.makeText(context, "Number copied to clipboard", Toast.LENGTH_SHORT).show()
                },
              color = BingoPurpleDeep
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy number",
                tint = BingoGold,
                modifier = Modifier
                  .padding(8.dp)
                  .size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Open Telebirr App / USSD Action Button
        Button(
          onClick = onOpenTelebirrApp,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("launch_telebirr_app_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BingoPurpleVibrant)
        ) {
          Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Pay via Telebirr App / USSD",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Receipt Verification Section
        OutlinedTextField(
          value = receiptInput,
          onValueChange = { receiptInput = it },
          label = { Text("Telebirr Receipt ID or Link") },
          placeholder = { Text("CL240899128 or SMS text") },
          singleLine = true,
          trailingIcon = {
            Text(
              text = "Paste",
              color = BingoGold,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              modifier = Modifier
                .clickable {
                  val clipText = clipboard.getText()?.text
                  if (!clipText.isNullOrEmpty()) {
                    receiptInput = clipText
                  }
                }
                .padding(end = 12.dp)
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("telebirr_receipt_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite
          ),
          shape = RoundedCornerShape(12.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Verify & Confirm Game Entry Button
        Button(
          onClick = {
            if (receiptInput.isNotBlank()) {
              onVerifyReceipt(receiptInput)
            }
          },
          enabled = !isVerifying && receiptInput.isNotBlank(),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
              modifier = Modifier.size(24.dp),
              color = BingoTextDark,
              strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Verifying with Telebirr...",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
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
              fontSize = 16.sp
            )
          }
        }
      }
    }
  }
}
