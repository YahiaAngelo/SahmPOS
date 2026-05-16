package io.github.yahiaangelo.sahmpos.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SahmAmber = Color(0xFFFDB913)
private val SahmAmberDark = Color(0xFFDB9C02)
private val SahmAmberSoft = Color(0xFFFFE9A8)
private val SahmAmberContainer = Color(0xFFFFF4D6)
private val SahmInk = Color(0xFF1F1B16)
private val SahmInkSoft = Color(0xFF4A4640)
private val SahmCanvas = Color(0xFFFFFBF3)
private val SahmSurface = Color(0xFFFFFFFF)
private val SahmOutline = Color(0xFFE7DFD0)
private val SahmRed = Color(0xFFD9534F)

private val SahmLightColors = lightColorScheme(
    primary = SahmAmber,
    onPrimary = SahmInk,
    primaryContainer = SahmAmberContainer,
    onPrimaryContainer = SahmInk,
    secondary = SahmAmberDark,
    onSecondary = Color.White,
    secondaryContainer = SahmAmberSoft,
    onSecondaryContainer = SahmInk,
    tertiary = Color(0xFF198754),
    onTertiary = Color.White,
    background = SahmCanvas,
    onBackground = SahmInk,
    surface = SahmSurface,
    onSurface = SahmInk,
    surfaceVariant = Color(0xFFF7F1E3),
    onSurfaceVariant = SahmInkSoft,
    outline = SahmOutline,
    outlineVariant = Color(0xFFEFE6D2),
    error = SahmRed,
    onError = Color.White,
)

private val SahmDarkColors = darkColorScheme(
    primary = SahmAmber,
    onPrimary = SahmInk,
    primaryContainer = Color(0xFF5A4200),
    onPrimaryContainer = SahmAmberSoft,
    secondary = SahmAmberDark,
    onSecondary = SahmInk,
    background = Color(0xFF1A1612),
    onBackground = Color(0xFFF3EBD9),
    surface = Color(0xFF221D17),
    onSurface = Color(0xFFF3EBD9),
    surfaceVariant = Color(0xFF2B2520),
    onSurfaceVariant = Color(0xFFD8CDB7),
    outline = Color(0xFF5A4F3D),
    error = Color(0xFFE57373),
    onError = SahmInk,
)

@Composable
fun SahmTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SahmDarkColors else SahmLightColors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
