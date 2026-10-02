# Requirements Document

## Introduction

This feature integrates IPO (Initial Public Offering) and ETF (Exchange Traded Fund) data from the BASARAT backend into the Android application, and wires up the existing chip navigation on the Market Dashboard. Users gain dedicated browsing, detail, calendar, and performance views for both asset classes, all surfaced from the live `/api/v1/ipos` and `/api/v1/etfs` endpoint families. Navigation originates from the two currently ID-less chips (`IPO` and `ETF`) on `fragment_market_dashboard.xml`.

---

## Glossary

- **MarketDashboard**: The `MarketDashboardFragment` that hosts the top-level market overview and chip navigation row.
- **IpoFragment**: The tabbed host Fragment for IPO content, containing Directory, Calendar, and Performance tabs.
- **IpoDirectoryFragment**: The tab displaying a scrollable list of all IPO items.
- **IpoCalendarFragment**: The tab displaying upcoming IPO milestone events.
- **IpoPerformanceFragment**: The tab displaying listed IPO performance statistics.
- **IpoDetailFragment**: The detail screen for a single IPO item.
- **EtfDirectoryFragment**: The Fragment displaying a scrollable list of all ETF items.
- **EtfDetailFragment**: The detail screen for a single ETF item, sectioned into Overview, History, and Performance tabs.
- **IpoApiService**: The Retrofit interface for all `/api/v1/ipos` endpoints.
- **EtfApiService**: The Retrofit interface for all `/api/v1/etfs` endpoints.
- **IpoRepository**: The repository that exposes IPO domain data to ViewModels.
- **EtfRepository**: The repository that exposes ETF domain data to ViewModels.
- **IpoViewModel**: The ViewModel that manages UI state for IpoFragment and its tabs.
- **EtfDirectoryViewModel**: The ViewModel that manages UI state for EtfDirectoryFragment.
- **EtfDetailViewModel**: The ViewModel that manages UI state for EtfDetailFragment.
- **UiState**: The sealed interface with states `Loading`, `Success`, `Empty`, and `Error` used project-wide.
- **NetworkClient**: The singleton object in `data/remote/NetworkClient.kt` that registers all Retrofit API service instances as `lazy` properties.
- **ListAdapter**: Android's `RecyclerView.Adapter` subclass using `DiffUtil.ItemCallback` for efficient list diffing, as used in existing adapters such as `MarketStockAdapter`.
- **IpoItem**: The domain model representing a single IPO, mapped from the backend `IpoItem` DTO.
- **EtfItem**: The domain model representing a single ETF, mapped from the backend `EtfItem` DTO.
- **IpoCalendarEvent**: The domain model representing a single upcoming IPO milestone, mapped from the `/api/v1/ipos/calendar` response.
- **IpoPerformanceItem**: The domain model representing a single listed IPO performance row, mapped from the `/api/v1/ipos/performance` response.
- **EtfHistoryEntry**: The domain model representing a single OHLCV candle, mapped from the `/api/v1/etfs/{symbol}/history` response.
- **EtfPerformance**: The domain model representing ETF return and benchmark comparison data from `/api/v1/etfs/{symbol}/performance`.

---

## Requirements

### Requirement 1 — Chip Navigation Wiring

**User Story:** As a market user, I want to tap the IPO and ETF chips on the Market Dashboard so that I can navigate directly to those feature areas.

#### Acceptance Criteria

1. THE `fragment_market_dashboard.xml` SHALL assign `android:id="@+id/chipIpo"` to the existing IPO chip view.
2. THE `fragment_market_dashboard.xml` SHALL assign `android:id="@+id/chipEtf"` to the existing ETF chip view.
3. WHEN the user taps `chipIpo`, THE `MarketDashboard` SHALL navigate to `IpoFragment` using `findNavController().navigate(R.id.ipoFragment)`.
4. WHEN the user taps `chipEtf`, THE `MarketDashboard` SHALL navigate to `EtfDirectoryFragment` using `findNavController().navigate(R.id.etfDirectoryFragment)`.
5. THE `nav_graph.xml` SHALL declare `ipoFragment` and `etfDirectoryFragment` as reachable destinations with appropriate actions from the market dashboard destination.

