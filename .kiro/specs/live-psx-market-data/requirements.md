# Requirements Document

## Introduction

This feature wires the already-implemented `LiveMarketDataSource` / `LiveMarketRepository` data layer into the three visible screens of the Basarat Trade & Invest app — Home (Dashboard), Market Dashboard, and Stock Detail — so that users see real-time PSX price data streamed over WebSocket (with automatic REST fallback and exponential-backoff reconnection). It also adds a non-intrusive connection status chip to the Markets and Stock Detail app bars, and ensures correct subscribe/unsubscribe lifecycle management across all screens.

The data layer is fully complete. No backend, networking, or domain model changes are required. All implementation work is in the ViewModel, UiState, Fragment, and Adapter layers.

---

## Glossary

- **LiveMarketRepository**: The domain-layer interface (`LiveMarketRepository.kt`) whose implementation wraps `LiveMarketDataSource`. ViewModels depend on this interface.
- **LiveMarketDataSource**: The application-singleton OkHttp WebSocket client that owns the single shared connection to `ws://16.16.26.247:8000/api/v1/ws/market`, performs REST fallback polling, and manages reconnection.
- **LiveQuote**: Domain model carrying `symbol`, `current`, `ldcp`, `open`, `high`, `low`, `change`, `changePct`, `volume`, `asOf`, and `source` for a single PSX symbol.
- **WsConnectionState**: Sealed interface representing the connection lifecycle — `Idle`, `Connecting`, `Live`, `Delayed`, `Reconnecting`, `Closed`.
- **quoteUpdates**: The `SharedFlow<LiveQuote>` emitted by `LiveMarketRepository` whenever a new quote arrives.
- **quoteCache**: The `StateFlow<Map<String, LiveQuote>>` snapshot map maintained by `LiveMarketRepository`.
- **HomeViewModel**: ViewModel for the Home/Dashboard screen (`HomeViewModel.kt`).
- **HomeUiState**: Sealed interface describing the state of the Home screen (`HomeUiState.kt`). `HomeUiState.Success` is the data-bearing variant.
- **HomeFragment**: Fragment that renders the Home screen and collects `HomeViewModel.uiState`.
- **MarketDashboardViewModel**: ViewModel for the Market Dashboard screen (`MarketDashboardViewModel.kt`).
- **MarketDashboardUiState**: Sealed interface for the Market Dashboard screen (`MarketDashboardUiState.kt`). `MarketDashboardUiState.Success` is the data-bearing variant.
- **MarketDashboardFragment**: Fragment that renders the Market Dashboard and collects `MarketDashboardViewModel.uiState`.
- **StockDetailViewModel**: ViewModel for the Stock Detail screen (`StockDetailViewModel.kt`).
- **StockDetailUiState**: Sealed interface for the Stock Detail screen (`StockDetailUiState.kt`). `StockDetailUiState.Success` is the data-bearing variant.
- **StockDetailFragment**: Fragment that renders the Stock Detail screen and collects `StockDetailViewModel.uiState`.
- **MarketStockAdapter**: RecyclerView adapter used in the Markets screen for top-symbols rows.
- **ConstituentAdapter**: RecyclerView adapter used in the Markets screen for index-constituent rows.
- **ConnectionStatusChip**: A small, non-modal chip/badge rendered in the app bar of Markets and Stock Detail screens that reflects `WsConnectionState`.
- **Watchlist**: The user's saved list of PSX symbols, loaded by `HomeViewModel` via `WatchlistRepository`.
- **REST Fallback**: Automatic polling of `GET /api/v1/market/quotes` when the WebSocket is unavailable.
- **Exponential Backoff**: Reconnection delay schedule: 1 s → 2 → 4 → 8 → 16 → 30 s (cap).
- **freshnessWindowMs**: 5-minute cache validity window already used in all three ViewModels.

---

## Requirements

### Requirement 1 — HomeUiState Live Quote Fields

**User Story:** As a developer integrating live data into the Home screen, I want `HomeUiState.Success` to carry live quote data and connection state, so that the Home fragment can display live prices without additional ViewModel calls.

#### Acceptance Criteria

