package cz.jenda.wowforever.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Use the available content window, after system bars and navigation, not a model name
// or the panel's pixel resolution. Large text keeps a single comfortable reading pane.
internal fun supportsCompanionPane(widthDp:Float,heightDp:Float,fontScale:Float):Boolean =
    widthDp>=960f && heightDp>=480f && widthDp>=840f*fontScale.coerceAtLeast(1f)

internal fun companionPaneWidth(widthDp:Float,fontScale:Float):Float =
    (widthDp*.34f).coerceIn(360f*fontScale.coerceAtLeast(1f),480f*fontScale.coerceAtLeast(1f))

@Composable internal fun ReadingPane(content:@Composable ()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
        Box(Modifier.widthIn(max=840.dp).fillMaxWidth().fillMaxHeight()) { content() }
    }
}