---

### Requirement 2 — IPO & ETF API Service and Network Registration

**User Story:** As a developer, I want the IPO and ETF Retrofit service interfaces wired into NetworkClient so that repositories can consume live backend data without creating duplicate HTTP clients.

#### Acceptance Criteria

1. THE `IpoApiService` SHALL declare a `@GET("api/v1/ipos")` method returning `Response<IposResponseDto>`.
2. THE `IpoApiService` SHALL declare a `@GET("api/v1/ipos/{symbol}")` method accepting a `@Path("symbol")` parameter and returning `Response<IpoItemDto>`.
3. THE `IpoApiService` SHALL declare a `@GET("api/v1/ipos/calendar")` method returning `Response<IpoCalendarResponseDto>`.
4. THE `IpoApiService` SHALL declare a `@GET("api/v1/ipos/performance")` method returning `Response<IpoPerformanceResponseDto>`.
5. THE `EtfApiService` SHALL declare a `@GET("api/v1/etfs")` method returning `Response<EtfsResponseDto>`.
6. THE `EtfApiService` SHALL declare a `@GET("api/v1/etfs/{symbol}")` method accepting a `@Path("symbol")` parameter and returning `Response<EtfItemDto>`.
7. THE `EtfApiService` SHALL declare a `@GET("api/v1/etfs/{symbol}/history")` method accepting `@Path("symbol")` and an optional `@Query("timeframe")` parameter and returning `Response<EtfHistoryResponseDto>`.
8. THE `EtfApiService` SHALL declare a `@GET("api/v1/etfs/{symbol}/performance")` method accepting `@Path("symbol")` and returning `Response<EtfPerformanceDto>`.
9. THE `NetworkClient` SHALL expose `ipoApiService: IpoApiService` as a `lazy` property created from the existing `marketRetrofit` instance.
10. THE `NetworkClient` SHALL expose `etfApiService: EtfApiService` as a `lazy` property created from the existing `marketRetrofit` instance.

---

### Requirement 3 — IPO & ETF DTOs

**User Story:** As a developer, I want accurate DTO data classes that mirror the verified backend response shapes so that Gson deserialization produces correct domain values.

#### Acceptance Criteria

1. THE `IpoItemDto` SHALL contain fields matching the verified backend contract: `id`, `symbol`, `company_name`, `sector`, `status`, `issue_size_shares`, `issue_size_pkr`, `floor_price`, `strike_price`, `listing_price`, `current_price`, `book_building_start`, `book_building_end`, `public_subscription_start`, `public_subscription_end`, `listing_date`, `lead_manager`, `is_shariah_compliant`, `prospectus_url`, `description`, `subscription_multiplier`, `listing_gain_pct`, `current_gain_pct`, `created_at`, `updated_at`.
2. THE `IposResponseDto` SHALL contain fields `total`, `upcoming_count`, `active_count`, `listed_count`, and `ipos` (list of `IpoItemDto`).
3. THE `IpoCalendarResponseDto` SHALL contain fields `total_events` and `upcoming_milestones` (list of `IpoCalendarEventDto`).
4. THE `IpoCalendarEventDto` SHALL contain fields `ipo_id`, `symbol`, `company_name`, `event_type`, `event_date`, `status`, and `price_info`.
5. THE `IpoPerformanceResponseDto` SHALL contain fields `total_listed`, `average_listing_day_gain_pct`, `top_performers`, and `recent_listings` (both lists of `IpoPerformanceItemDto`).
6. THE `IpoPerformanceItemDto` SHALL contain fields `symbol`, `company_name`, `sector`, `listing_date`, `offer_price`, `first_day_close`, `current_price`, `first_day_return_pct`, and `total_return_pct`.
7. THE `EtfItemDto` SHALL contain fields `id`, `symbol`, `name`, `fund_manager`, `category`, `benchmark_index`, `is_shariah_compliant`, `expense_ratio`, `inception_date`, `total_assets_pkr`, `description`, `is_active`, `quote`, `current_price`, `change`, `change_percent`, `volume`, `created_at`, `updated_at`.
8. THE `EtfQuoteDto` SHALL contain fields `current_price`, `change`, `change_percent`, `open_price`, `high_price`, `low_price`, `close_price`, `volume`, `bid_price`, `ask_price`, `data_as_of`, `quote_source`.
9. THE `EtfsResponseDto` SHALL contain fields `total`, `shariah_count`, `conventional_count`, and `etfs` (list of `EtfItemDto`).
10. THE `EtfHistoryResponseDto` SHALL contain fields `symbol`, `timeframe`, `count`, and `history` (list of `EtfHistoryEntryDto` with `date`, `open`, `high`, `low`, `close`, `volume`).
11. THE `EtfPerformanceDto` SHALL contain fields `symbol`, `name`, `benchmark_index`, `returns`, `benchmark_returns`, `tracking_difference_1m`, and `volatility_annualized`.

