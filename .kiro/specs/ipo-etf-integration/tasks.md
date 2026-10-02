# Implementation Plan: IPO & ETF Integration + Market Dashboard Chip Navigation

## Overview

Implement the full IPO and ETF data pipeline from backend to UI, plus wire the Market Dashboard chip navigation. The work is structured in dependency order: API contracts and DTOs first, then domain models and mappers, then repositories, then NetworkClient registration, then ViewModels and UI states, then adapters, then Fragments, then layouts, and finally navigation and chip wiring.

All code is Kotlin with XML layouts. No new Retrofit or OkHttp instances are created — all new API services use the existing `marketRetrofit` instance in `NetworkClient`.

## Tasks

- [ ] 1. Define IPO and ETF Retrofit API services
  - [-] 1.1 Create `IpoApiService.kt` in `data/remote/api/`
    - Declare `@GET("api/v1/ipos")` returning `Response<IposResponseDto>`
    - Declare `@GET("api/v1/ipos/{symbol}")` with `@Path("symbol")` returning `Response<IpoItemDto>`
    - Declare `@GET("api/v1/ipos/calendar")` returning `Response<IpoCalendarResponseDto>`
    - Declare `@GET("api/v1/ipos/performance")` returning `Response<IpoPerformanceResponseDto>`
    - All functions are `suspend`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 1.2 Create `EtfApiService.kt` in `data/remote/api/`
    - Declare `@GET("api/v1/etfs")` returning `Response<EtfsResponseDto>`
    - Declare `@GET("api/v1/etfs/{symbol}")` with `@Path("symbol")` returning `Response<EtfItemDto>`
    - Declare `@GET("api/v1/etfs/{symbol}/history")` with `@Path("symbol")` and optional `@Query("timeframe")` returning `Response<EtfHistoryResponseDto>`
    - Declare `@GET("api/v1/etfs/{symbol}/performance")` with `@Path("symbol")` returning `Response<EtfPerformanceDto>`
    - All functions are `suspend`
    - _Requirements: 2.5, 2.6, 2.7, 2.8_

- [ ] 2. Create IPO and ETF DTO data classes
  - [~] 2.1 Create `IpoDtos.kt` in `data/remote/dto/ipo/`
    - `IpoItemDto` with all backend fields using `@SerializedName` (id, symbol, company_name, sector, status, issue_size_shares, issue_size_pkr, floor_price, strike_price, listing_price, current_price, book_building_start, book_building_end, public_subscription_start, public_subscription_end, listing_date, lead_manager, is_shariah_compliant, prospectus_url, description, subscription_multiplier, listing_gain_pct, current_gain_pct, created_at, updated_at)
    - `IposResponseDto` with total, upcoming_count, active_count, listed_count, ipos
    - `IpoCalendarEventDto` with ipo_id, symbol, company_name, event_type, event_date, status, price_info
    - `IpoCalendarResponseDto` with total_events, upcoming_milestones
    - `IpoPerformanceItemDto` with symbol, company_name, sector, listing_date, offer_price, first_day_close, current_price, first_day_return_pct, total_return_pct
    - `IpoPerformanceResponseDto` with total_listed, average_listing_day_gain_pct, top_performers, recent_listings
    - All fields are nullable types except `symbol` in `IpoItemDto`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [~] 2.2 Create `EtfDtos.kt` in `data/remote/dto/etf/`
    - `EtfQuoteDto` with current_price, change, change_percent, open_price, high_price, low_price, close_price, volume, bid_price, ask_price, data_as_of, quote_source
    - `EtfItemDto` with all backend fields (id, symbol, name, fund_manager, category, benchmark_index, is_shariah_compliant, expense_ratio, inception_date, total_assets_pkr, description, is_active, quote, current_price, change, change_percent, volume, created_at, updated_at)
    - `EtfsResponseDto` with total, shariah_count, conventional_count, etfs
    - `EtfHistoryEntryDto` with date, open, high, low, close, volume
    - `EtfHistoryResponseDto` with symbol, timeframe, count, history
    - `EtfReturnsDto` with 1D, 1W, 1M, 3M, 1Y, YTD fields (use `@SerializedName` for numeric-prefixed fields)
    - `EtfPerformanceDto` with symbol, name, benchmark_index, returns, benchmark_returns, tracking_difference_1m, volatility_annualized
    - All fields nullable except `symbol` in `EtfItemDto`
    - _Requirements: 3.7, 3.8, 3.9, 3.10, 3.11_

