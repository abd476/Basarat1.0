package com.fyp.basarattradeinvest.feature.markets

import com.fyp.basarattradeinvest.domain.model.market.MarketOverview
import com.fyp.basarattradeinvest.domain.model.sentiment.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.stock.SymbolFilterType
import com.fyp.basarattradeinvest.domain.model.live.WsConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Property 13: UiState Default Compatibility for MarketDashboardUiState.
 *
 * Validates: Requirements 4.3
 */
class MarketDashboardUiStateTest {

    private fun makeSuccess() = MarketDashboardUiState.Success(
        indices = emptyList(),
        overview = MarketOverview(
            tradedValue = "0",
            tradedVolume = "0",
            totalTrades = "0",
            upCount = 0,
            downCount = 0,
            unchangedCount = 0,
            totalStocks = 0
        ),
        topSymbols = emptyList(),
        selectedFilter = SymbolFilterType.GAINERS,
        sentiment = MarketSentiment(
            overallScore = 50,
            sentimentLabel = "Neutral",
            positivePercent = 33,
            neutralPercent = 34,
            negativePercent = 33
        ),
        sectors = emptyList(),
        lastUpdated = "12:00"
    )

    @Test
    fun `Success default liveQuotes is emptyMap`() {
        val state = makeSuccess()
        assertTrue("liveQuotes should default to emptyMap", state.liveQuotes.isEmpty())
    }

    @Test
    fun `Success default wsConnectionState is Idle`() {
        val state = makeSuccess()
        assertEquals(
            "wsConnectionState should default to Idle",
            WsConnectionState.Idle,
            state.wsConnectionState
        )
    }
}