---

### Requirement 4 — Domain Models and Mappers

**User Story:** As a developer, I want clean domain models isolated from DTO structure so that ViewModels operate on stable types independent of API field names.

#### Acceptance Criteria

1. THE `IpoItem` domain model SHALL expose Kotlin-idiomatic property names mapped from `IpoItemDto`, with nullable fields typed as nullable Kotlin types.
2. THE `EtfItem` domain model SHALL expose Kotlin-idiomatic property names mapped from `EtfItemDto`, including a nested `quote` property of type `EtfQuote`.
3. THE `IpoCalendarEvent` domain model SHALL expose `ipoId`, `symbol`, `companyName`, `eventType`, `eventDate`, `status`, and `priceInfo` mapped from `IpoCalendarEventDto`.
4. THE `IpoPerformanceItem` domain model SHALL expose `symbol`, `companyName`, `sector`, `listingDate`, `offerPrice`, `firstDayClose`, `currentPrice`, `firstDayReturnPct`, and `totalReturnPct` mapped from `IpoPerformanceItemDto`.
5. THE `EtfHistoryEntry` domain model SHALL expose `date`, `open`, `high`, `low`, `close`, and `volume` mapped from `EtfHistoryEntryDto`.
6. THE `EtfPerformance` domain model SHALL expose `symbol`, `name`, `benchmarkIndex`, `returns`, `benchmarkReturns`, `trackingDifference1m`, and `volatilityAnnualized`.
7. THE `IpoRepository` SHALL expose a `getIpos()` suspend function returning `List<IpoItem>`.
8. THE `IpoRepository` SHALL expose a `getIpoDetail(symbol: String)` suspend function returning `IpoItem`.
9. THE `IpoRepository` SHALL expose a `getIpoCalendar()` suspend function returning `List<IpoCalendarEvent>`.
10. THE `IpoRepository` SHALL expose a `getIpoPerformance()` suspend function returning an aggregate containing `totalListed`, `averageListingDayGainPct`, `topPerformers`, and `recentListings`.
11. THE `EtfRepository` SHALL expose a `getEtfs()` suspend function returning `List<EtfItem>`.
12. THE `EtfRepository` SHALL expose a `getEtfDetail(symbol: String)` suspend function returning `EtfItem`.
13. THE `EtfRepository` SHALL expose a `getEtfHistory(symbol: String, timeframe: String)` suspend function returning `List<EtfHistoryEntry>`.
14. THE `EtfRepository` SHALL expose a `getEtfPerformance(symbol: String)` suspend function returning `EtfPerformance`.