- [ ] 3. Create domain models and DTO mappers
  - [~] 3.1 Create `IpoModels.kt` in `domain/model/`
    - `IpoItem` data class with Kotlin-idiomatic camelCase names
    - `IpoCalendarEvent` data class
    - `IpoPerformanceItem` data class
    - `IpoPerformanceSummary` data class (totalListed, averageListingDayGainPct, topPerformers, recentListings)
    - `IpoItemDto.toDomain()` extension mapping all fields; fallback `companyName = companyName ?: symbol`; `isShariahCompliant = isShariahCompliant ?: false`
    - `IpoCalendarEventDto.toDomain()` extension
    - `IpoPerformanceItemDto.toDomain()` extension
    - _Requirements: 4.1, 4.3, 4.4_

  - [~] 3.2 Write property test for IPO mapper symbol preservation (Property 1)
    - **Property 1: Mapper symbol preservation — IPO**
    - For any `IpoItemDto` with a non-null `symbol`, `.toDomain().symbol` must equal the original `symbol`
    - **Validates: Requirements 4.1**

  - [~] 3.3 Write property test for IPO DTO round-trip (Property 11)
    - **Property 11: IpoItem DTO round-trip deserialization**
    - For any valid `IpoItem` constructed from a known `IpoItemDto`, the `symbol` field survives the DTO → domain mapper pipeline unchanged
    - **Validates: Requirements 3.1, 4.1**

  - [~] 3.4 Create `EtfModels.kt` in `domain/model/`
    - `EtfQuote` data class with all quote fields
    - `EtfItem` data class with nested `quote: EtfQuote?`
    - `EtfHistoryEntry` data class
    - `EtfReturns` data class (oneDay, oneWeek, oneMonth, threeMonths, oneYear, ytd)
    - `EtfPerformance` data class
    - `EtfsResult` data class (total, shariahCount, conventionalCount, etfs)
    - `EtfQuoteDto.toDomain()` extension
    - `EtfItemDto.toDomain()` extension: `currentPrice = currentPrice ?: quote?.currentPrice`; `isShariahCompliant = isShariahCompliant ?: false`; `isActive = isActive ?: true`
    - `EtfHistoryEntryDto.toDomain()` extension (use `?: 0.0` / `?: 0L` for non-null domain fields)
    - `EtfReturnsDto.toDomain()` extension
    - `EtfPerformanceDto.toDomain()` extension
    - _Requirements: 4.2, 4.5, 4.6_

  - [~] 3.5 Write property test for ETF mapper symbol preservation (Property 2)
    - **Property 2: Mapper symbol preservation — ETF**
    - For any `EtfItemDto` with a non-null `symbol`, `.toDomain().symbol` must equal the original `symbol`
    - **Validates: Requirements 4.2**

  - [~] 3.6 Write property test for ETF mapper quote field propagation (Property 12)
    - **Property 12: ETF mapper quote field propagation**
    - For any `EtfItemDto` with a non-null `quote`, the resulting `EtfItem.quote` must be non-null and `EtfItem.quote.currentPrice` must equal `EtfQuoteDto.currentPrice`
    - **Validates: Requirements 4.2, 10.4**

- [ ] 4. Create repository interfaces and implementations
  - [~] 4.1 Create `IpoRepository.kt` in `domain/repository/`
    - `interface IpoRepository` with four suspend functions:
      - `getIpos(): Result<List<IpoItem>>`
      - `getIpoDetail(symbol: String): Result<IpoItem>`
      - `getIpoCalendar(): Result<List<IpoCalendarEvent>>`
      - `getIpoPerformance(): Result<IpoPerformanceSummary>`
    - _Requirements: 4.7, 4.8, 4.9, 4.10_

  - [~] 4.2 Create `EtfRepository.kt` in `domain/repository/`
    - `interface EtfRepository` with four suspend functions:
      - `getEtfs(): Result<EtfsResult>`
      - `getEtfDetail(symbol: String): Result<EtfItem>`
      - `getEtfHistory(symbol: String, timeframe: String): Result<List<EtfHistoryEntry>>`
      - `getEtfPerformance(symbol: String): Result<EtfPerformance>`
    - _Requirements: 4.11, 4.12, 4.13, 4.14_

  - [~] 4.3 Create `IpoRepositoryImpl.kt` in `data/repository/`
    - Constructor takes `IpoApiService` defaulting to `NetworkClient.ipoApiService`
    - Each method runs in `withContext(Dispatchers.IO)`, returns `Result.success` on successful response or `Result.failure(Exception(NetworkErrorParser.parseError(response)))` on HTTP error
    - Re-throws `CancellationException` before catching general `Exception`
    - `getIpoPerformance()` maps the response to `IpoPerformanceSummary`
    - _Requirements: 15.3, 15.7_

  - [~] 4.4 Write property test for repository error message non-blank invariant (Property 10)
    - **Property 10: Repository error message non-blank invariant**
    - For any HTTP 4xx/5xx response, the error path in `IpoRepositoryImpl` must produce `Result.failure` whose `exception.message` is non-null and non-blank
    - **Validates: Requirements 15.7**

  - [~] 4.5 Create `EtfRepositoryImpl.kt` in `data/repository/`
    - Constructor takes `EtfApiService` defaulting to `NetworkClient.etfApiService`
    - Same error-handling pattern as `IpoRepositoryImpl`
    - `getEtfs()` maps `EtfsResponseDto` to `EtfsResult` (total, shariahCount, conventionalCount, etfs list)
    - _Requirements: 15.3, 15.7_

