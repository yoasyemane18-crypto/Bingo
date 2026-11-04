package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite

/**
 * Enhanced receipt input text field that supports:
 * 1. Long-press in the field space showing a prominent 'Paste' context menu popup.
 * 2. Dedicated one-tap Paste button in the trailing icon and quick-paste chip.
 * 3. Clipboard reading and automatic feedback toast upon pasting.
 */
@Composable
fun PasteableReceiptTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String = "Telebirr Receipt ID or Link",
  placeholder: String = "https://transactioninfo.ethiotelecom.et/receipt/... or CL...",
  testTag: String = "pasteable_receipt_input"
) {
  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current
  var showPastePopup by remember { mutableStateOf(false) }

  fun executePaste() {
    val clipText = clipboard.getText()?.text?.trim()
    if (!clipText.isNullOrEmpty()) {
      onValueChange(clipText)
      Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
    }
    showPastePopup = false
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .pointerInput(Unit) {
          detectTapGestures(
            onLongPress = {
              showPastePopup = true
            }
          )
        }
    ) {
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        singleLine = true,
        trailingIcon = {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = BingoGold.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f)),
            modifier = Modifier
              .padding(end = 6.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable { executePaste() }
              .testTag("${testTag}_trailing_paste_btn")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Paste",
                tint = BingoGold,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Paste",
                color = BingoGoldLight,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag(testTag),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = BingoGold,
          unfocusedBorderColor = BingoBorderLight,
          focusedTextColor = BingoTextWhite,
          unfocusedTextColor = BingoTextWhite,
          cursorColor = BingoGold
        ),
        shape = RoundedCornerShape(12.dp)
      )

      // Floating 'Paste' Popup when long-pressed in the space
      if (showPastePopup) {
        Popup(
          alignment = Alignment.TopCenter,
          offset = IntOffset(0, -56),
          onDismissRequest = { showPastePopup = false }
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = BingoGold,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { executePaste() }
              .testTag("floating_longpress_paste_popup")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = null,
                tint = BingoTextDark,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Paste",
                color = BingoTextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }
        }
      }
    }

    // Helper text with instructions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Press & hold space to paste link",
        color = BingoTextMuted,
        fontSize = 11.sp
      )
      if (value.isNotEmpty()) {
        Text(
          text = "Clear",
          color = BingoGold,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable { onValueChange("") }
        )
      }
    }
  }
}