1. THE `HomeUiState.Success` data class SHALL include a `liveQuotes: Map<String, LiveQuote>` field with a default value of `emptyMap()`.
2. THE `HomeUiState.Success` data class SHALL include a `wsConnectionState: WsConnectionState` field with a default value of `WsConnectionState.Idle`.
3. WHEN `HomeUiState.Success` is constructed without providing `liveQuotes` or `wsConnectionState`, THE `HomeUiState.Success` SHALL use the default values so that all existing call sites remain source-compatible.

---

### Requirement 2 — HomeViewModel Live Market Integration

**User Story:** As a user viewing the Home screen, I want my watchlist items to show current live prices, so that I can monitor my holdings without navigating away.

#### Acceptance Criteria

1. THE `HomeViewModel` SHALL accept a `liveMarketRepository: LiveMarketRepository` constructor parameter with a default value of `NetworkClient.liveMarketRepository`.
2. WHEN `HomeViewModel` successfully loads a watchlist that contains at least one symbol, THE `HomeViewModel` SHALL call `liveMarketRepository.subscribe(symbols)` with the set of watchlist symbol strings.
3. WHILE `HomeViewModel` is active and has subscribed symbols, THE `HomeViewModel` SHALL collect `liveMarketRepository.quoteUpdates` and update the `liveQuotes` field in the current `HomeUiState.Success` with each received `LiveQuote`.
4. THE `HomeViewModel` SHALL collect `liveMarketRepository.connectionState` and update the `wsConnectionState` field in the current `HomeUiState.Success` whenever the connection state changes.
5. IF a new watchlist is loaded that contains different symbols from the previously subscribed set, THEN THE `HomeViewModel` SHALL call `liveMarketRepository.subscribe()` with the new symbols without calling `unsubscribe` on the old ones, relying on the data source's shared-connection model.
6. THE `HomeViewModel` SHALL NOT call `liveMarketRepository.unsubscribe()` in `onCleared()` because it operates as an activity-scoped ViewModel sharing the singleton connection.

---

### Requirement 3 — HomeFragment Live Price Binding

**User Story:** As a user viewing the Home screen watchlist, I want each watchlist row to display the live current price and change percentage, so that I can see real-time performance at a glance.

#### Acceptance Criteria

1. WHEN `HomeFragment` receives a `HomeUiState.Success` in which `liveQuotes` contains an entry whose key matches a watchlist item's symbol, THE `HomeFragment` SHALL update that watchlist row's price and change-percentage text views with the values from the matching `LiveQuote`.
2. WHEN `HomeFragment` receives a `HomeUiState.Success` in which `liveQuotes` does not contain an entry for a watchlist item's symbol, THE `HomeFragment` SHALL leave that row's price display unchanged from its last REST-fetched value.
3. WHEN `HomeFragment` collects a `HomeUiState.Success` whose `wsConnectionState` is `WsConnectionState.Live`, THE `HomeFragment` SHALL display the watchlist section last-updated label with a "Live" indicator.
4. WHEN `HomeFragment` collects a `HomeUiState.Success` whose `wsConnectionState` is `WsConnectionState.Delayed` or `WsConnectionState.Reconnecting`, THE `HomeFragment` SHALL display the watchlist section last-updated label without a "Live" indicator, reflecting that data may be from REST fallback.

---

### Requirement 4 — MarketDashboardUiState Live Quote Fields

**User Story:** As a developer integrating live data into the Market Dashboard screen, I want `MarketDashboardUiState.Success` to carry live quote data and connection state, so that the Markets fragment can update individual rows without reloading the full list.

#### Acceptance Criteria

1. THE `MarketDashboardUiState.Success` data class SHALL include a `liveQuotes: Map<String, LiveQuote>` field with a default value of `emptyMap()`.
2. THE `MarketDashboardUiState.Success` data class SHALL include a `wsConnectionState: WsConnectionState` field with a default value of `WsConnectionState.Idle`.
3. WHEN `MarketDashboardUiState.Success` is constructed without providing `liveQuotes` or `wsConnectionState`, THE `MarketDashboardUiState.Success` SHALL use the default values so that all existing call sites remain source-compatible.

---

### Requirement 5 — MarketDashboardViewModel Live Market Integration

**User Story:** As a user viewing the Market Dashboard, I want index constituents and top-symbol rows to refresh with live prices, so that I can track market movements without manually refreshing.