- [ ] 5. Register services and repositories in NetworkClient
  - [~] 5.1 Modify `data/remote/NetworkClient.kt`
    - Add `val ipoApiService: IpoApiService by lazy { marketRetrofit.create(IpoApiService::class.java) }`
    - Add `val etfApiService: EtfApiService by lazy { marketRetrofit.create(EtfApiService::class.java) }`
    - Add `val ipoRepository: IpoRepository by lazy { IpoRepositoryImpl(ipoApiService) }`
    - Add `val etfRepository: EtfRepository by lazy { EtfRepositoryImpl(etfApiService) }`
    - Do NOT create any new OkHttp or Retrofit instance
    - _Requirements: 2.9, 2.10, 15.6_

- [~] 6. Checkpoint — Data layer complete
  - Ensure all data layer files compile: API services, DTOs, domain models, repositories, NetworkClient additions
  - Ensure `CancellationException` re-throw is present in all repository catch blocks
  - Ensure all `@SerializedName` annotations match exact backend field names from the design
  - Ask the user if any questions arise before proceeding to ViewModels

- [ ] 7. Create IPO UI states and ViewModel
  - [~] 7.1 Create `IpoUiState.kt` in `feature/ipo/`
    - `sealed interface IpoDirectoryUiState` with `Loading`, `Success(items: List<IpoItem>)`, `Empty`, `Error(message: String)`
    - `sealed interface IpoCalendarUiState` with `Loading`, `Success(events: List<IpoCalendarEvent>)`, `Empty`, `Error(message: String)`
    - `sealed interface IpoPerformanceUiState` with `Loading`, `Success(summary: IpoPerformanceSummary)`, `Empty`, `Error(message: String)`
    - Use `data object` for `Loading` and `Empty`; `data class` for `Success` and `Error`
    - _Requirements: 15.1_

  - [~] 7.2 Create `IpoDetailUiState.kt` in `feature/ipo/`
    - `sealed interface IpoDetailUiState` with `Loading`, `Success(ipo: IpoItem)`, `Error(message: String)`
    - _Requirements: 15.1_

  - [~] 7.3 Create `IpoViewModel.kt` in `feature/ipo/`
    - Constructor: `private val repository: IpoRepository = NetworkClient.ipoRepository`
    - Three `MutableStateFlow` backing fields, three public `StateFlow` properties: `directoryState`, `calendarState`, `performanceState`
    - `init` block calls `loadDirectory()`, `loadCalendar()`, `loadPerformance()`
    - Each load function launches in `viewModelScope`, emits `Loading` first, then `Success`/`Empty`/`Error`
    - Public retry methods: `loadDirectory()`, `loadCalendar()`, `loadPerformance()` (same as initial load, idempotent)
    - _Requirements: 5.1, 5.8, 6.1, 7.1, 13.3, 13.4, 15.1, 15.2_

  - [~] 7.4 Create `IpoDetailViewModel.kt` in `feature/ipo/`
    - Constructor: `private val repository: IpoRepository = NetworkClient.ipoRepository`
    - Single `MutableStateFlow<IpoDetailUiState>`, exposed as `uiState: StateFlow<IpoDetailUiState>`
    - `fun load(symbol: String)` launches in `viewModelScope`, emits `Loading` → `Success`/`Error`
    - _Requirements: 8.1, 8.10, 15.1, 15.2_

