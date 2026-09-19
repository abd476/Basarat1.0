package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.repository.NewsRepository
import kotlinx.coroutines.delay

class NewsRepositoryImpl : NewsRepository {

    private val mockNewsArticles = listOf(
        NewsArticle(
            id = "news_1",
            symbol = "LUCK",
            hasDividend = true,
            source = "Business Recorder",
            timestamp = "25m ago",
            sentimentScore = 0.82,
            sentimentLabel = "Bullish",
            headline = "Lucky Cement reports 24% profit surge on robust export volumes and lower energy costs",
            snippet = "The company posted unconsolidated net earnings of PKR 27.6B for the fiscal year, driven by higher operational efficiency and captive power usage.",
            category = "Cement"
        ),
        NewsArticle(
            id = "news_2",
            symbol = "MEBL",
            hasDividend = true,
            source = "Profit Pakistan Today",
            timestamp = "45m ago",
            sentimentScore = 0.76,
            sentimentLabel = "Bullish",
            headline = "Meezan Bank records historic quarterly profit; announces PKR 7.00 interim cash dividend",
            snippet = "Pakistan's premier Islamic bank expanded its branch footprint to 1,050 locations while preserving an industry-leading non-performing financing ratio below 1.5%.",
            category = "Banks"
        ),
        NewsArticle(
            id = "news_3",
            symbol = "SYS",
            hasDividend = false,
            source = "Dawn Business",
            timestamp = "1h ago",
            sentimentScore = 0.65,
            sentimentLabel = "Bullish",
            headline = "Systems Limited secures \$45M digital transformation contract with GCC financial group",
            snippet = "The multi-year project entails core banking cloud migration and generative AI integration across regional operations in Saudi Arabia and the UAE.",
            category = "Technology"
        ),
        NewsArticle(
            id = "news_4",
            symbol = "OGDC",
            hasDividend = true,
            source = "The News International",
            timestamp = "2h ago",
            sentimentScore = 0.54,
            sentimentLabel = "Bullish",
            headline = "OGDCL discovers new gas and condensate reserves in Kohat Basin; testing yields 14.5 MMSCFD",
            snippet = "The exploratory well Baratai-01 struck commercial hydrocarbons, substantially augmenting indigenous energy supply for the national grid.",
            category = "KSE-100"
        ),
        NewsArticle(
            id = "news_5",
            symbol = "HUBC",
            hasDividend = true,
            source = "Express Tribune",
            timestamp = "3h ago",
            sentimentScore = -0.32,
            sentimentLabel = "Bearish",
            headline = "Hub Power Company faces temporary tariff renegotiation talks under government power sector review",
            snippet = "Management stated that sovereign contractual guarantees remain intact while collaborative consultations with CPPA-G and Power Division progress smoothly.",
            category = "Earnings"
        ),
        NewsArticle(
            id = "news_6",
            symbol = "ENGRO",
            hasDividend = true,
            source = "Business Recorder",
            timestamp = "4h ago",
            sentimentScore = 0.15,
            sentimentLabel = "Neutral",
            headline = "Engro Corporation restructuring plan approved by High Court; demerger set for next quarter",
            snippet = "The corporate simplification is anticipated to unlock shareholder value by separating telecom infrastructure operations from fertilizer and petrochemical assets.",
            category = "Dividends"
        ),
        NewsArticle(
            id = "news_7",
            symbol = "MCB",
            hasDividend = true,
            source = "Profit Pakistan Today",
            timestamp = "5h ago",
            sentimentScore = 0.68,
            sentimentLabel = "Bullish",
            headline = "MCB Bank capital adequacy ratio reaches 20.4%, providing robust buffer for private sector lending",
            snippet = "High policy rate spread environments continued to buoy net interest margins, allowing the board to declare consistent payout rounds.",
            category = "Banks"
        ),
        NewsArticle(
            id = "news_8",
            symbol = "TRG",
            hasDividend = false,
            source = "Dawn Business",
            timestamp = "6h ago",
            sentimentScore = -0.48,
            sentimentLabel = "Bearish",
            headline = "TRG Pakistan portfolio valuation impacted by global tech multiples re-rating in Q3",
            snippet = "The holding entity experienced marked-to-market adjustments on minority offshore software investments amidst elevated global interest rate benchmarks.",
            category = "Technology"
        )
    )

