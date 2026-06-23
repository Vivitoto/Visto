package app.visto.ui.layout

import androidx.compose.ui.unit.dp
import app.visto.data.account.GridDensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VistoLayoutMetricsTest {

    @Test
    fun scrollEndPaddingUsesMeasuredOverlayHeight() {
        val padding = VistoLayoutMetrics.scrollEndPadding(bottomOverlayHeight = 32.dp)

        assertEquals(64.dp, padding)
    }

    @Test
    fun scrollEndPaddingUsesMeasuredFloatingActionButtonHeight() {
        val padding = VistoLayoutMetrics.scrollEndPadding(
            floatingActionButtonHeight = VistoLayoutMetrics.DefaultFloatingActionButtonHeight,
        )

        assertEquals(96.dp, padding)
    }

    @Test
    fun scrollEndPaddingKeepsLargestBottomReserve() {
        val padding = VistoLayoutMetrics.scrollEndPadding(
            bottomOverlayHeight = 72.dp,
            floatingActionButtonHeight = VistoLayoutMetrics.DefaultFloatingActionButtonHeight,
        )

        assertEquals(104.dp, padding)
    }

    @Test
    fun formAndSettingsStackActionsOnlyOnNarrowScreens() {
        val narrowForm = VistoLayoutMetrics.formContent(maxWidth = 320.dp)
        val regularForm = VistoLayoutMetrics.formContent(maxWidth = 430.dp)
        val wideSettings = VistoLayoutMetrics.settingsContent(maxWidth = 720.dp)

        assertEquals(12.dp, narrowForm.horizontalPadding)
        assertTrue(narrowForm.stackActions)
        assertFalse(regularForm.stackActions)
        assertEquals(24.dp, wideSettings.horizontalPadding)
        assertEquals(720.dp, wideSettings.maxContentWidth)
    }

    @Test
    fun albumGridDensityMapsToAdaptiveMinimumCellWidths() {
        assertEquals(150.dp, VistoLayoutMetrics.albumGridMinCellWidth(GridDensity.COMFORTABLE))
        assertEquals(112.dp, VistoLayoutMetrics.albumGridMinCellWidth(GridDensity.STANDARD))
        assertEquals(72.dp, VistoLayoutMetrics.albumGridMinCellWidth(GridDensity.COMPACT))
    }

    @Test
    fun viewerOverlayTightensOffsetsOnCompactScreens() {
        val compact = VistoLayoutMetrics.viewerOverlay(maxWidth = 320.dp, maxHeight = 560.dp)
        val regular = VistoLayoutMetrics.viewerOverlay(maxWidth = 390.dp, maxHeight = 800.dp)

        assertEquals(8.dp, compact.edgePadding)
        assertEquals(8.dp, compact.topPadding)
        assertEquals(16.dp, compact.bottomPadding)
        assertEquals(12.dp, regular.edgePadding)
        assertEquals(24.dp, regular.bottomPadding)
    }
}