- [ ] 8. Create ETF UI states and ViewModels
  - [~] 8.1 Create `EtfDirectoryUiState.kt` in `feature/etf/`
    - `sealed interface EtfDirectoryUiState` with `Loading`, `Success(total, shariahCount, conventionalCount, etfs)`, `Empty`, `Error(message: String)`
    - _Requirements: 15.1_

  - [~] 8.2 Create `EtfDetailUiState.kt` in `feature/etf/`
    - `enum class EtfDetailTab { OVERVIEW, HISTORY, PERFORMANCE }`
    - `sealed interface EtfHistoryUiState` with `Loading`, `Success(entries: List<EtfHistoryEntry>)`, `Empty`, `Error(message: String)`
    - `sealed interface EtfPerformanceUiState` with `Loading`, `Success(performance: EtfPerformance)`, `Error(message: String)`
    - `sealed interface EtfDetailUiState` with `Loading`, `Success(etf, selectedTab, historyState, performanceState, selectedTimeframe)`, `Error(message: String)`
    - _Requirements: 15.1_

  - [~] 8.3 Create `EtfDirectoryViewModel.kt` in `feature/etf/`
    - Constructor: `private val repository: EtfRepository = NetworkClient.etfRepository`
    - Single `MutableStateFlow<EtfDirectoryUiState>`, exposed as `uiState: StateFlow<EtfDirectoryUiState>`
    - `init` calls `load()`; public `fun load()` is the retry method
    - Maps `EtfsResult` to `Success` with all summary counts
    - _Requirements: 9.1, 9.9, 15.1, 15.2_

  - [~] 8.4 Create `EtfDetailViewModel.kt` in `feature/etf/`
    - Constructor: `private val repository: EtfRepository = NetworkClient.etfRepository`
    - Single `MutableStateFlow<EtfDetailUiState>`, exposed as `uiState: StateFlow<EtfDetailUiState>`
    - `fun loadDetail(symbol: String)`: fetches ETF detail, emits `Loading` → `Success(etf, selectedTab = OVERVIEW, ...)`
    - `fun selectTab(tab: EtfDetailTab)`: updates `selectedTab` in current `Success` state; lazily triggers `loadHistory` or `loadPerformance` if not yet loaded
    - `fun selectTimeframe(symbol: String, timeframe: String)`: re-fetches history with new timeframe, updates `historyState` inside existing `Success`
    - `fun retryPerformance(symbol: String)`: re-fetches performance
    - `fun retryHistory(symbol: String)`: re-fetches history with current timeframe
    - _Requirements: 10.1, 11.1, 11.4, 12.1, 14.3, 14.4, 15.1, 15.2_

