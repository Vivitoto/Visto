package app.visto.ui.reader

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLayoutMetricsTest {

    @Test
    fun contentPaddingReservesStableFooterAndLineSafety() {
        val textSafety = ReaderLayoutMetrics.textBottomSafetyPadding(
            fontSizeSp = 18,
            lineSpacing = 1.5f,
            fontScale = 1f,
        )
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 390.dp, maxHeight = 800.dp)

        assertEquals(22.dp, padding.horizontalPadding)
        assertEquals(28.dp, padding.topContentPadding)
        assertEquals(54.dp, padding.bottomContentPadding)
        assertEquals(textSafety, padding.pageEndClearance)
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
    fun viewportUsesTheSameFontScaledReservedPaddingAsPageText() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            textBottomSafetyPadding = ReaderLayoutMetrics.textBottomSafetyPadding(
                fontSizeSp = 18,
                lineSpacing = 1.5f,
                fontScale = 1.25f,
            ),
        )

        val viewport = ReaderLayoutMetrics.viewport(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            padding = padding,
            density = Density(density = 2f, fontScale = 1.25f),
        )

        assertEquals(692, viewport.widthPx)
        assertEquals(1369, viewport.heightPx)
        assertEquals(2.5f, viewport.density, 0.0f)
    }

    @Test
    fun compactScreensKeepFooterReserveAndCompactSidePadding() {
        val padding = ReaderLayoutMetrics.contentPadding(maxWidth = 320.dp, maxHeight = 560.dp)

        assertEquals(18.dp, padding.horizontalPadding)
        assertEquals(24.dp, padding.topContentPadding)
        assertEquals(54.dp, padding.bottomContentPadding)
    }

    @Test
    fun measuredFooterHeightExpandsTheReservedFooterSpace() {
        val padding = ReaderLayoutMetrics.contentPadding(
            maxWidth = 390.dp,
            maxHeight = 800.dp,
            measuredFooterHeight = 40.dp,
        )

        assertEquals(66.dp, padding.bottomContentPadding)
        assertEquals(ReaderLayoutMetrics.DefaultTextBottomSafetyPadding, padding.pageEndClearance)
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
    fun textSafetyPaddingCoversStyledTitleOverheadAtTightLineSpacing() {
        val safety = ReaderLayoutMetrics.textBottomSafetyPadding(
            fontSizeSp = 14,
            lineSpacing = 1.0f,
            fontScale = 1f,
        )

        assertTrue(safety > 14.dp)
        assertTrue(safety >= ReaderLayoutMetrics.FooterTextClearance)
    }
}
