package app.visto.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.visto.data.account.GridDensity

internal data class ScreenContentMetrics(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val maxContentWidth: Dp,
    val stackActions: Boolean,
)

internal data class ViewerOverlayMetrics(
    val edgePadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val infoBarMaxWidth: Dp,
    val actionMaxWidth: Dp,
)

internal object VistoLayoutMetrics {
    private val CompactWidthThreshold = 360.dp
    private val ExpandedWidthThreshold = 600.dp
    private val StackActionsThreshold = 430.dp
    private val FormMaxContentWidth = 560.dp
    private val SettingsMaxContentWidth = 720.dp

    internal val DefaultFloatingActionButtonHeight = 56.dp
    private val ScrollEndBasePadding = 16.dp
    private val OverlayClearance = 16.dp
    private val FloatingActionButtonClearance = 24.dp

    private val AlbumComfortableMinCellWidth = 150.dp
    private val AlbumStandardMinCellWidth = 112.dp
    private val AlbumCompactMinCellWidth = 72.dp

    fun formContent(maxWidth: Dp): ScreenContentMetrics =
        ScreenContentMetrics(
            horizontalPadding = screenHorizontalPadding(maxWidth),
            topPadding = 16.dp,
            bottomPadding = 20.dp,
            maxContentWidth = FormMaxContentWidth,
            stackActions = shouldStackActions(maxWidth),
        )

    fun settingsContent(maxWidth: Dp): ScreenContentMetrics =
        ScreenContentMetrics(
            horizontalPadding = screenHorizontalPadding(maxWidth),
            topPadding = if (maxWidth < CompactWidthThreshold) 12.dp else 14.dp,
            bottomPadding = 16.dp,
            maxContentWidth = SettingsMaxContentWidth,
            stackActions = shouldStackActions(maxWidth),
        )

    fun shouldStackActions(maxWidth: Dp): Boolean = maxWidth < StackActionsThreshold

    fun scrollEndPadding(
        bottomOverlayHeight: Dp = 0.dp,
        floatingActionButtonHeight: Dp = 0.dp,
    ): Dp {
        val overlayReserve = if (bottomOverlayHeight > 0.dp) {
            bottomOverlayHeight + OverlayClearance
        } else {
            0.dp
        }
        val fabReserve = if (floatingActionButtonHeight > 0.dp) {
            floatingActionButtonHeight + FloatingActionButtonClearance
        } else {
            0.dp
        }
        return ScrollEndBasePadding + maxOf(overlayReserve, fabReserve)
    }

    fun albumGridMinCellWidth(gridDensity: GridDensity): Dp = when (gridDensity) {
        GridDensity.COMFORTABLE -> AlbumComfortableMinCellWidth
        GridDensity.STANDARD -> AlbumStandardMinCellWidth
        GridDensity.COMPACT -> AlbumCompactMinCellWidth
    }

    fun viewerOverlay(maxWidth: Dp, maxHeight: Dp): ViewerOverlayMetrics =
        ViewerOverlayMetrics(
            edgePadding = if (maxWidth < CompactWidthThreshold) 8.dp else 12.dp,
            topPadding = if (maxHeight < 600.dp) 8.dp else 12.dp,
            bottomPadding = if (maxHeight < 600.dp) 16.dp else 24.dp,
            infoBarMaxWidth = 560.dp,
            actionMaxWidth = if (maxWidth < CompactWidthThreshold) 312.dp else 360.dp,
        )

    private fun screenHorizontalPadding(maxWidth: Dp): Dp = when {
        maxWidth < CompactWidthThreshold -> 12.dp
        maxWidth >= ExpandedWidthThreshold -> 24.dp
        else -> 16.dp
    }
}