#### Acceptance Criteria

1. THE `MarketDashboardViewModel` SHALL accept a `liveMarketRepository: LiveMarketRepository` constructor parameter with a default value of `NetworkClient.liveMarketRepository`.
2. WHEN `MarketDashboardViewModel` successfully loads constituent data for an index, THE `MarketDashboardViewModel` SHALL call `liveMarketRepository.subscribe(symbols)` with the symbols of all loaded constituents.
3. WHEN `MarketDashboardViewModel` successfully loads top-symbol data (gainers, losers, or volume spikes), THE `MarketDashboardViewModel` SHALL call `liveMarketRepository.subscribe(symbols)` with the symbols of all loaded top-symbol rows.
4. WHILE `MarketDashboardViewModel` is active, THE `MarketDashboardViewModel` SHALL collect `liveMarketRepository.quoteUpdates` and update the `liveQuotes` field in the current `MarketDashboardUiState.Success` with each received `LiveQuote`.
5. THE `MarketDashboardViewModel` SHALL collect `liveMarketRepository.connectionState` and update the `wsConnectionState` field in `MarketDashboardUiState.Success` whenever the connection state changes.
6. THE `MarketDashboardViewModel` SHALL NOT call `liveMarketRepository.unsubscribe()` in `onCleared()` because it operates as an activity-scoped ViewModel.

---

### Requirement 6 — MarketStockAdapter Targeted Update

**User Story:** As a developer updating the Markets screen efficiently, I want a method on `MarketStockAdapter` that updates only the affected row, so that the full list does not flicker on every quote tick.

#### Acceptance Criteria

1. THE `MarketStockAdapter` SHALL expose a `updateQuote(symbol: String, quote: LiveQuote)` method.
2. WHEN `updateQuote` is called with a symbol that matches an item currently in the adapter's list, THE `MarketStockAdapter` SHALL update the price and change fields of that item and call `notifyItemChanged` for only that position.
3. WHEN `updateQuote` is called with a symbol that does not match any item in the adapter's list, THE `MarketStockAdapter` SHALL perform no update and SHALL NOT throw an exception.

---

### Requirement 7 — ConstituentAdapter Targeted Update

**User Story:** As a developer updating the Markets screen efficiently, I want a method on `ConstituentAdapter` that updates only the affected row, so that the full constituents list does not flicker on every quote tick.

#### Acceptance Criteria

1. THE `ConstituentAdapter` SHALL expose a `updateQuote(symbol: String, quote: LiveQuote)` method.
2. WHEN `updateQuote` is called with a symbol that matches an item currently in the adapter's list, THE `ConstituentAdapter` SHALL update the price and change fields of that item and call `notifyItemChanged` for only that position.
3. WHEN `updateQuote` is called with a symbol that does not match any item in the adapter's list, THE `ConstituentAdapter` SHALL perform no update and SHALL NOT throw an exception.

---

### Requirement 8 — MarketDashboardFragment Live Price Binding

**User Story:** As a user viewing the Market Dashboard, I want the top-symbol and constituent rows to update prices smoothly when live quotes arrive, so that I can track price movements in real time.

#### Acceptance Criteria

1. WHEN `MarketDashboardFragment` receives a `MarketDashboardUiState.Success` in which `liveQuotes` contains a new or changed entry, THE `MarketDashboardFragment` SHALL call `MarketStockAdapter.updateQuote()` for matching symbols in the top-symbols list, not perform a full adapter `notifyDataSetChanged`.
2. WHEN `MarketDashboardFragment` receives a `MarketDashboardUiState.Success` in which `liveQuotes` contains a new or changed entry, THE `MarketDashboardFragment` SHALL call `ConstituentAdapter.updateQuote()` for matching symbols in the constituents list, not perform a full adapter `notifyDataSetChanged`.
3. WHEN `MarketDashboardFragment` collects a `MarketDashboardUiState.Success` whose `wsConnectionState` changes, THE `MarketDashboardFragment` SHALL update the `ConnectionStatusChip` in the app bar accordingly.

---

### Requirement 9 — StockDetailUiState Live Price Fields

