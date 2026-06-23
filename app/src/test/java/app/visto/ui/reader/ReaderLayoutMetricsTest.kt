package app.visto.ui.reader

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLayoutMetricsTest {

    @Test
    fun contentPaddingReservesFooterAndBottomChromeOverlay() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        assertEquals(22.dp, padding.horizontalPadding)
        assertEquals(28.dp, padding.topContentPadding)
        assertEquals(140.dp, padding.bottomContentPadding)
        assertEquals(100.dp, padding.footerBottomPadding(chromeVisible = true))
        assertEquals(14.dp, padding.footerBottomPadding(chromeVisible = false))
        assertTrue(
            padding.bottomContentPadding >=
                padding.footerBottomPadding(chromeVisible = true) +
                ReaderLayoutMetrics.FooterHeightReserve +
                ReaderLayoutMetrics.FooterTextClearance,
        )
    }

    @Test
    fun viewportUsesTheSameReservedPaddingAsPageText() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1.25f),
        )

        assertEquals(692, viewport.widthPx)
        assertEquals(1264, viewport.heightPx)
        assertEquals(2.5f, viewport.density, 0.0f)
    }

    @Test
    fun compactScreensKeepOverlayReserveAndCompactSidePadding() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 320.dp, maxHeight = 560.dp)

        assertEquals(18.dp, padding.horizontalPadding)
        assertEquals(24.dp, padding.topContentPadding)
        assertEquals(140.dp, padding.bottomContentPadding)
    }

    @Test
    fun measuredFooterAndBottomBarHeightsExpandTheReservedOverlaySpace() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            measuredFooterHeight = 40.dp,
            measuredBottomBarHeight = 72.dp,
        )

        assertEquals(166.dp, padding.bottomContentPadding)
        assertEquals(114.dp, padding.footerBottomPadding(chromeVisible = true))
        assertTrue(
            padding.bottomContentPadding >=
                padding.footerBottomPadding(chromeVisible = true) +
                40.dp +
                ReaderLayoutMetrics.FooterTextClearance,
        )
    }
}
