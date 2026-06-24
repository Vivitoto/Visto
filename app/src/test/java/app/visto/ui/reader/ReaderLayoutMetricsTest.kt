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

        assertEquals(8.dp, padding.startContentPadding)
        assertEquals(8.dp, padding.endContentPadding)
        assertEquals(12.dp, padding.topContentPadding)
        assertEquals(52.dp, padding.bottomContentPadding)
        assertEquals(12.dp, padding.footerBottomPadding(chromeVisible = true))
        assertEquals(12.dp, padding.footerBottomPadding(chromeVisible = false))
        assertTrue(
            padding.bottomContentPadding >=
                padding.footerBottomPadding(chromeVisible = true) +
                ReaderLayoutMetrics.FooterHeightReserve +
                ReaderLayoutMetrics.FooterTextGap,
        )
    }

    @Test
    fun viewportHeightMatchesContentPaddingWithDefaultMargins() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1.25f),
        )

        // width: 390 - 8 - 8 = 374dp -> 748px at 2x
        assertEquals(748, viewport.widthPx)
        // height: 800 - 12 - 52 = 736dp -> 1472px at 2x
        assertEquals(1472, viewport.heightPx)
        assertEquals(2.5f, viewport.density, 0.0f)
    }

    @Test
    fun compactScreensKeepBaselineMarginsAndFooterReserve() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 320.dp, maxHeight = 560.dp)

        assertEquals(8.dp, padding.startContentPadding)
        assertEquals(8.dp, padding.endContentPadding)
        assertEquals(12.dp, padding.topContentPadding)
        assertEquals(52.dp, padding.bottomContentPadding)
    }

    @Test
    fun totalStartAndEndMarginsArePassedThrough() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            pageMargins = ReaderPageMargins(
                topDp = ReaderPageMargins.TOP_BASELINE_DP,
                bottomDp = ReaderPageMargins.BOTTOM_BASELINE_DP,
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
        // width: 390 - 30 - 42 = 318dp -> 636px
        assertEquals(636, viewport.widthPx)
    }

    @Test
    fun totalTopAndBottomMarginsArePassedThrough() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            pageMargins = ReaderPageMargins(
                topDp = 40,
                bottomDp = 70,
                startDp = ReaderPageMargins.HORIZONTAL_BASELINE_DP,
                endDp = ReaderPageMargins.HORIZONTAL_BASELINE_DP,
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
        // height: 800 - 40 - 70 = 690dp -> 1380px
        assertEquals(1380, viewport.heightPx)
    }

    @Test
    fun measuredFooterHeightExpandsTheReservedFooterSpace() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            measuredFooterHeight = 40.dp,
        )

        // bottomReserve = 12 + 40 + 12 = 64; default bottom margin is 52, so max = 64
        assertEquals(64.dp, padding.bottomContentPadding)
        assertEquals(12.dp, padding.footerBottomPadding(chromeVisible = true))
        // bottomBar: 12 + 40 + 10 = 62
        assertEquals(62.dp, padding.bottomBarBottomPadding)
        assertTrue(
            padding.bottomContentPadding >=
                padding.footerBottomPadding(chromeVisible = true) +
                40.dp +
                ReaderLayoutMetrics.FooterTextGap,
        )
    }

    @Test
    fun footerBottomPaddingDoesNotChangeWhenChromeToggles() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1f),
        )

        assertEquals(12.dp, padding.footerBottomPadding(chromeVisible = true))
        assertEquals(12.dp, padding.footerBottomPadding(chromeVisible = false))
        // bottomBar: 12 + 28 + 10 = 50
        assertEquals(50.dp, padding.bottomBarBottomPadding)
        // 800 - 12 - 52 = 736dp -> 1472px
        assertEquals(1472, viewport.heightPx)
    }
}