- [ ] 9. Create list adapters
  - [~] 9.1 Create `IpoListAdapter.kt` in `feature/ipo/adapter/`
    - Extends `ListAdapter<IpoItem, IpoListAdapter.ViewHolder>`
    - `DiffUtil.ItemCallback` keyed on `symbol` (`areItemsTheSame`) and data-class equality (`areContentsTheSame`)
    - Constructor accepts `onItemClick: (IpoItem) -> Unit`
    - `ViewHolder` uses `ItemIpoCardBinding` (ViewBinding); binds companyName, symbol, sector, status badge color, floorPrice, Shariah indicator visibility
    - Status badge color: `ACTIVE` → `R.color.color_positive`, `UPCOMING` → `R.color.color_warning`, else neutral
    - _Requirements: 5.3, 5.4, 5.5, 15.5_

  - [~] 9.2 Write property test for IPO list adapter count invariant (Property 3)
    - **Property 3: IPO list adapter count invariant**
    - For any non-empty `List<IpoItem>` submitted via `submitList`, `adapter.itemCount` must equal the list size
    - **Validates: Requirements 5.3, 15.5**

  - [~] 9.3 Write property test for DiffUtil symbol identity — IPO (Property 5)
    - **Property 5: DiffUtil symbol identity — IPO**
    - `IpoListAdapter.DIFF.areItemsTheSame(a, b)` returns `true` if and only if `a.symbol == b.symbol`
    - **Validates: Requirements 5.3, 15.5**

  - [~] 9.4 Create `IpoCalendarAdapter.kt` in `feature/ipo/adapter/`
    - Extends `ListAdapter<IpoCalendarEvent, IpoCalendarAdapter.ViewHolder>`
    - `DiffUtil.ItemCallback` keyed on `symbol`
    - `ViewHolder` uses `ItemIpoCalendarEventBinding`; binds companyName, symbol, humanized eventType label, eventDate, status (COMPLETED → secondary color, SCHEDULED → primary text color)
    - `eventType` label mapping: `BOOK_BUILDING_START` → "Book Building Start", `BOOK_BUILDING_END` → "Book Building End", `PUBLIC_SUBSCRIPTION_START` → "Public Subscription Start", `PUBLIC_SUBSCRIPTION_END` → "Public Subscription End", `LISTING_DATE` → "Listing Date", else raw value
    - _Requirements: 6.3, 6.4, 6.5_

  - [~] 9.5 Write property test for IPO calendar event type label mapping (Property 8)
    - **Property 8: IPO calendar event type label mapping**
    - For each eventType in `{BOOK_BUILDING_START, BOOK_BUILDING_END, PUBLIC_SUBSCRIPTION_START, PUBLIC_SUBSCRIPTION_END, LISTING_DATE}`, the label function returns a non-empty string that differs from the raw input
    - **Validates: Requirements 6.4**

  - [~] 9.6 Create `IpoPerformanceAdapter.kt` in `feature/ipo/adapter/`
    - Extends `ListAdapter<IpoPerformanceItem, IpoPerformanceAdapter.ViewHolder>`
    - `DiffUtil.ItemCallback` keyed on `symbol`
    - `ViewHolder` uses `ItemIpoPerformanceBinding`; binds symbol, companyName, sector, listingDate, offerPrice, firstDayReturnPct, totalReturnPct; colors return percentages using positive/negative color logic
    - _Requirements: 7.4, 7.5, 7.6_

  - [~] 9.7 Write property test for return percentage sign-to-color mapping (Property 7)
    - **Property 7: Return percentage sign-to-color mapping**
    - For any non-null Double, the color resolution function returns `R.color.color_positive` for values > 0 and `R.color.color_negative` for values < 0
    - **Validates: Requirements 7.6, 8.7, 8.8, 9.5, 10.5, 12.5**

  - [~] 9.8 Create `EtfListAdapter.kt` in `feature/etf/adapter/`
    - Extends `ListAdapter<EtfItem, EtfListAdapter.ViewHolder>`
    - `DiffUtil.ItemCallback` keyed on `symbol`
    - Constructor accepts `onItemClick: (EtfItem) -> Unit`
    - `ViewHolder` uses `ItemEtfCardBinding`; binds name, symbol, fundManager, category, currentPrice, changePercent (colored), Shariah indicator visibility
    - _Requirements: 9.3, 9.4, 9.5, 15.5_

  - [~] 9.9 Write property test for ETF list adapter count invariant (Property 4)
    - **Property 4: ETF list adapter count invariant**
    - For any non-empty `List<EtfItem>` submitted via `submitList`, `adapter.itemCount` must equal the list size
    - **Validates: Requirements 9.3, 15.5**

  - [~] 9.10 Write property test for DiffUtil symbol identity — ETF (Property 6)
    - **Property 6: DiffUtil symbol identity — ETF**
    - `EtfListAdapter.DIFF.areItemsTheSame(a, b)` returns `true` if and only if `a.symbol == b.symbol`
    - **Validates: Requirements 9.3, 15.5**

- [ ] 10. Create item layouts
  - [~] 10.1 Create `res/layout/item_ipo_card.xml`
    - Root: `ConstraintLayout` with standard card padding
    - Views: company name `TextView`, symbol `TextView`, sector `TextView`, status badge `TextView` (background tint set programmatically), floor price `TextView`, Shariah indicator `ImageView` (visible only when compliant)
    - Provide stable `android:id` for each view matching `ItemIpoCardBinding` field names
    - _Requirements: 5.4, 5.5_

  - [~] 10.2 Create `res/layout/item_ipo_calendar_event.xml`
    - Root: `ConstraintLayout`
    - Views: company name, symbol, event type label `TextView`, event date `TextView`, status `TextView`
    - _Requirements: 6.3_

  - [~] 10.3 Create `res/layout/item_ipo_performance.xml`
    - Root: `ConstraintLayout`
    - Views: symbol, company name, sector, listing date, offer price, first-day return % `TextView`, total return % `TextView`
    - _Requirements: 7.4, 7.5_

  - [~] 10.4 Create `res/layout/item_etf_card.xml`
    - Root: `ConstraintLayout`
    - Views: name, symbol, fund manager, category, current price `TextView`, change percent `TextView` (colored in adapter)
    - _Requirements: 9.4_