---

### Requirement 5 — IPO Directory

**User Story:** As a market user, I want to see a list of all IPOs categorized by status so that I can quickly identify UPCOMING, ACTIVE, and LISTED offerings.

#### Acceptance Criteria

1. WHEN `IpoDirectoryFragment` is displayed, THE `IpoViewModel` SHALL fetch data from `IpoRepository.getIpos()` within `viewModelScope`.
2. WHILE loading, THE `IpoDirectoryFragment` SHALL display skeleton placeholder views and suppress the list.
3. WHEN the backend returns a non-empty list, THE `IpoDirectoryFragment` SHALL display each `IpoItem` in a `ListAdapter` using `DiffUtil.ItemCallback` keyed on `symbol`.
4. THE `IpoDirectoryFragment` SHALL display each IPO row with: `company_name`, `symbol`, `sector`, `status` badge, `floor_price`, and `is_shariah_compliant` indicator when the value is `true`.
5. THE status badge SHALL use `R.color.color_positive` for `ACTIVE`, `R.color.color_warning` for `UPCOMING`, and `R.color.color_text_secondary` (or neutral) for `LISTED`.
6. IF the backend returns an empty list, THEN THE `IpoDirectoryFragment` SHALL display an empty state message and hide the RecyclerView.
7. IF the network request fails, THEN THE `IpoDirectoryFragment` SHALL display an error message with a retry button.
8. WHEN the user taps a retry button, THE `IpoViewModel` SHALL re-fetch data from `IpoRepository.getIpos()`.
9. WHEN the user taps an IPO row, THE `IpoDirectoryFragment` SHALL navigate to `IpoDetailFragment` passing `symbol` as a Bundle argument.
10. WHEN the user pulls down to refresh, THE `IpoViewModel` SHALL re-fetch data from `IpoRepository.getIpos()`.

---

### Requirement 6 — IPO Calendar

**User Story:** As a market user, I want to see upcoming IPO milestone dates in a calendar-style list so that I can track book-building and subscription windows.

#### Acceptance Criteria

1. WHEN `IpoCalendarFragment` is displayed, THE `IpoViewModel` SHALL fetch data from `IpoRepository.getIpoCalendar()` within `viewModelScope`.
2. WHILE loading, THE `IpoCalendarFragment` SHALL display skeleton placeholder views.
3. WHEN the backend returns calendar events, THE `IpoCalendarFragment` SHALL display each event row with: `company_name`, `symbol`, `event_type`, `event_date`, and `status`.
4. THE `event_type` SHALL be rendered as a human-readable label: `BOOK_BUILDING_START` → "Book Building Start", `BOOK_BUILDING_END` → "Book Building End", `PUBLIC_SUBSCRIPTION_START` → "Public Subscription Start", `PUBLIC_SUBSCRIPTION_END` → "Public Subscription End", `LISTING_DATE` → "Listing Date".
5. THE `status` SHALL render `COMPLETED` events with a muted/secondary text color and `SCHEDULED` events with the primary text color.
6. IF the backend returns an empty calendar, THEN THE `IpoCalendarFragment` SHALL display an empty state message.
7. IF the network request fails, THEN THE `IpoCalendarFragment` SHALL display an error state with a retry action.

---

### Requirement 7 — IPO Performance

**User Story:** As a market user, I want to see how listed IPOs have performed so that I can evaluate historical IPO returns.

#### Acceptance Criteria

