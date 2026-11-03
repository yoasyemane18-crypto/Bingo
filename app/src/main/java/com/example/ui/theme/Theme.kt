package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val YemeBingoDarkColorScheme = darkColorScheme(
  primary = BingoGold,
  onPrimary = BingoTextDark,
  primaryContainer = BingoPurpleDeep,
  onPrimaryContainer = BingoGoldLight,
  secondary = BingoPurpleVibrant,
  onSecondary = BingoTextWhite,
  secondaryContainer = BingoSurface,
  onSecondaryContainer = BingoGoldLight,
  tertiary = BingoCoral,
  onTertiary = BingoTextWhite,
  background = BingoNight,
  onBackground = BingoTextWhite,
  surface = BingoCardBg,
  onSurface = BingoTextWhite,
  surfaceVariant = BingoSurface,
  onSurfaceVariant = BingoTextMuted,
  outline = BingoBorderPurple,
  outlineVariant = BingoBorderLight,
  error = BingoRed,
  onError = BingoTextWhite
)

val YemeBingoShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = YemeBingoDarkColorScheme,
    typography = AppTypography,
    shapes = YemeBingoShapes,
    content = content
  )
}