- [ ] 11. Create fragment layouts
  - [~] 11.1 Populate `res/layout/fragment_ipo_directory.xml` (replaces empty file)
    - Root: `SwipeRefreshLayout` wrapping `NestedScrollView`
    - Skeleton loading group (`View` with `@drawable/bg_skeleton`, `android:id="@+id/skeletonGroup"`)
    - `RecyclerView` (`android:id="@+id/recyclerViewIpos"`)
    - Empty state `TextView` (`android:id="@+id/textViewEmpty"`)
    - Error state `TextView` (`android:id="@+id/textViewError"`) + retry `Button` (`android:id="@+id/buttonRetry"`)
    - _Requirements: 5.2, 5.6, 5.7_

  - [~] 11.2 Create `res/layout/fragment_ipo_detail.xml`
    - Root: `NestedScrollView`
    - Header row: back arrow `ImageView` + company name `TextView`
    - Status badge `TextView`
    - Price row: floor price, strike price, listing price, current price `TextViews` (nullable-guarded visibility in Fragment)
    - Dates section: book-building range, subscription range, listing date `TextViews`
    - Gain rows: listing gain % `TextView`, current gain % `TextView` (colors set in Fragment)
    - Description `TextView`
    - Shariah badge `TextView` (visibility controlled in Fragment)
    - "View Prospectus" `Button` (visibility controlled by ViewModel)
    - Skeleton overlay group + error group with retry `Button`
    - _Requirements: 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10_

  - [~] 11.3 Create `res/layout/fragment_etf_directory.xml`
    - Root: `SwipeRefreshLayout` wrapping `NestedScrollView`
    - Summary header `ConstraintLayout`: total count `TextView`, Shariah count `TextView`, conventional count `TextView`
    - Skeleton group, `RecyclerView`, empty state, error state + retry button
    - _Requirements: 9.2, 9.6, 9.7, 9.8_

  - [~] 11.4 Create `res/layout/fragment_etf_detail.xml`
    - Root: `NestedScrollView`
    - Header: back arrow `ImageView`, ETF name `TextView`, symbol `TextView`
    - Price row: current price, change amount, change percent `TextViews`
    - Three tab buttons (`MaterialButton` or styled `TextView`): "Overview", "History", "Performance" with `android:id` values `tabOverview`, `tabHistory`, `tabPerformance`
    - Overview section container (`android:id="@+id/sectionOverview"`): fund metadata fields + quote fields
    - History section container (`android:id="@+id/sectionHistory"`): timeframe button row ("1M", "1Y"), `IntradayChartView`, `RecyclerView` for OHLCV rows
    - Performance section container (`android:id="@+id/sectionPerformance"`): returns comparison `TextView` rows + tracking/volatility metrics
    - Skeleton overlay group + error group with retry `Button`
    - _Requirements: 10.2, 10.3, 10.4, 11.2, 11.3, 11.5, 11.6, 12.2, 12.3, 12.4, 14.1, 14.2_

