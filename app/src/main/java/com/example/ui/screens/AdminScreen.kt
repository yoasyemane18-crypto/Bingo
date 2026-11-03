package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.UserProfile
import com.example.model.WinnerRecord
import com.example.model.WithdrawalRequest
import com.example.ui.components.LuxuryButton
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldDark
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoGreen
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoRed
import com.example.ui.theme.BingoRedLight
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun AdminScreen(
  isAdminLoggedIn: Boolean,
  paymentRequests: List<PaymentRequest>,
  withdrawalRequests: List<WithdrawalRequest>,
  allUsers: List<UserProfile>,
  winnersHistory: List<WinnerRecord>,
  onAdminLogin: (String, String) -> Unit,
  onAdminLogout: () -> Unit,
  onApprovePayment: (String, Int) -> Unit,
  onDeclinePayment: (String) -> Unit,
  onApproveWithdrawal: (String) -> Unit,
  onDeclineWithdrawal: (String) -> Unit,
  onAdjustBalance: (String, Int) -> Unit,
  onDeleteUser: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var selectedTab by remember { mutableIntStateOf(0) }
  var userSearchQuery by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    if (!isAdminLoggedIn) {
      // Admin Login Form
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BingoCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_login_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Admin Login",
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(18.dp))

          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Admin Email") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_email_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_password_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          LuxuryButton(
            text = "Login as Admin",
            onClick = { onAdminLogin(emailInput, passwordInput) },
            isGold = true,
            testTag = "admin_submit_login_button"
          )
        }
      }
    } else {
      // Admin Dashboard with Tabs
      val pendingPays = paymentRequests.filter { it.status == RequestStatus.PENDING }
      val pendingWds = withdrawalRequests.filter { it.status == RequestStatus.PENDING }

      val tabTitles = listOf(
        "Payments (${pendingPays.size})",
        "Users (${allUsers.size})",
        "Withdrawals (${pendingWds.size})",
        "Winners",
        "Logout"
      )

      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = BingoSurface,
        contentColor = BingoGold,
        edgePadding = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
      ) {
        tabTitles.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = {
              if (index == 4) {
                onAdminLogout()
              } else {
                selectedTab = index
              }
            },
            text = {
              Text(
                text = title,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                color = if (index == 4) BingoRedLight else if (selectedTab == index) BingoGold else BingoTextWhite
              )
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      when (selectedTab) {
        // Tab 0: Payments
        0 -> {
          Text(
            text = "Pending Payments (${pendingPays.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (pendingPays.isEmpty()) {
            Text("No pending payments.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            pendingPays.forEach { p ->
              var editAmt by remember(p.id) { mutableStateOf(p.amountBirr.toString()) }

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = "${p.username} · ${p.phone}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoGold
                  )
                  Text(
                    text = "Receipt: ${p.receiptId}",
                    fontSize = 14.sp,
                    color = BingoTextWhite
                  )
                  if (p.receiptLink.isNotEmpty()) {
                    Text(
                      text = p.receiptLink,
                      fontSize = 12.sp,
                      color = BingoTextMuted
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = editAmt,
                      onValueChange = { editAmt = it },
                      label = { Text("Birr") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.weight(1f),
                      singleLine = true,
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BingoTextWhite,
                        unfocusedTextColor = BingoTextWhite
                      )
                    )

                    Button(
                      onClick = {
                        val amt = editAmt.toIntOrNull() ?: p.amountBirr
                        onApprovePayment(p.id, amt)
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGreen),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      onClick = { onDeclinePayment(p.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoRed),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("Decline", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }

        // Tab 1: Users
        1 -> {
          OutlinedTextField(
            value = userSearchQuery,
            onValueChange = { userSearchQuery = it },
            placeholder = { Text("Search username or phone", color = BingoTextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          val filtered = allUsers.filter {
            it.username.contains(userSearchQuery, ignoreCase = true) ||
              it.phone.contains(userSearchQuery, ignoreCase = true)
          }

          if (filtered.isEmpty()) {
            Text("No users found.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            filtered.forEach { u ->
              var adjustInput by remember(u.username) { mutableStateOf("") }

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = "${u.username} · ${u.phone}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoGold
                  )
                  Text(
                    text = "Playing Time: ${u.playingTime}",
                    fontSize = 13.sp,
                    color = BingoTextMuted
                  )
                  Text(
                    text = "Wallet: ${u.walletBalance} birr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoTextWhite
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = adjustInput,
                      onValueChange = { adjustInput = it },
                      placeholder = { Text("birr") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.weight(1f),
                      singleLine = true,
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BingoTextWhite,
                        unfocusedTextColor = BingoTextWhite
                      )
                    )

                    Button(
                      onClick = {
                        val v = adjustInput.toIntOrNull() ?: 0
                        if (v > 0) onAdjustBalance(u.username, v)
                        adjustInput = ""
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGreen),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("+Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      onClick = {
                        val v = adjustInput.toIntOrNull() ?: 0
                        if (v > 0) onAdjustBalance(u.username, -v)
                        adjustInput = ""
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGoldDark),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("-Deduct", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Button(
                    onClick = { onDeleteUser(u.username) },
                    colors = ButtonDefaults.buttonColors(containerColor = BingoRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Delete User", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // Tab 2: Withdrawals
        2 -> {
          Text(
            text = "Pending Withdrawals (${pendingWds.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (pendingWds.isEmpty()) {
            Text("No pending withdrawals.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            pendingWds.forEach { w ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = "${w.username} · ${w.phone}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoGold
                  )
                  Text(
                    text = "Send ${w.amountBirr} birr to Telebirr: ${w.telebirrAccount}",
                    fontSize = 15.sp,
                    color = BingoTextWhite
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = { onApproveWithdrawal(w.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGreen),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Mark Paid", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      onClick = { onDeclineWithdrawal(w.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoRed),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Decline & Refund", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }

        // Tab 3: Winners
        3 -> {
          Text(
            text = "Winners (Last 24 hours): ${winnersHistory.size}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BingoGoldLight
          )
          Text(
            text = "Entries are automatically retained for 24 hours.",
            color = BingoTextMuted,
            fontSize = 13.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (winnersHistory.isEmpty()) {
            Text("No winners recorded yet.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            winnersHistory.forEach { rec ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "${rec.username} · ${rec.phone}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BingoGold
                  )
                  Text(
                    text = "Reward: ${rec.rewardBirr} birr · Card #${rec.cardNum} · ${rec.callCount} calls",
                    color = BingoTextWhite,
                    fontSize = 14.sp
                  )
                  Text(
                    text = "Game: ${rec.slotLabel}",
                    color = BingoTextMuted,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}
