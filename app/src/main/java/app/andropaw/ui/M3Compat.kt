package app.andropaw.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * AndroPaw — Material3 1.3 compatibility shims.
 *
 * The original UI was written against Material3 1.5-alpha and used the *Expressive* components
 * (`ToggleButton`, `ButtonGroupDefaults.connected*Shapes`, `LinearWavyProgressIndicator`,
 * `FloatingActionButtonMenu`, `LoadingIndicator`). Miuix 0.9.4 — the library behind the
 * HyperOS/Termux-Ultra look — only builds against the Material3 1.3.x baseline, where none of
 * those exist. These helpers reproduce the same visual result with stable 1.3.0 APIs.
 */

/** Gap used by a "connected" button group. */
val ConnectedButtonSpace = 2.dp

/** Corner geometry for a connected button group: rounded on the outer edges only. */
fun connectedButtonShapes(index: Int, lastIndex: Int): RoundedCornerShape = when (index) {
    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
    lastIndex -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp)
    else -> RoundedCornerShape(4.dp)
}

/**
 * Stable stand-in for the expressive `ToggleButton`: filled-tonal when selected, outlined when not.
 * Unlike the original it never toggles off by itself — the caller decides (`onCheckedChange`).
 */
@Composable
fun ConnectedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    if (checked) {
        FilledTonalButton(
            onClick = { onCheckedChange(true) },
            modifier = modifier,
            shape = shape,
            contentPadding = contentPadding,
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = { onCheckedChange(true) },
            modifier = modifier,
            shape = shape,
            contentPadding = contentPadding,
            content = content,
        )
    }
}
