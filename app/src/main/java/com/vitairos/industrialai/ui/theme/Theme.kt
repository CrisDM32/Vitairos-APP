package com.vitairos.industrialai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta de la guía visual VITAIROS Industrial AI
val VerdeNoche = Color(0xFF081D16)
val VerdeProfundo = Color(0xFF102B23)
val VerdeMineral = Color(0xFF1B342D)
val Hueso = Color(0xFFFBF7F1)
val Niebla = Color(0xFFE8EFE9)
val Cobre = Color(0xFFDD7A52)
val Oro = Color(0xFFD9B46C)
val Acero = Color(0xFFAAB6B7)

/** Colores de estado: siempre acompañados de icono + texto. Texto oscuro para mantener contraste. */
val ChipBorrador = Niebla
val ChipEnCola = Color(0xFFF2E2B8)
val ChipEnviando = Color(0xFFD3DCDD)
val ChipSincronizado = Color(0xFFCDE6D3)
val ChipError = Color(0xFFF6CDBB)
val ChipCorreccion = Color(0xFFFBE7A8)

private val LightColors = lightColorScheme(
    primary = Cobre,
    onPrimary = VerdeNoche,
    primaryContainer = Color(0xFFF6D6C6),
    onPrimaryContainer = VerdeNoche,
    secondary = VerdeProfundo,
    onSecondary = Hueso,
    secondaryContainer = Niebla,
    onSecondaryContainer = VerdeNoche,
    tertiary = Oro,
    onTertiary = VerdeNoche,
    background = Hueso,
    onBackground = VerdeNoche,
    surface = Hueso,
    onSurface = VerdeNoche,
    surfaceVariant = Niebla,
    onSurfaceVariant = VerdeMineral,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Niebla,
    outline = Color(0xFF5E6F69),
    outlineVariant = Acero,
    error = Color(0xFFB3351A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Cobre,
    onPrimary = VerdeNoche,
    primaryContainer = Color(0xFF5A2F1D),
    onPrimaryContainer = Hueso,
    secondary = Oro,
    onSecondary = VerdeNoche,
    secondaryContainer = VerdeMineral,
    onSecondaryContainer = Hueso,
    tertiary = Oro,
    onTertiary = VerdeNoche,
    background = VerdeNoche,
    onBackground = Hueso,
    surface = VerdeNoche,
    onSurface = Hueso,
    surfaceVariant = VerdeMineral,
    onSurfaceVariant = Niebla,
    surfaceContainer = VerdeProfundo,
    surfaceContainerHigh = VerdeMineral,
    outline = Acero,
    outlineVariant = Color(0xFF3B534B),
    error = Color(0xFFFF9C80),
    onError = VerdeNoche,
)

private val Base = Typography()
private val VitairosTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 17.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun VitairosTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VitairosTypography,
        content = content,
    )
}