**User Story:** As a developer integrating live data into the Stock Detail screen, I want `StockDetailUiState.Success` to carry live price fields and a streaming flag, so that the detail fragment can overlay live data on top of the REST-fetched static data.

#### Acceptance Criteria

1. THE `StockDetailUiState.Success` data class SHALL include a `livePrice: Double?` field with a default value of `null`.
2. THE `StockDetailUiState.Success` data class SHALL include a `liveChangePct: Double?` field with a default value of `null`.
3. THE `StockDetailUiState.Success` data class SHALL include a `liveChange: Double?` field with a default value of `null`.
4. THE `StockDetailUiState.Success` data class SHALL include a `liveVolume: Long?` field with a default value of `null`.
5. THE `StockDetailUiState.Success` data class SHALL include an `isLivePriceStreaming: Boolean` field with a default value of `false`.
6. WHEN `StockDetailUiState.Success` is constructed without providing any of the live fields, THE `StockDetailUiState.Success` SHALL use the default values so that all existing call sites remain source-compatible.

---

### Requirement 10 — StockDetailViewModel Live Market Integration

**User Story:** As a user viewing a stock's detail page, I want the displayed price and change to update in real time as trades happen, so that I always see the current market price without refreshing.

#### Acceptance Criteria

1. THE `StockDetailViewModel` SHALL accept a `liveMarketRepository: LiveMarketRepository` constructor parameter with a default value of `NetworkClient.liveMarketRepository`.
2. WHEN `StockDetailViewModel.loadStock(symbol)` is called, THE `StockDetailViewModel` SHALL call `liveMarketRepository.subscribe(listOf(symbol))` after the initial REST fetch completes.
3. WHILE `StockDetailViewModel` has an active symbol, THE `StockDetailViewModel` SHALL collect `liveMarketRepository.quoteUpdates`, filter for quotes whose `symbol` matches `currentSymbol`, and update the `livePrice`, `liveChangePct`, `liveChange`, `liveVolume`, and `isLivePriceStreaming` fields in the current `StockDetailUiState.Success`.
4. WHEN a live quote is received for the current symbol and the current state is `StockDetailUiState.Success`, THE `StockDetailViewModel` SHALL set `isLivePriceStreaming` to `true`.
5. WHEN `StockDetailViewModel.loadStock(symbol)` is called with a symbol different from the previously loaded symbol, THE `StockDetailViewModel` SHALL call `liveMarketRepository.unsubscribe(listOf(previousSymbol))` before subscribing to the new symbol.
6. WHEN `StockDetailViewModel.onCleared()` is called and `currentSymbol` is non-empty, THE `StockDetailViewModel` SHALL call `liveMarketRepository.unsubscribe(listOf(currentSymbol))`.
7. THE `StockDetailViewModel` SHALL expose a `liveConnectionState: StateFlow<WsConnectionState>` property derived from `liveMarketRepository.connectionState`.

---

### Requirement 11 — StockDetailFragment Live Price Binding

**User Story:** As a user viewing the Stock Detail screen, I want the price header and change label to reflect the live streamed price as soon as it arrives, so that I see the most current data without any manual action.

#### Acceptance Criteria

1. WHEN `StockDetailFragment` collects a `StockDetailUiState.Success` in which `livePrice` is non-null, THE `StockDetailFragment` SHALL display `livePrice` in the primary price text view in place of the REST-fetched `detail.currentPrice`.
2. WHEN `StockDetailFragment` collects a `StockDetailUiState.Success` in which `liveChangePct` and `liveChange` are non-null, THE `StockDetailFragment` SHALL display `liveChange` and `liveChangePct` in the change and change-percentage text views.
3. WHEN `StockDetailFragment` collects a `StockDetailUiState.Success` in which `isLivePriceStreaming` is `true`, THE `StockDetailFragment` SHALL display the `ConnectionStatusChip` in the app bar showing the current `WsConnectionState`.
4. WHEN `StockDetailFragment` collects a `StockDetailUiState.Success` in which `isLivePriceStreaming` is `false`, THE `StockDetailFragment` SHALL hide or omit the `ConnectionStatusChip`.
5. WHEN `StockDetailFragment` collects a `StockDetailUiState.Success` in which `liveVolume` is non-null, THE `StockDetailFragment` SHALL display `liveVolume` in the volume field of the Quotes section.

