package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.components.LuxuryButton
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoRed
import com.example.ui.theme.BingoRedLight
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun LoginScreen(
  language: AppLanguage,
  onLogin: (String, String) -> Boolean,
  onNavigateToRegister: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var usernameInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  fun performLogin() {
    errorMessage = null
    val u = usernameInput.trim()
    val p = passwordInput
    if (u.isEmpty()) {
      errorMessage = when (language) {
        AppLanguage.EN -> "Please enter your username."
        AppLanguage.AM -> "እባክዎን የተጠቃሚ ስምዎን ያስገቡ።"
        AppLanguage.TI -> "በይዞም ስም ተጠቃሚ የእትዉ።"
      }
      return
    }
    if (p.isEmpty()) {
      errorMessage = when (language) {
        AppLanguage.EN -> "Please enter your password."
        AppLanguage.AM -> "እባክዎን የይለፍ ቃልዎን ያስገቡ።"
        AppLanguage.TI -> "በይዞም መሕለፊ ቃል የእትዉ።"
      }
      return
    }

    isSubmitting = true
    focusManager.clearFocus()
    val success = onLogin(u, p)
    isSubmitting = false
    if (!success) {
      errorMessage = when (language) {
        AppLanguage.EN -> "Incorrect username or password. Please try again."
        AppLanguage.AM -> "የተሳሳተ የተጠቃሚ ስም ወይም የይለፍ ቃል። እባክዎ እንደገና ይሞክሩ።"
        AppLanguage.TI -> "ግጉይ ስም ተጠቃሚ ወይ መሕለፊ ቃል። በይዞም እንደገና ይፈትኑ።"
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Top Bar with Back Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(BingoPurpleDeep)
          .border(1.dp, BingoGold.copy(alpha = 0.4f), CircleShape)
          .testTag("login_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Home",
          tint = BingoGold
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = when (language) {
          AppLanguage.EN -> "Sign In"
          AppLanguage.AM -> "ግባ"
          AppLanguage.TI -> "እቶ"
        },
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = BingoGoldLight
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Main Login Card
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("signin_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Person Icon Badge
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(BingoPurpleDeep)
            .border(1.5.dp, BingoGold.copy(alpha = 0.7f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User icon",
            tint = BingoGold,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = when (language) {
            AppLanguage.EN -> "Welcome Back"
            AppLanguage.AM -> "እንኳን ደህና መጡ"
            AppLanguage.TI -> "እንኳዕ ደሓን መጻእኩም"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = when (language) {
            AppLanguage.EN -> "Sign in to play bingo, view your wallet, and win prizes"
            AppLanguage.AM -> "ቢንጎ ለመጫወት፣ ቦርሳዎን ለማየት እና ሽልማቶችን ለማሸነፍ ይግቡ"
            AppLanguage.TI -> "ቢንጎ ንምጽዋት፣ ቦርሳኹም ንምርኣይን ሽልማት ንምዕዋትን እተዉ"
          },
          color = BingoTextMuted,
          fontSize = 13.sp,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error message banner
        if (errorMessage != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(BingoRed.copy(alpha = 0.15f))
              .border(1.dp, BingoRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
              .padding(12.dp)
          ) {
            Text(
              text = errorMessage!!,
              color = BingoRedLight,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Username Field
        OutlinedTextField(
          value = usernameInput,
          onValueChange = {
            usernameInput = it
            errorMessage = null
          },
          label = {
            Text(
              when (language) {
                AppLanguage.EN -> "Username"
                AppLanguage.AM -> "የተጠቃሚ ስም"
                AppLanguage.TI -> "ስም ተጠቃሚ"
              }
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Username",
              tint = BingoGold
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("signin_username_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite,
            focusedLabelColor = BingoGold,
            unfocusedLabelColor = BingoTextMuted
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Field
        OutlinedTextField(
          value = passwordInput,
          onValueChange = {
            passwordInput = it
            errorMessage = null
          },
          label = {
            Text(
              when (language) {
                AppLanguage.EN -> "Password"
                AppLanguage.AM -> "የይለፍ ቃል"
                AppLanguage.TI -> "መሕለፊ ቃል"
              }
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Password",
              tint = BingoGold
            )
          },
          trailingIcon = {
            IconButton(
              onClick = { passwordVisible = !passwordVisible },
              modifier = Modifier.testTag("signin_password_visibility_toggle")
            ) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = BingoTextMuted
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(onDone = { performLogin() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("signin_password_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite,
            focusedLabelColor = BingoGold,
            unfocusedLabelColor = BingoTextMuted
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In Button
        LuxuryButton(
          text = if (isSubmitting) "..." else when (language) {
            AppLanguage.EN -> "⇥ Sign In"
            AppLanguage.AM -> "⇥ ግባ"
            AppLanguage.TI -> "⇥ እቶ"
          },
          onClick = { performLogin() },
          isGold = true,
          testTag = "signin_submit_button"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Don't have an account? Register link
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = when (language) {
              AppLanguage.EN -> "Don't have an account? "
              AppLanguage.AM -> "መለያ የለዎትም? "
              AppLanguage.TI -> "መለያ የብልኩምን? "
            },
            color = BingoTextMuted,
            fontSize = 14.sp
          )
          Text(
            text = when (language) {
              AppLanguage.EN -> "Register here"
              AppLanguage.AM -> "እዚህ ይመዝገቡ"
              AppLanguage.TI -> "ኣብዚ ተመዝገቡ"
            },
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
              .clickable { onNavigateToRegister() }
              .testTag("signin_to_register_link")
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}
