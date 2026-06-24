package app.visto.ui.reader

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLayoutMetricsTest {

    @Test
    fun contentPaddingReservesStableFooterClearance() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        assertEquals(22.dp, padding.startContentPadding)
        assertEquals(22.dp, padding.endContentPadding)
        assertEquals(28.dp, padding.topContentPadding)
        assertEquals(66.dp, padding.bottomContentPadding)
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
    fun viewportHeightMatchesContentPaddingRegardlessOfFontSettings() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1.25f),
        )

        assertEquals(692, viewport.widthPx)
        // height: 800 - 28(top) - 66(footer reserve) = 706dp -> 1412px at density 2x
        assertEquals(1412, viewport.heightPx)
        assertEquals(2.5f, viewport.density, 0.0f)
    }

    @Test
    fun compactScreensKeepDefaultMarginsAndFooterReserve() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 320.dp, maxHeight = 560.dp)

        assertEquals(22.dp, padding.startContentPadding)
        assertEquals(22.dp, padding.endContentPadding)
        assertEquals(28.dp, padding.topContentPadding)
        assertEquals(66.dp, padding.bottomContentPadding)
    }

    @Test
    fun viewportSubtractsExactStartAndEndMargins() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            pageMargins = ReaderPageMargins(
                topDp = 28,
                bottomDp = 66,
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
    fun viewportSubtractsExactTopAndBottomMargins() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            pageMargins = ReaderPageMargins(
                topDp = 40,
                bottomDp = 70,
                startDp = 22,
                endDp = 22,
            ),
        )

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1f),
        )

        assertEquals(40.dp, padding.topContentPadding)
        assertEquals(70.dp, padding.bottomContentPadding)
        // height: 800 - 40(top) - 70(bottom) = 690dp -> 1380px
        assertEquals(1380, viewport.heightPx)
    }

    @Test
    fun measuredFooterHeightExpandsTheReservedFooterSpace() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            measuredFooterHeight = 40.dp,
        )

        assertEquals(78.dp, padding.bottomContentPadding)
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
    fun footerReserveDoesNotChangeWhenChromeToggles() {
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
        assertEquals(1412, viewport.heightPx)
    }
}