- [ ] 12. Create IPO Fragments
  - [~] 12.1 Create `IpoUiState.kt` page-host layout `fragment_ipo_host.xml` (ViewPager2 + TabLayout) inline during `IpoFragment` creation
    - Root: `CoordinatorLayout` or `LinearLayout`
    - `com.google.android.material.tabs.TabLayout` with `android:id="@+id/tabLayout"`
    - `androidx.viewpager2.widget.ViewPager2` with `android:id="@+id/viewPager"`
    - Back arrow `ImageView` in a top toolbar row
    - _Requirements: 13.1, 13.5_

  - [~] 12.2 Create `IpoFragment.kt` in `feature/ipo/`
    - Hosts `ViewPager2` with a `FragmentStateAdapter` creating `IpoDirectoryFragment`, `IpoCalendarFragment`, `IpoPerformanceFragment`
    - Binds `TabLayout` to `ViewPager2` via `TabLayoutMediator` with tab titles "Directory", "Calendar", "Performance"
    - Default tab is index 0 (Directory)
    - Back arrow click calls `findNavController().popBackStack()`
    - Does NOT own the `IpoViewModel` — ViewModel is instantiated via `by viewModels()` in child fragments using `requireParentFragment()`
    - _Requirements: 13.1, 13.2, 13.5_

  - [~] 12.3 Create `IpoDirectoryFragment.kt` inline in `feature/ipo/`
    - Uses layout `fragment_ipo_directory.xml`
    - Obtains shared ViewModel: `private val viewModel: IpoViewModel by viewModels({ requireParentFragment() })`
    - In `onViewCreated`: sets up `IpoListAdapter`, attaches to RecyclerView, collects `viewModel.directoryState` in `repeatOnLifecycle(STARTED)`
    - `Loading` → show skeleton, hide list/empty/error
    - `Success` → hide skeleton, show list, call `adapter.submitList(items)`
    - `Empty` → hide skeleton/list, show empty text
    - `Error` → hide skeleton/list, show error text + retry button; retry calls `viewModel.loadDirectory()`
    - `SwipeRefreshLayout` refresh calls `viewModel.loadDirectory()`, hides refreshing indicator on non-Loading state
    - Item click navigates to `IpoDetailFragment` with `symbol` in Bundle
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 15.4_

  - [~] 12.4 Create `IpoCalendarFragment.kt` in `feature/ipo/`
    - Uses layout for calendar (reuse `fragment_ipo_directory.xml` skeleton pattern or a dedicated lightweight layout)
    - Obtains shared ViewModel: `by viewModels({ requireParentFragment() })`
    - Collects `viewModel.calendarState`, drives `IpoCalendarAdapter`
    - Pull-to-refresh calls `viewModel.loadCalendar()`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 15.4_

  - [~] 12.5 Create `IpoPerformanceFragment.kt` in `feature/ipo/`
    - Obtains shared ViewModel: `by viewModels({ requireParentFragment() })`
    - Collects `viewModel.performanceState`
    - On `Success`: shows summary header (totalListed, averageListingDayGainPct), drives `IpoPerformanceAdapter` for `topPerformers`, drives a second section listing `recentListings`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 15.4_

  - [~] 12.6 Create `IpoDetailFragment.kt` in `feature/ipo/`
    - Uses layout `fragment_ipo_detail.xml`
    - Receives `symbol` from `arguments` (key `"symbol"`)
    - Instantiates `IpoDetailViewModel` via `by viewModels()`
    - In `onViewCreated`: calls `viewModel.load(symbol)`, collects `viewModel.uiState` in `repeatOnLifecycle(STARTED)`
    - `Loading` → show skeleton overlay, hide content
    - `Success(ipo)` → hide skeleton, bind all fields; nullable fields set `View.VISIBLE`/`View.GONE`; gain % colored by sign using `R.color.color_positive`/`R.color.color_negative`; Shariah badge visible only if `isShariahCompliant`; "View Prospectus" button visible only if `prospectusUrl` non-null and non-blank, click opens `Intent(Intent.ACTION_VIEW, Uri.parse(url))`
    - `Error(msg)` → show error group with retry button calling `viewModel.load(symbol)`
    - Back arrow calls `findNavController().popBackStack()`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10, 15.4_

- [ ] 13. Create ETF Fragments
  - [~] 13.1 Create `EtfDirectoryFragment.kt` in `feature/etf/`
    - Uses layout `fragment_etf_directory.xml`
    - Instantiates `EtfDirectoryViewModel` via `by viewModels()`
    - In `onViewCreated`: sets up `EtfListAdapter`, collects `viewModel.uiState` in `repeatOnLifecycle(STARTED)`
    - `Success` → shows summary header counts, drives adapter
    - Item click navigates to `EtfDetailFragment` with `symbol` in Bundle
    - Pull-to-refresh calls `viewModel.load()`
    - Back arrow calls `findNavController().popBackStack()`
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10, 9.11, 15.4_

  - [~] 13.2 Create `EtfDetailFragment.kt` in `feature/etf/`
    - Uses layout `fragment_etf_detail.xml`
    - Receives `symbol` from `arguments` (key `"symbol"`)
    - Instantiates `EtfDetailViewModel` via `by viewModels()`
    - In `onViewCreated`: calls `viewModel.loadDetail(symbol)`; collects `viewModel.uiState` in `repeatOnLifecycle(STARTED)`
    - Tab buttons (`tabOverview`, `tabHistory`, `tabPerformance`) call `viewModel.selectTab(tab)` on click
    - `Success.selectedTab` determines which section container is `VISIBLE`; others are `GONE`
    - Overview section: binds all ETF fields and quote fields; colors change/changePercent by sign
    - History section: timeframe buttons call `viewModel.selectTimeframe(symbol, tf)`; on `Success(entries)` calls `intradayChartView.setChartData(entries.map { it.close.toFloat() }, emptyList())`; drives a plain `RecyclerView` adapter for OHLCV rows
    - Performance section: binds returns table rows and metric labels; colors return values by sign
    - Loading/Error states handled at section level for history and performance sub-states; top-level `Loading`/`Error` handled for the detail load
    - Back arrow calls `findNavController().popBackStack()`
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 14.1, 14.2, 14.3, 14.5, 15.4_

  - [~] 13.3 Write property test for ETF history close price extraction (Property 9)
    - **Property 9: ETF history close price extraction**
    - For any non-empty `List<EtfHistoryEntry>`, the extracted primary chart data must have the same length as the input and `primary[i] == entries[i].close`
    - **Validates: Requirements 11.5**

