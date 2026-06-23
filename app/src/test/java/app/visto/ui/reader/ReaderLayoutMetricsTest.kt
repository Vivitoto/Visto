package app.visto.ui.reader

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLayoutMetricsTest {

    @Test
    fun contentPaddingReservesStableFooterAndLineSafety() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        assertEquals(22.dp, padding.startContentPadding)
        assertEquals(22.dp, padding.endContentPadding)
        assertEquals(28.dp, padding.topContentPadding)
        assertEquals(54.dp, padding.bottomContentPadding)
        assertEquals(ReaderLayoutMetrics.PageEndClearance, padding.pageEndClearance)
        assertEquals(14.dp, padding.footerBottomPadding(chromeVisible = true))
        assertEquals(14.dp, padding.footerBottomPadding(chromeVisible = false))
        assertTrue(
            padding.bottomContentPadding >=
                padding.footerBottomPadding(chromeVisible = true) +
                ReaderLayoutMetrics.FooterHeightReserve +
                ReaderLayoutMetrics.FooterTextClearance,
        )
    }

    @Test
    fun pageEndClearanceIsFixedRegardlessOfFontSettings() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1.25f),
        )

        // pageEndClearance is a fixed 28dp constant, not font-dependent
        assertEquals(ReaderLayoutMetrics.PageEndClearance, padding.pageEndClearance)
        assertEquals(692, viewport.widthPx)
        // height: 800 - 28(top) - 54(bottom) - 28(fixed pageEndClearance) = 690dp → 1725px at density 2.5
        assertEquals(1725, viewport.heightPx)
        assertEquals(2.5f, viewport.density, 0.0f)
    }

    @Test
    fun compactScreensKeepDefaultMarginsAndFooterReserve() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 320.dp, maxHeight = 560.dp)

        assertEquals(22.dp, padding.startContentPadding)
        assertEquals(22.dp, padding.endContentPadding)
        assertEquals(28.dp, padding.topContentPadding)
        assertEquals(54.dp, padding.bottomContentPadding)
    }

    @Test
    fun viewportSubtractsExactStartAndEndMargins() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            pageMargins = ReaderPageMargins(
                topDp = 28,
                bottomDp = 54,
                startDp = 30,
                endDp = 42,
            ),
        )

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1f),
        )

        assertEquals(30.dp, padding.startContentPadding)
        assertEquals(42.dp, padding.endContentPadding)
        assertEquals(636, viewport.widthPx)
    }

    @Test
    fun measuredFooterHeightExpandsTheReservedFooterSpace() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            measuredFooterHeight = 40.dp,
        )

        assertEquals(66.dp, padding.bottomContentPadding)
        assertEquals(ReaderLayoutMetrics.PageEndClearance, padding.pageEndClearance)
        assertEquals(14.dp, padding.footerBottomPadding(chromeVisible = true))
        assertEquals(64.dp, padding.bottomBarBottomPadding)
        assertTrue(
            padding.bottomContentPadding >=
                padding.footerBottomPadding(chromeVisible = true) +
                40.dp +
                ReaderLayoutMetrics.FooterTextClearance,
        )
    }

    @Test
    fun bottomChromeFloatsAboveStableFooterWithoutChangingViewport() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1f),
        )

        assertEquals(14.dp, padding.footerBottomPadding(chromeVisible = true))
        assertEquals(14.dp, padding.footerBottomPadding(chromeVisible = false))
        assertEquals(52.dp, padding.bottomBarBottomPadding)
        assertEquals(1382, viewport.heightPx)
    }

    @Test
    fun pageEndClearanceIsFixedAt28dp() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)
        assertEquals(28.dp, padding.pageEndClearance)
    }
}
