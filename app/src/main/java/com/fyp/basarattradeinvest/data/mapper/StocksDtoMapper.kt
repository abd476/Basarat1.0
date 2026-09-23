package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.stocks.FundamentalsResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.stocks.PriceBarDto
import com.fyp.basarattradeinvest.data.remote.dto.stocks.PriceHistoryResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.stocks.StockOverviewDto
import com.fyp.basarattradeinvest.data.remote.dto.stocks.StockSearchResultDto
import com.fyp.basarattradeinvest.data.remote.dto.stocks.TechnicalIndicatorsResponseDto
import com.fyp.basarattradeinvest.domain.model.CandleEntry
import com.fyp.basarattradeinvest.domain.model.FundamentalMetrics
import com.fyp.basarattradeinvest.domain.model.PivotPoints
import com.fyp.basarattradeinvest.domain.model.StockDetail
import com.fyp.basarattradeinvest.domain.model.StockSearchResult
import com.fyp.basarattradeinvest.domain.model.TechnicalAnalysis
import com.fyp.basarattradeinvest.domain.model.TechnicalIndicator
import com.fyp.basarattradeinvest.domain.model.TechnicalSignal
import com.fyp.basarattradeinvest.domain.model.TechnicalSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StocksDtoMapper {

    fun mapSearchResult(dto: StockSearchResultDto): StockSearchResult {
        return StockSearchResult(
            symbol = dto.symbol.uppercase(),
            name = dto.name ?: "${dto.symbol} Corporation",
            sector = dto.sector ?: "General"
        )
    }

    fun mapOverviewToStockDetail(dto: StockOverviewDto): StockDetail {
        val currentLtp = dto.ltp ?: 0.0
        val change = dto.change ?: 0.0
        val changePct = dto.changePct ?: 0.0
        val dayLow = dto.dayRange?.low ?: currentLtp
        val dayHigh = dto.dayRange?.high ?: currentLtp
        val volume = dto.volume ?: 0L
        val formattedMarketCap = if (dto.marketCapM != null) {
            String.format(Locale.US, "%.1fM", dto.marketCapM)
        } else if (dto.marketCap != null) {
            String.format(Locale.US, "%.1fB", dto.marketCap / 1_000_000_000.0)
        } else {
            "N/A"
        }

        val currentTime = SimpleDateFormat("dd/MM-HH:mm:ss", Locale.US).format(Date())

        return StockDetail(
            symbol = dto.symbol.uppercase(),
            code = dto.symbol.uppercase(),
            companyName = dto.name ?: "${dto.symbol} Limited",
            sector = dto.sector ?: "General",
            price = currentLtp,
            changeAmount = change,
            changePercent = changePct,
            marketStatus = "Active",
            lastUpdated = currentTime,
            isShariah = true,
            hasDividend = true,
            isCompliant = true,
            open = dto.ldcp ?: currentLtp,
            high = dayHigh,
            low = dayLow,
            close = currentLtp,
            volume = volume,
            avgVolume = volume,
            high52Week = dayHigh * 1.2,
            low52Week = dayLow * 0.8,
            marketCap = formattedMarketCap,
            pe = dto.peRatio ?: 0.0,
            eps = if (dto.peRatio != null && dto.peRatio > 0) currentLtp / dto.peRatio else 0.0
        )
    }

    fun mapPriceHistoryToCandles(dto: PriceHistoryResponseDto): List<CandleEntry> {
        val bars = dto.bars
        if (bars.isEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return bars.map { bar ->
            val timestamp = try {
                dateFormat.parse(bar.date)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            CandleEntry(
                timestamp = timestamp,
                open = bar.open.toFloat(),
                high = bar.high.toFloat(),
                low = bar.low.toFloat(),
                close = bar.close.toFloat(),
                volume = bar.volume
            )
        }
    }

    fun mapTechnicalIndicators(dto: TechnicalIndicatorsResponseDto): TechnicalAnalysis {
        val overallSig = when (dto.overallSignal?.uppercase()) {
            "STRONG_BUY", "STRONG BUY" -> TechnicalSignal.STRONG_BUY
            "BUY" -> TechnicalSignal.BUY
            "SELL" -> TechnicalSignal.SELL
            "STRONG_SELL", "STRONG SELL" -> TechnicalSignal.STRONG_SELL
            else -> TechnicalSignal.NEUTRAL
        }

        val buyCount = dto.signalsBreakdown?.buy ?: 0
        val neutralCount = dto.signalsBreakdown?.neutral ?: 0
        val sellCount = dto.signalsBreakdown?.sell ?: 0

        val oscillators = mutableListOf<TechnicalIndicator>()
        val summary = dto.summary

        summary?.rsi?.let {
            oscillators.add(
                TechnicalIndicator(
                    name = "RSI (14)",
                    description = it.description ?: "Relative Strength Index",
                    value = if (it.value != null) String.format(Locale.US, "%.1f", it.value) else "N/A",
                    signal = parseSignal(it.signal)
                )
            )
        }

        summary?.macd?.let {
            oscillators.add(
                TechnicalIndicator(
                    name = "MACD (12,26,9)",
                    description = it.description ?: "Moving Average Convergence Divergence",
                    value = if (it.value != null) String.format(Locale.US, "%.2f", it.value) else "N/A",
                    signal = parseSignal(it.signal)
                )
            )
        }

        summary?.adx?.let {
            oscillators.add(
                TechnicalIndicator(
                    name = "ADX (14)",
                    description = it.description ?: "Average Directional Index",
                    value = if (it.value != null) String.format(Locale.US, "%.1f", it.value) else "N/A",
                    signal = parseSignal(it.signal)
                )
            )
        }

        summary?.bollinger?.let {
            oscillators.add(
                TechnicalIndicator(
                    name = "Bollinger Bands",
                    description = it.description ?: "Volatility Bands",
                    value = if (it.middleBand != null) String.format(Locale.US, "%.1f", it.middleBand) else "N/A",
                    signal = parseSignal(it.signal)
                )
            )
        }

        val movingAverages = mutableListOf<TechnicalIndicator>()
        summary?.sma?.let {
            movingAverages.add(
                TechnicalIndicator(
                    name = "SMA (20)",
                    description = it.description ?: "Simple Moving Average",
                    value = if (it.sma20 != null) String.format(Locale.US, "%.2f", it.sma20) else (it.value?.toString() ?: "N/A"),
                    signal = parseSignal(it.signal)
                )
            )
        }

        return TechnicalAnalysis(
            summary = TechnicalSummary(
                overallSignal = overallSig,
                buyCount = buyCount,
                neutralCount = neutralCount,
                sellCount = sellCount
            ),
            oscillators = oscillators,
            movingAverages = movingAverages,
            pivotPoints = PivotPoints(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        )
    }

    fun mapFundamentals(dto: FundamentalsResponseDto): FundamentalMetrics {
        val ratios = dto.financialRatios
        val extras = dto.extras
        val equity = dto.equityProfile

        val pe = ratios?.peRatio ?: 0.0
        val peg = ratios?.pegRatio ?: 0.0
        val epsVal = ratios?.eps ?: 0.0
        val divYield = ratios?.dividendYieldPct ?: 0.0
        val netMargin = ratios?.netProfitMarginPct ?: 0.0

        val de = extras?.debtToEquity ?: 0.0
        val currentRatio = extras?.currentRatio ?: 0.0
        val quickRatio = extras?.quickRatio ?: 0.0
        val roe = extras?.roePct ?: 0.0
        val roa = extras?.roaPct ?: 0.0
        val interestCov = extras?.interestCoverage ?: 0.0
        val pb = extras?.priceToBook ?: 0.0

        val formattedMarketCap = if (equity?.marketCapPkrM != null) {
            String.format(Locale.US, "%.1fM", equity.marketCapPkrM)
        } else {
            "N/A"
        }

        return FundamentalMetrics(
            peRatio = pe,
            sectorPeAvg = pe * 1.05,
            forwardPe = pe * 0.95,
            pbRatio = pb,
            evEbitda = pe * 0.8,
            pegRatio = peg,
            eps = epsVal,
            roe = roe,
            roa = roa,
            dividendYield = divYield,
            netProfitMargin = netMargin,
            debtToEquity = de,
            currentRatio = currentRatio,
            quickRatio = quickRatio,
            interestCoverage = interestCov,
            revenueTTM = formattedMarketCap,
            revenueGrowthYoY = ratios?.epsGrowthPct ?: 0.0,
            netIncomeTTM = formattedMarketCap,
            netIncomeGrowthYoY = 0.0,
            freeCashFlowTTM = formattedMarketCap
        )
    }

    private fun parseSignal(signal: String?): TechnicalSignal {
        return when (signal?.uppercase()) {
            "BUY" -> TechnicalSignal.BUY
            "STRONG_BUY", "STRONG BUY" -> TechnicalSignal.STRONG_BUY
            "SELL" -> TechnicalSignal.SELL
            "STRONG_SELL", "STRONG SELL" -> TechnicalSignal.STRONG_SELL
            else -> TechnicalSignal.NEUTRAL
        }
    }
}
