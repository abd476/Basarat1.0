package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.sentiment.MarketSentimentResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.sentiment.SentimentHistoryPointDto
import com.fyp.basarattradeinvest.data.remote.dto.sentiment.SentimentHistoryResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.sentiment.SentimentNewsItemDto
import com.fyp.basarattradeinvest.data.remote.dto.sentiment.SentimentNewsResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.sentiment.SentimentResponseDto
import com.fyp.basarattradeinvest.domain.model.sentiment.MarketSentimentOverview
import com.fyp.basarattradeinvest.domain.model.sentiment.SentimentHistory
import com.fyp.basarattradeinvest.domain.model.sentiment.SentimentHistoryPoint
import com.fyp.basarattradeinvest.domain.model.sentiment.SentimentNewsItem
import com.fyp.basarattradeinvest.domain.model.sentiment.SentimentNewsPage
import com.fyp.basarattradeinvest.domain.model.sentiment.StockSentiment

/**
 * SentimentDtoMapper
 *
 * Maps Sentiment API DTOs to domain models.
 * Field-by-field mapping verified against OpenAPI schema.
 * All nullable fields preserved as nullable in domain models.
 */
object SentimentDtoMapper {

    // ─── Stock Sentiment ───────────────────────────────────────────────────────

    fun mapStockSentiment(dto: SentimentResponseDto): StockSentiment {
        val newsCount = dto.sourceBreakdown?.get("news") ?: 0
        val communityCount = dto.sourceBreakdown?.get("community") ?: 0
        return StockSentiment(
            symbol = dto.symbol,
            score = dto.score,
            label = dto.label,
            articleCount = dto.articleCount,
            trend = dto.trend,
            newsSourceCount = newsCount,
            communitySourceCount = communityCount
        )
    }

    // ─── Sentiment History ─────────────────────────────────────────────────────

    fun mapSentimentHistory(dto: SentimentHistoryResponseDto): SentimentHistory {
        return SentimentHistory(
            symbol = dto.symbol,
            period = dto.period,
            data = dto.data.map { mapHistoryPoint(it) }
        )
    }

    private fun mapHistoryPoint(dto: SentimentHistoryPointDto): SentimentHistoryPoint {
        return SentimentHistoryPoint(
            date = dto.date,
            score = dto.score,
            label = dto.label,
            articleCount = dto.articleCount,
            positiveRatio = dto.positiveRatio,
            neutralRatio = dto.neutralRatio,
            negativeRatio = dto.negativeRatio,
            trend = dto.trend
        )
    }

    // ─── Sentiment News ────────────────────────────────────────────────────────

    fun mapSentimentNewsPage(dto: SentimentNewsResponseDto): SentimentNewsPage {
        return SentimentNewsPage(
            symbol = dto.symbol,
            items = dto.items.map { mapNewsItem(it) },
            total = dto.total,
            page = dto.page,
            limit = dto.limit
        )
    }

    private fun mapNewsItem(dto: SentimentNewsItemDto): SentimentNewsItem {
        return SentimentNewsItem(
            id = dto.id,
            title = dto.title,
            source = dto.source,
            publishedAt = dto.publishedAt,
            url = dto.url,
            sentiment = dto.sentiment,
            sentimentScore = dto.sentimentScore,
            sentimentModel = dto.sentimentModel
        )
    }

    // ─── Market Sentiment Overview ─────────────────────────────────────────────

    fun mapMarketSentimentOverview(dto: MarketSentimentResponseDto): MarketSentimentOverview {
        val dist = dto.scoreDistribution
        return MarketSentimentOverview(
            marketMood = dto.marketMood,
            overallScore = dto.overallScore,
            articleCount = dto.articleCount,
            communityPostCount = dto.communityPostCount,
            advancing = dto.advancing,
            declining = dto.declining,
            unchanged = dto.unchanged,
            advanceDeclineRatio = dto.advanceDeclineRatio,
            newsSentimentAvg = dto.newsSentimentAvg,
            communitySentimentAvg = dto.communitySentimentAvg,
            scoreDistributionPositive = dist?.get("positive") ?: 0,
            scoreDistributionNeutral = dist?.get("neutral") ?: 0,
            scoreDistributionNegative = dist?.get("negative") ?: 0
        )
    }
}
