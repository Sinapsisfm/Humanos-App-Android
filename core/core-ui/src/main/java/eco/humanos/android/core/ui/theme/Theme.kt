package eco.humanos.android.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val HumanosLightColorScheme = lightColorScheme(
    primary = HumanosPrimary,
    onPrimary = HumanosOnPrimary,
    primaryContainer = HumanosSecondary,
    secondary = HumanosSecondary,
    onSecondary = HumanosOnSecondary,
    error = HumanosError,
    onError = HumanosOnError,
    background = HumanosBackground,
    onBackground = HumanosOnBackground,
    surface = HumanosSurface,
    onSurface = HumanosOnSurface,
)

private val HumanosDarkColorScheme = darkColorScheme(
    primary = HumanosPrimaryDark,
    onPrimary = HumanosOnPrimaryDark,
    primaryContainer = HumanosPrimary,
    secondary = HumanosSecondaryDark,
    onSecondary = HumanosOnPrimary,
    error = HumanosError,
    onError = HumanosOnError,
    background = HumanosBackgroundDark,
    onBackground = HumanosOnBackgroundDark,
    surface = HumanosSurfaceDark,
    onSurface = HumanosOnSurfaceDark,
)

@Composable
fun HumanosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> HumanosDarkColorScheme
        else -> HumanosLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HumanosTypography,
        content = content,
    )
}