---

### Requirement 12 — ConnectionStatusChip Appearance

**User Story:** As a user on the Markets or Stock Detail screen, I want a visible but unobtrusive indicator of whether live data is flowing, so that I know if the prices I see are real-time or delayed.

#### Acceptance Criteria

1. THE `ConnectionStatusChip` SHALL be a small chip or badge widget rendered in the app bar of `MarketDashboardFragment` and `StockDetailFragment`.
2. WHEN `WsConnectionState` is `Live`, THE `ConnectionStatusChip` SHALL display the label "Live" with a green background using the existing BASARAT color system's success or green token.
3. WHEN `WsConnectionState` is `Connecting` or `Reconnecting`, THE `ConnectionStatusChip` SHALL display the label "Connecting" with an amber/warning background using the existing BASARAT color system's warning token.
4. WHEN `WsConnectionState` is `Delayed`, THE `ConnectionStatusChip` SHALL display the label "Delayed" with an amber/warning background using the existing BASARAT color system's warning token.
5. WHEN `WsConnectionState` is `Idle` or `Closed`, THE `ConnectionStatusChip` SHALL be hidden (visibility `GONE`).
6. THE `ConnectionStatusChip` SHALL NOT block or intercept user interaction with the app bar.

---

### Requirement 13 — Subscription Lifecycle Correctness

**User Story:** As a developer maintaining the app, I want subscribe and unsubscribe calls to follow screen lifecycle correctly, so that bandwidth is not wasted and the shared WebSocket connection is not prematurely closed.

#### Acceptance Criteria

1. WHEN `StockDetailViewModel.onCleared()` is called, THE `StockDetailViewModel` SHALL call `liveMarketRepository.unsubscribe(listOf(currentSymbol))` if `currentSymbol` is non-empty.
2. THE `HomeViewModel` SHALL NOT call `liveMarketRepository.unsubscribe()` at any point, because the Home screen is activity-scoped and its subscriptions must remain active while the user navigates the app.
3. THE `MarketDashboardViewModel` SHALL NOT call `liveMarketRepository.unsubscribe()` at any point, because the Market Dashboard is activity-scoped and its subscriptions must remain active while the user navigates the app.
4. WHEN `StockDetailViewModel.loadStock(symbol)` is called with a symbol that differs from the current `currentSymbol`, THE `StockDetailViewModel` SHALL call `liveMarketRepository.unsubscribe(listOf(currentSymbol))` with the old symbol before calling `subscribe` with the new symbol.
5. WHILE no subscribers have called `subscribe` for 60 seconds, THE `LiveMarketDataSource` SHALL close the idle WebSocket connection, as per its existing idle-close logic; this behavior requires no ViewModel changes.

---

### Requirement 14 — REST Fallback Transparency

**User Story:** As a user on any live-data screen, I want the app to continue showing updated prices even when the WebSocket is unavailable, so that a temporary connection failure does not leave me with stale data.

#### Acceptance Criteria

1. WHEN `WsConnectionState` transitions to `Delayed` or `Reconnecting`, THE `LiveMarketDataSource` SHALL continue polling `GET /api/v1/market/quotes` at the interval indicated by `recommended_poll_seconds` from the last REST response, or 15 seconds if no interval was received; this behavior is already implemented and requires no additional changes.
2. WHEN a `LiveQuote` with `source == LiveQuoteSource.REST_FALLBACK` is received by `HomeViewModel`, THE `HomeViewModel` SHALL process it identically to a WebSocket-sourced quote and update `liveQuotes` in `HomeUiState.Success`.
3. WHEN a `LiveQuote` with `source == LiveQuoteSource.REST_FALLBACK` is received by `MarketDashboardViewModel`, THE `MarketDashboardViewModel` SHALL process it identically to a WebSocket-sourced quote and update `liveQuotes` in `MarketDashboardUiState.Success`.
4. WHEN a `LiveQuote` with `source == LiveQuoteSource.REST_FALLBACK` is received by `StockDetailViewModel` for the current symbol, THE `StockDetailViewModel` SHALL update the live price fields in `StockDetailUiState.Success` identically to a WebSocket-sourced quote.