1. WHEN `IpoPerformanceFragment` is displayed, THE `IpoViewModel` SHALL fetch data from `IpoRepository.getIpoPerformance()` within `viewModelScope`.
2. WHILE loading, THE `IpoPerformanceFragment` SHALL display skeleton placeholder views.
3. WHEN the backend returns performance data, THE `IpoPerformanceFragment` SHALL display a summary header showing `total_listed` count and `average_listing_day_gain_pct`.
4. THE `IpoPerformanceFragment` SHALL display a "Top Performers" section listing each `IpoPerformanceItem` with `company_name`, `symbol`, `first_day_return_pct`, and `total_return_pct`.
5. THE `IpoPerformanceFragment` SHALL display a "Recent Listings" section listing each `IpoPerformanceItem` with `company_name`, `symbol`, `listing_date`, `offer_price`, and `first_day_return_pct`.
6. THE return percentage values SHALL be colored using `R.color.color_positive` for positive values and `R.color.color_negative` for negative values.
7. IF the network request fails, THEN THE `IpoPerformanceFragment` SHALL display an error state with a retry action.

---

### Requirement 8 — IPO Detail

**User Story:** As a market user, I want to view the full details of a specific IPO so that I can make informed investment decisions.

#### Acceptance Criteria

1. WHEN `IpoDetailFragment` receives a `symbol` Bundle argument, THE `IpoDetailViewModel` SHALL fetch data from `IpoRepository.getIpoDetail(symbol)` within `viewModelScope`.
2. WHILE loading, THE `IpoDetailFragment` SHALL display skeleton placeholder views for all data fields.
3. WHEN the backend returns the IPO detail, THE `IpoDetailFragment` SHALL display: `company_name`, `symbol`, `sector`, `status` badge, `floor_price`, `strike_price` (if non-null), `listing_price` (if non-null), `current_price` (if non-null), `issue_size_pkr`, `lead_manager`, `is_shariah_compliant`, `description`.
4. WHERE `book_building_start` and `book_building_end` are non-null, THE `IpoDetailFragment` SHALL display the book-building date range.
5. WHERE `public_subscription_start` and `public_subscription_end` are non-null, THE `IpoDetailFragment` SHALL display the public subscription date range.
6. WHERE `listing_date` is non-null, THE `IpoDetailFragment` SHALL display the listing date.
7. WHERE `listing_gain_pct` is non-null, THE `IpoDetailFragment` SHALL display the listing day gain colored by `R.color.color_positive` or `R.color.color_negative`.
8. WHERE `current_gain_pct` is non-null, THE `IpoDetailFragment` SHALL display the current gain colored by `R.color.color_positive` or `R.color.color_negative`.
9. WHERE `prospectus_url` is non-null and non-empty, THE `IpoDetailFragment` SHALL display a "View Prospectus" action that opens the URL in the system browser via an Intent.
10. IF the network request fails, THEN THE `IpoDetailFragment` SHALL display an error message and a retry button.

---

### Requirement 9 — ETF Directory

**User Story:** As a market user, I want to browse all available ETFs so that I can discover funds suitable for my investment strategy.

#### Acceptance Criteria

1. WHEN `EtfDirectoryFragment` is displayed, THE `EtfDirectoryViewModel` SHALL fetch data from `EtfRepository.getEtfs()` within `viewModelScope`.
2. WHILE loading, THE `EtfDirectoryFragment` SHALL display skeleton placeholder views and suppress the list.
3. WHEN the backend returns a non-empty list, THE `EtfDirectoryFragment` SHALL display each `EtfItem` in a `ListAdapter` using `DiffUtil.ItemCallback` keyed on `symbol`.
4. THE `EtfDirectoryFragment` SHALL display each ETF row with: `name`, `symbol`, `fund_manager`, `category`, `current_price`, `change_percent`, and an `is_shariah_compliant` indicator when the value is `true`.
5. THE `change_percent` SHALL be colored using `R.color.color_positive` for positive values and `R.color.color_negative` for negative values.
6. THE `EtfDirectoryFragment` SHALL display a summary header showing total ETF count, Shariah-compliant count, and conventional count from the backend response.
7. IF the backend returns an empty list, THEN THE `EtfDirectoryFragment` SHALL display an empty state message and hide the RecyclerView.
8. IF the network request fails, THEN THE `EtfDirectoryFragment` SHALL display an error state with a retry button.
9. WHEN the user taps a retry button, THE `EtfDirectoryViewModel` SHALL re-fetch data from `EtfRepository.getEtfs()`.
10. WHEN the user taps an ETF row, THE `EtfDirectoryFragment` SHALL navigate to `EtfDetailFragment` passing `symbol` as a Bundle argument.
11. WHEN the user pulls down to refresh, THE `EtfDirectoryViewModel` SHALL re-fetch data from `EtfRepository.getEtfs()`.

