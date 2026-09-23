package com.fyp.basarattradeinvest.data.remote.dto.stocks

import com.google.gson.annotations.SerializedName

data class FundamentalsResponseDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("company_profile") val companyProfile: CompanyProfileDto? = null,
    @SerializedName("equity_profile") val equityProfile: EquityProfileDto? = null,
    @SerializedName("financial_ratios") val financialRatios: FinancialRatiosDto? = null,
    @SerializedName("trading_limits") val tradingLimits: TradingLimitsDto? = null,
    @SerializedName("dividends") val dividends: List<DividendHistoryItemDto>? = null,
    @SerializedName("announcements") val announcements: List<AnnouncementItemDto>? = null,
    @SerializedName("metrics") val metrics: List<FundamentalMetricDto>? = null,
    @SerializedName("extras") val extras: FundamentalsExtrasDto? = null
)

data class CompanyProfileDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("sector") val sector: String? = null,
    @SerializedName("business_description") val businessDescription: String? = null,
    @SerializedName("ceo") val ceo: String? = null,
    @SerializedName("chairperson") val chairperson: String? = null,
    @SerializedName("company_secretary") val companySecretary: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("address") val address: String? = null
)

data class EquityProfileDto(
    @SerializedName("market_cap_pkr") val marketCapPkr: Double? = null,
    @SerializedName("market_cap_pkr_m") val marketCapPkrM: Double? = null,
    @SerializedName("total_shares") val totalShares: Long? = null,
    @SerializedName("free_float_shares") val freeFloatShares: Long? = null,
    @SerializedName("free_float_pct") val freeFloatPct: Double? = null
)

data class FinancialRatiosDto(
    @SerializedName("pe_ratio") val peRatio: Double? = null,
    @SerializedName("peg_ratio") val pegRatio: Double? = null,
    @SerializedName("eps") val eps: Double? = null,
    @SerializedName("eps_growth_pct") val epsGrowthPct: Double? = null,
    @SerializedName("net_profit_margin_pct") val netProfitMarginPct: Double? = null,
    @SerializedName("gross_profit_margin_pct") val grossProfitMarginPct: Double? = null,
    @SerializedName("dividend_yield_pct") val dividendYieldPct: Double? = null
)

data class TradingLimitsDto(
    @SerializedName("year_high") val yearHigh: Double? = null,
    @SerializedName("year_low") val yearLow: Double? = null,
    @SerializedName("circuit_breaker_lower") val circuitBreakerLower: Double? = null,
    @SerializedName("circuit_breaker_upper") val circuitBreakerUpper: Double? = null,
    @SerializedName("year_change_pct") val yearChangePct: Double? = null,
    @SerializedName("ytd_change_pct") val ytdChangePct: Double? = null
)

data class DividendHistoryItemDto(
    @SerializedName("announcement_date") val announcementDate: String? = null,
    @SerializedName("financial_year") val financialYear: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("amount_per_share") val amountPerShare: Double? = null,
    @SerializedName("dividend_pct") val dividendPct: Double? = null
)

data class AnnouncementItemDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("description") val description: String? = null
)

data class FundamentalMetricDto(
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: String,
    @SerializedName("category") val category: String? = null,
    @SerializedName("trend") val trend: String? = null
)

data class FundamentalsExtrasDto(
    @SerializedName("book_value_per_share") val bookValuePerShare: Double? = null,
    @SerializedName("price_to_book") val priceToBook: Double? = null,
    @SerializedName("debt_to_equity") val debtToEquity: Double? = null,
    @SerializedName("current_ratio") val currentRatio: Double? = null,
    @SerializedName("quick_ratio") val quickRatio: Double? = null,
    @SerializedName("roe_pct") val roePct: Double? = null,
    @SerializedName("roa_pct") val roaPct: Double? = null,
    @SerializedName("interest_coverage") val interestCoverage: Double? = null
)