    private val mockCorporateEvents = listOf(
        CorporateEvent(
            id = "event_1",
            eventType = "CASH DIVIDEND",
            symbol = "MEBL",
            companyName = "Meezan Bank Limited",
            eventDate = "Oct 04, 2026",
            description = "Board of Directors announces interim cash dividend of PKR 7.00 per share (70%) for the 3rd quarter ended September 30, 2026.",
            bookClosure = "Book Closure: Sep 28 - Oct 04, 2026",
            sourceNotice = "PSX Official Notice",
            category = "Dividends"
        ),
        CorporateEvent(
            id = "event_2",
            eventType = "BOARD MEETING",
            symbol = "LUCK",
            companyName = "Lucky Cement Limited",
            eventDate = "Oct 08, 2026",
            description = "Meeting of the Board of Directors scheduled at head office Karachi to consider financial statements and interim corporate actions.",
            bookClosure = "Closed Period: Oct 01 - Oct 08, 2026",
            sourceNotice = "PSX Official Notice",
            category = "Earnings"
        ),
        CorporateEvent(
            id = "event_3",
            eventType = "ANNUAL GENERAL MEETING",
            symbol = "HUBC",
            companyName = "The Hub Power Company Limited",
            eventDate = "Oct 12, 2026",
            description = "35th Annual General Meeting of shareholders to approve audited financial statements and confirm final cash dividend of PKR 4.50 per share.",
            bookClosure = "Book Closure: Oct 05 - Oct 12, 2026",
            sourceNotice = "PSX Official Notice",
            category = "Dividends"
        ),
        CorporateEvent(
            id = "event_4",
            eventType = "FINANCIAL RESULTS",
            symbol = "SYS",
            companyName = "Systems Limited",
            eventDate = "Oct 15, 2026",
            description = "Board of Directors meeting to review unconsolidated and consolidated financial results for the 9-month period ended September 30, 2026.",
            bookClosure = "Closed Period: Oct 08 - Oct 15, 2026",
            sourceNotice = "PSX Official Notice",
            category = "Technology"
        ),
        CorporateEvent(
            id = "event_5",
            eventType = "CASH DIVIDEND",
            symbol = "FFC",
            companyName = "Fauji Fertilizer Company Limited",
            eventDate = "Oct 18, 2026",
            description = "Declaration of 3rd interim cash dividend of PKR 5.25 per share (52.5%) for the period ended September 30, 2026.",
            bookClosure = "Book Closure: Oct 11 - Oct 18, 2026",
            sourceNotice = "PSX Official Notice",
            category = "Dividends"
        ),
        CorporateEvent(
            id = "event_6",
            eventType = "CORPORATE BRIEFING",
            symbol = "OGDC",
            companyName = "Oil & Gas Development Company Ltd",
            eventDate = "Oct 22, 2026",
            description = "Analyst and investor corporate briefing session to present strategic operational updates on circular debt resolution and offshore exploration.",
            bookClosure = "Virtual & Physical: PSX Auditorium",
            sourceNotice = "PSX Official Notice",
            category = "KSE-100"
        )
    )

    override suspend fun getNewsArticles(category: String, query: String): Result<List<NewsArticle>> {
        delay(120) // Realistic light network latency
        val normalizedCategory = category.trim()
        val normalizedQuery = query.trim().lowercase()

        val filtered = mockNewsArticles.filter { article ->
            val matchesCategory = when (normalizedCategory) {
                "All", "" -> true
                "KSE-100" -> true // All top stocks are part of KSE-100
                "Earnings" -> article.category.equals("Earnings", ignoreCase = true) || article.headline.contains("profit", ignoreCase = true) || article.headline.contains("earnings", ignoreCase = true)
                "Dividends" -> article.hasDividend || article.category.equals("Dividends", ignoreCase = true) || article.headline.contains("dividend", ignoreCase = true)
                "Banks" -> article.category.equals("Banks", ignoreCase = true)
                "Cement" -> article.category.equals("Cement", ignoreCase = true)
                "Technology" -> article.category.equals("Technology", ignoreCase = true)
                else -> article.category.equals(normalizedCategory, ignoreCase = true)
            }

            val matchesQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                article.symbol.lowercase().contains(normalizedQuery) ||
                        article.headline.lowercase().contains(normalizedQuery) ||
                        article.snippet.lowercase().contains(normalizedQuery) ||
                        article.source.lowercase().contains(normalizedQuery)
            }

            matchesCategory && matchesQuery
        }

        return Result.success(filtered)
    }

    override suspend fun getCorporateEvents(category: String, query: String): Result<List<CorporateEvent>> {
        delay(120) // Realistic light network latency
        val normalizedCategory = category.trim()
        val normalizedQuery = query.trim().lowercase()

        val filtered = mockCorporateEvents.filter { event ->
            val matchesCategory = when (normalizedCategory) {
                "All", "" -> true
                "KSE-100" -> true
                "Earnings" -> event.category.equals("Earnings", ignoreCase = true) || event.eventType.contains("RESULTS", ignoreCase = true) || event.eventType.contains("BOARD MEETING", ignoreCase = true)
                "Dividends" -> event.eventType.contains("DIVIDEND", ignoreCase = true) || event.category.equals("Dividends", ignoreCase = true)
                "Banks" -> event.companyName.contains("Bank", ignoreCase = true) || event.symbol == "MEBL" || event.symbol == "MCB"
                "Cement" -> event.companyName.contains("Cement", ignoreCase = true) || event.symbol == "LUCK"
                "Technology" -> event.category.equals("Technology", ignoreCase = true) || event.symbol == "SYS" || event.symbol == "TRG"
                else -> event.category.equals(normalizedCategory, ignoreCase = true)
            }

            val matchesQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                event.symbol.lowercase().contains(normalizedQuery) ||
                        event.companyName.lowercase().contains(normalizedQuery) ||
                        event.eventType.lowercase().contains(normalizedQuery) ||
                        event.description.lowercase().contains(normalizedQuery)
            }

            matchesCategory && matchesQuery
        }

        return Result.success(filtered)
    }
}