---

### Requirement 10 — ETF Detail: Overview

**User Story:** As a market user, I want to see the detailed overview of an ETF so that I can review its pricing, characteristics, and fund metadata.

#### Acceptance Criteria

1. WHEN `EtfDetailFragment` receives a `symbol` Bundle argument, THE `EtfDetailViewModel` SHALL fetch data from `EtfRepository.getEtfDetail(symbol)` within `viewModelScope`.
2. WHILE loading, THE `EtfDetailFragment` SHALL display skeleton placeholder views.
3. WHEN the backend returns the ETF detail, THE `EtfDetailFragment` Overview tab SHALL display: `name`, `symbol`, `fund_manager`, `category`, `benchmark_index`, `expense_ratio`, `inception_date`, `total_assets_pkr`, `is_shariah_compliant`, and `description`.
4. THE Overview tab SHALL display quote fields `current_price`, `change`, `change_percent`, `open_price`, `high_price`, `low_price`, `close_price`, `volume`, `bid_price`, `ask_price`, and `data_as_of` sourced from the `quote` object.
5. THE `change` and `change_percent` SHALL be colored using `R.color.color_positive` for positive values and `R.color.color_negative` for negative values.
6. IF the network request fails, THEN THE `EtfDetailFragment` SHALL display an error state with a retry button.

---

### Requirement 11 — ETF Detail: History

**User Story:** As a market user, I want to view historical OHLCV data for an ETF across selectable timeframes so that I can assess price trends.

#### Acceptance Criteria

1. WHEN the user selects the History tab on `EtfDetailFragment`, THE `EtfDetailViewModel` SHALL fetch data from `EtfRepository.getEtfHistory(symbol, timeframe)` with the currently selected timeframe within `viewModelScope`.
2. THE History tab SHALL display timeframe selection buttons for at least "1M" and "1Y".
3. THE default selected timeframe SHALL be "1M".
4. WHEN a timeframe button is tapped, THE `EtfDetailViewModel` SHALL re-fetch history from `EtfRepository.getEtfHistory(symbol, newTimeframe)`.
5. WHEN the backend returns history data, THE History tab SHALL render closing prices as a line chart using `IntradayChartView.setChartData(primary, secondary)` where `primary` is the list of `close` values.
6. WHEN the backend returns history data, THE History tab SHALL also display a scrollable list of OHLCV rows showing `date`, `open`, `high`, `low`, `close`, and `volume`.
7. WHILE history is loading, THE History tab SHALL display a loading indicator.
8. IF the backend returns empty history, THEN THE History tab SHALL display an empty state message.
9. IF the network request fails, THEN THE History tab SHALL display an error state with a retry action.

---

### Requirement 12 — ETF Detail: Performance

**User Story:** As a market user, I want to compare an ETF's returns against its benchmark index so that I can evaluate how well the fund tracks its target.

#### Acceptance Criteria

1. WHEN the user selects the Performance tab on `EtfDetailFragment`, THE `EtfDetailViewModel` SHALL fetch data from `EtfRepository.getEtfPerformance(symbol)` within `viewModelScope`.
2. WHEN the backend returns performance data, THE Performance tab SHALL display a returns comparison table showing both ETF returns and benchmark returns for periods: `1D`, `1W`, `1M`, `3M`, `1Y`, and `YTD`.
3. THE Performance tab SHALL display `tracking_difference_1m` and `volatility_annualized` as labeled metrics.
4. THE Performance tab SHALL display `benchmark_index` as a labeled reference.
5. THE return values SHALL be colored using `R.color.color_positive` for positive values and `R.color.color_negative` for negative values.
6. WHILE performance data is loading, THE Performance tab SHALL display a loading indicator.
7. IF the network request fails, THEN THE Performance tab SHALL display an error state with a retry action.

