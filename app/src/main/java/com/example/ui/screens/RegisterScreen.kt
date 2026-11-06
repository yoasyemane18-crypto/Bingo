package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.Screen
import com.example.model.StringsDictionary
import com.example.ui.components.LuxuryButton
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun RegisterScreen(
  language: AppLanguage,
  onRegister: (String, String, String, String) -> Unit,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  var username by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var selectedTime by remember { mutableStateOf(StringsDictionary.SLOTS.first().label(language)) }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPassword by remember { mutableStateOf("") }
  var confirmPasswordVisible by remember { mutableStateOf(false) }
  var timeDropdownExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.35f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("register_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(BingoPurpleDeep)
            .border(1.dp, BingoGold.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = "Register",
            tint = BingoGold,
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = when (language) {
            AppLanguage.EN -> "User Registration"
            AppLanguage.AM -> "የተጠቃሚ ምዝገባ"
            AppLanguage.TI -> "ምዝገባ ተጠቃሚ"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Username
        OutlinedTextField(
          value = username,
          onValueChange = { username = it },
          label = {
            Text(
              when (language) {
                AppLanguage.EN -> "Username"
                AppLanguage.AM -> "የተጠቃሚ ስም"
                AppLanguage.TI -> "ስም ተጠቃሚ"
              }
            )
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_username_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedLabelColor = BingoGold,
            unfocusedLabelColor = BingoTextMuted,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Phone
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = {
            Text(
              when (language) {
                AppLanguage.EN -> "Phone No"
                AppLanguage.AM -> "ስልክ ቁጥር"
                AppLanguage.TI -> "ቁጽሪ ተሌፎን"
              }
            )
          },
          placeholder = { Text("09XXXXXXXX") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_phone_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedLabelColor = BingoGold,
            unfocusedLabelColor = BingoTextMuted,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Playing Time Selector Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = selectedTime,
            onValueChange = {},
            readOnly = true,
            label = {
              Text(
                when (language) {
                  AppLanguage.EN -> "Playing Time"
                  AppLanguage.AM -> "የመጫወቻ ሰዓት"
                  AppLanguage.TI -> "ናይ ጸወታ ሰዓት"
                }
              )
            },
            trailingIcon = {
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Time",
                tint = BingoGold,
                modifier = Modifier.clickable { timeDropdownExpanded = !timeDropdownExpanded }
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { timeDropdownExpanded = !timeDropdownExpanded }
              .testTag("register_time_select"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedLabelColor = BingoGold,
              unfocusedLabelColor = BingoTextMuted,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          DropdownMenu(
            expanded = timeDropdownExpanded,
            onDismissRequest = { timeDropdownExpanded = false },
            modifier = Modifier.background(BingoSurface)
          ) {
            StringsDictionary.SLOTS.forEach { slot ->
              val label = slot.label(language)
              DropdownMenuItem(
                text = { Text(label, color = BingoTextWhite, fontWeight = FontWeight.SemiBold) },
                onClick = {
                  selectedTime = label
                  timeDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Password
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = {
            Text(
              when (language) {
                AppLanguage.EN -> "Password"
                AppLanguage.AM -> "የይለፍ ቃል"
                AppLanguage.TI -> "መሕለፊ ቃል"
              }
            )
          },
          singleLine = true,
          trailingIcon = {
            IconButton(
              onClick = { passwordVisible = !passwordVisible },
              modifier = Modifier.testTag("register_password_visibility_toggle")
            ) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = BingoGoldLight
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_password_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedLabelColor = BingoGold,
            unfocusedLabelColor = BingoTextMuted,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it },
          label = {
            Text(
              when (language) {
                AppLanguage.EN -> "Confirm Password"
                AppLanguage.AM -> "የይለፍ ቃል ያረጋግጡ"
                AppLanguage.TI -> "መሕለፊ ቃል ኣረጋግጽ"
              }
            )
          },
          singleLine = true,
          trailingIcon = {
            IconButton(
              onClick = { confirmPasswordVisible = !confirmPasswordVisible },
              modifier = Modifier.testTag("register_confirm_password_visibility_toggle")
            ) {
              Icon(
                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                tint = BingoGoldLight
              )
            }
          },
          visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_confirm_password_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedLabelColor = BingoGold,
            unfocusedLabelColor = BingoTextMuted,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        LuxuryButton(
          text = when (language) {
            AppLanguage.EN -> "✚ Register"
            AppLanguage.AM -> "✚ ተመዝገብ"
            AppLanguage.TI -> "✚ ተመዝገብ"
          },
          onClick = {
            onRegister(username, phone, selectedTime, password)
          },
          isGold = false,
          testTag = "submit_register_button"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = when (language) {
              AppLanguage.EN -> "ⓘ Have an account? "
              AppLanguage.AM -> "ⓘ መለያ አለዎት? "
              AppLanguage.TI -> "ⓘ ሕሳብ ኣለኩም? "
            },
            color = BingoTextMuted,
            fontSize = 14.sp
          )
          Text(
            text = when (language) {
              AppLanguage.EN -> "Sign In"
              AppLanguage.AM -> "ግባ"
              AppLanguage.TI -> "እቶ"
            },
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
              .clickable { onNavigate(Screen.SIGN_IN) }
              .testTag("register_to_login_link")
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}