- [ ] 14. Wire navigation graph and chip listeners
  - [~] 14.1 Modify `res/navigation/nav_graph.xml`
    - Add `<fragment android:id="@+id/ipoFragment" ... />` destination
    - Add `<fragment android:id="@+id/ipoDetailFragment" ... />` destination
    - Add `<fragment android:id="@+id/etfDirectoryFragment" ... />` destination
    - Add `<fragment android:id="@+id/etfDetailFragment" ... />` destination
    - Add action `action_markets_to_ipoFragment` on the `nav_markets` destination
    - Add action `action_markets_to_etfDirectoryFragment` on the `nav_markets` destination
    - Add action from `ipoFragment` to `ipoDetailFragment`
    - Add action from `etfDirectoryFragment` to `etfDetailFragment`
    - _Requirements: 1.5_

  - [~] 14.2 Add chip IDs to `res/layout/fragment_market_dashboard.xml`
    - Find the existing IPO chip and assign `android:id="@+id/chipIpo"`
    - Find the existing ETF chip and assign `android:id="@+id/chipEtf"`
    - Make NO other changes to this file
    - _Requirements: 1.1, 1.2_

  - [~] 14.3 Modify `feature/markets/MarketDashboardFragment.kt`
    - Inside `setupTopBarListeners()` (or equivalent chip setup method), add:
      ```kotlin
      binding.chipIpo.setOnClickListener {
          findNavController().navigate(R.id.ipoFragment)
      }
      binding.chipEtf.setOnClickListener {
          findNavController().navigate(R.id.etfDirectoryFragment)
      }
      ```
    - Make NO other changes to this fragment
    - _Requirements: 1.3, 1.4_

- [~] 15. Final checkpoint — Ensure all tests pass
  - Ensure all tasks pass compilation
  - Verify no new Retrofit/OkHttp instances were created (only `marketRetrofit.create(...)`)
  - Verify `CancellationException` is re-thrown in both repository implementations
  - Verify chip navigation actions match nav_graph destination IDs
  - Verify no mock/hardcoded financial data was introduced anywhere
  - Ask the user if any questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at the data layer boundary and at the end
- Property tests validate universal correctness properties defined in the design document
- Unit tests validate specific examples and edge cases
- The `IpoViewModel` is shared across three tab fragments via `by viewModels({ requireParentFragment() })` — no `activityViewModels()` needed
- `EtfDetailFragment` uses show/hide section containers instead of nested ViewPager2 — matches `StockDetailFragment` pattern
- The `IntradayChartView.setChartData` call passes `entries.map { it.close.toFloat() }` as `primary` and `emptyList()` as `secondary`
- All color-by-sign logic (positive/negative return percentages) centralizes in a single helper function referenced from adapters and fragments

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.4"] },
    { "id": 3, "tasks": ["3.2", "3.3", "3.5", "3.6", "4.1", "4.2"] },
    { "id": 4, "tasks": ["4.3", "4.5"] },
    { "id": 5, "tasks": ["4.4", "5.1"] },
    { "id": 6, "tasks": ["7.1", "7.2", "8.1", "8.2"] },
    { "id": 7, "tasks": ["7.3", "7.4", "8.3", "8.4"] },
    { "id": 8, "tasks": ["9.1", "9.4", "9.6", "9.8"] },
    { "id": 9, "tasks": ["9.2", "9.3", "9.5", "9.7", "9.9", "9.10", "10.1", "10.2", "10.3", "10.4"] },
    { "id": 10, "tasks": ["11.1", "11.2", "11.3", "11.4"] },
    { "id": 11, "tasks": ["12.1", "12.2", "12.3", "12.4", "12.5", "12.6", "13.1", "13.2"] },
    { "id": 12, "tasks": ["13.3", "14.1", "14.2", "14.3"] }
  ]
}
```