---

### Requirement 13 — IpoFragment Tab Host

**User Story:** As a market user, I want a tabbed interface within the IPO section so that I can switch between directory, calendar, and performance views without re-navigating.

#### Acceptance Criteria

1. THE `IpoFragment` SHALL host three tabs: "Directory", "Calendar", and "Performance".
2. WHEN `IpoFragment` is displayed, THE `IpoFragment` SHALL default to displaying the "Directory" tab.
3. WHEN the user taps a tab label, THE `IpoFragment` SHALL switch to the corresponding child Fragment without triggering a full data reload if data is already loaded.
4. THE `IpoViewModel` SHALL be scoped to `IpoFragment` so that all three tab Fragments share a single ViewModel instance using `activityViewModels()` or `by viewModels({ requireParentFragment() })`.
5. WHEN `IpoFragment` is navigated to from `MarketDashboardFragment`, THE `IpoFragment` SHALL display a back navigation affordance that returns the user to `MarketDashboardFragment`.

---

### Requirement 14 — EtfDetailFragment Tab Host

**User Story:** As a market user, I want tabbed sections within the ETF detail screen so that I can switch between Overview, History, and Performance without losing detail context.

#### Acceptance Criteria

1. THE `EtfDetailFragment` SHALL host three tabs: "Overview", "History", and "Performance".
2. WHEN `EtfDetailFragment` is displayed, THE `EtfDetailFragment` SHALL default to displaying the "Overview" tab.
3. WHEN the user taps a tab label, THE `EtfDetailFragment` SHALL switch to the corresponding section.
4. THE `EtfDetailViewModel` SHALL be scoped to `EtfDetailFragment` so all three tab sections share the same ViewModel instance.
5. WHEN `EtfDetailFragment` is navigated to, THE `EtfDetailFragment` SHALL display a back navigation affordance that returns the user to `EtfDirectoryFragment`.

---

### Requirement 15 — State Management and Architecture Compliance

**User Story:** As a developer, I want all IPO and ETF screens to follow the existing project state and architecture patterns so that the codebase remains consistent and maintainable.

#### Acceptance Criteria

1. THE `IpoViewModel` and `EtfDirectoryViewModel` and `EtfDetailViewModel` SHALL expose UI state using `StateFlow<UiState>` where `UiState` is the project's existing sealed interface with `Loading`, `Success`, `Empty`, and `Error` states.
2. THE `IpoViewModel` and `EtfDirectoryViewModel` and `EtfDetailViewModel` SHALL launch all coroutines within `viewModelScope` and SHALL NOT use `GlobalScope`.
3. THE `IpoRepository` and `EtfRepository` SHALL accept their respective API service as a constructor parameter, defaulting to `NetworkClient.ipoApiService` and `NetworkClient.etfApiService` respectively.
4. THE Fragments SHALL collect `StateFlow` using `viewLifecycleOwner.lifecycleScope.launch` inside `repeatOnLifecycle(Lifecycle.State.STARTED)`.
5. THE list adapters for IPO and ETF directories SHALL extend `ListAdapter` and implement `DiffUtil.ItemCallback` keyed on `symbol`.
6. THE `IpoApiService` and `EtfApiService` SHALL be added to `NetworkClient` as `lazy` properties using the existing `marketRetrofit` instance; no new Retrofit or OkHttp instance SHALL be created.
7. IF a network call returns an HTTP error response, THEN THE respective repository SHALL propagate a meaningful error message to the ViewModel so that the `Error` UiState carries a user-readable description.
