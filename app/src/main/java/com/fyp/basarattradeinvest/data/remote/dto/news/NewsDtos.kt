package com.fyp.basarattradeinvest.data.remote.dto.news

import com.google.gson.annotations.SerializedName

/**
 * News and Events DTOs matching backend OpenAPI schemas.
 * Reference: http://16.16.26.247:8000/api/v1/openapi.json
 */

data class NewsListResponseDto(
    @SerializedName("items")
    val items: List<NewsArticleResponseDto>? = null,

    @SerializedName("next_cursor")
    val nextCursor: String? = null,

    @SerializedName("has_more")
    val hasMore: Boolean? = null,

    @SerializedName("row")
    val row: String? = null,

    @SerializedName("last_updated_at")
    val lastUpdatedAt: String? = null,

    @SerializedName("empty_reason")
    val emptyReason: String? = null,

    @SerializedName("total")
    val total: Int? = null
)

data class NewsArticleResponseDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("external_url")
    val externalUrl: String? = null,

    @SerializedName("source")
    val source: SourceInfoDto? = null,

    @SerializedName("is_official")
    val isOfficial: Boolean? = null,

    @SerializedName("summary")
    val summary: String? = null,

    @SerializedName("symbols")
    val symbols: List<SymbolInfoDto>? = null,

    @SerializedName("event_type")
    val eventType: String? = null,

    @SerializedName("sentiment")
    val sentiment: String? = null, // "bullish" | "bearish" | "neutral"

    @SerializedName("impact_score")
    val impactScore: Double? = null,

    @SerializedName("published_at")
    val publishedAt: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

data class SourceInfoDto(
    @SerializedName("key")
    val key: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("type")
    val type: String? = null
)

data class SymbolInfoDto(
    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("name")
    val name: String? = null
)

data class NewsRefreshResponseDto(
    @SerializedName("status")
    val status: String,

    @SerializedName("last_updated")
    val lastUpdated: String? = null,

    @SerializedName("refresh_available")
    val refreshAvailable: Boolean? = null,

    @SerializedName("next_refresh_at")
    val nextRefreshAt: String? = null,

    @SerializedName("articles_inserted")
    val articlesInserted: Int? = null,

    @SerializedName("market_status")
    val marketStatus: Any? = null,

    @SerializedName("retry_after_seconds")
    val retryAfterSeconds: Int? = null
)

data class NewsRefreshStatusResponseDto(
    @SerializedName("state")
    val state: String, // "idle" | "running" | "completed" | "failed"

    @SerializedName("last_success_at")
    val lastSuccessAt: String? = null,

    @SerializedName("new_articles")
    val newArticles: Int? = null
)

data class MarketStatusResponseDto(
    @SerializedName("current_time_pkt")
    val currentTimePkt: String? = null,

    @SerializedName("market_window")
    val marketWindow: String? = null,

    @SerializedName("is_open")
    val isOpen: Boolean? = null,

    @SerializedName("next_window")
    val nextWindow: String? = null,

    @SerializedName("ingestion_status")
    val ingestionStatus: String? = null
)

data class SourcesResponseDto(
    @SerializedName("sources")
    val sources: List<SourceHealthResponseDto>? = null
)

data class SourceHealthResponseDto(
    @SerializedName("key")
    val key: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("last_success_at")
    val lastSuccessAt: String? = null,

    @SerializedName("last_error")
    val lastError: String? = null,

    @SerializedName("consecutive_failures")
    val consecutiveFailures: Int = 0,

    @SerializedName("healthy")
    val healthy: Boolean = true
)

data class EventsCalendarResponseDto(
    @SerializedName("items")
    val items: List<EventResponseDto>? = null,

    @SerializedName("total")
    val total: Int? = null
)

data class EventResponseDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("event_type")
    val eventType: String,

    @SerializedName("symbol")
    val symbol: String? = null,

    @SerializedName("company_name")
    val companyName: String? = null,

    @SerializedName("event_date")
    val eventDate: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("source")
    val source: String? = null,

    @SerializedName("source_url")
    val sourceUrl: String? = null
)
