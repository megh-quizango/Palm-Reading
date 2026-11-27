package app.palm.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSpace

/** Shows an entry marked with [bottomSheet] as an M3 bottom sheet over the entries beneath (ComingSoon, D-01). */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull() ?: return null
        if (!last.metadata.contains(BottomSheetKey)) return null
        val beneath = entries.dropLast(1)
        return BottomSheetScene(last.contentKey, last, beneath, beneath, onBack)
    }

    companion object {
        fun bottomSheet(): Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}

private data object BottomSheetKey : NavMetadataKey<Boolean>

private val SheetShape = RoundedCornerShape(topStart = NocturneRadius.Xl, topEnd = NocturneRadius.Xl)

private class BottomSheetScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    @OptIn(ExperimentalMaterial3Api::class)
    override val content: @Composable () -> Unit = {
        // Back, a scrim tap and a drag all end in onDismissRequest
        ModalBottomSheet(
            onDismissRequest = onBack,
            shape = SheetShape,
            containerColor = NocturneColors.BgRaised,
            scrimColor = NocturneColors.SurfaceScrim.copy(alpha = 0.6f),
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = NocturneSpace.S16)
                        .size(32.dp, 4.dp)
                        .background(NocturneSemantic.MutedForeground.copy(alpha = 0.5f), RoundedCornerShape(50)),
                )
            },
        ) { entry.Content() }
    }

    override fun equals(other: Any?): Boolean =
        other is BottomSheetScene<*> && key == other.key && entry == other.entry &&
            previousEntries == other.previousEntries && overlaidEntries == other.overlaidEntries

    override fun hashCode(): Int =
        ((key.hashCode() * 31 + entry.hashCode()) * 31 + previousEntries.hashCode()) * 31 + overlaidEntries.hashCode()
}
