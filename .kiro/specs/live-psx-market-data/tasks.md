# Implementation Plan: Live PSX Market Data (WebSocket + REST Fallback)

## Overview

Wire the already-complete `LiveMarketDataSource` / `LiveMarketRepository` data layer into the UI layer across three screens — Home (Dashboard), Market Dashboard, and Stock Detail. Work is confined to UiState models, ViewModels, RecyclerView adapters, Fragments, and XML layouts. No changes to the network stack, domain models, or DTOs are required.

---

## Tasks

- [x] 1. Add `liveMarketRepository` to `NetworkClient` and create the chip drawable
  - [x] 1.1 Add `liveMarketRepository` lazy property to `NetworkClient.kt`
    - Add `val liveMarketRepository: LiveMarketRepository by lazy { LiveMarketRepositoryImpl(liveMarketDataSource) }` to `NetworkClient`
    - Import `LiveMarketRepository` and `LiveMarketRepositoryImpl`
    - _Requirements: 2.1, 5.1, 10.1_

  - [x] 1.2 Create `bg_connection_chip.xml` drawable
    - Create `app/src/main/res/drawable/bg_connection_chip.xml` as a rounded rectangle shape (radius 12dp) with a solid default green fill
    - _Requirements: 12.1_

- [ ] 2. Home screen — UiState and ViewModel
  - [x] 2.1 Add live fields to `HomeUiState.Success`
    - Add `val liveQuotes: Map<String, LiveQuote> = emptyMap()` to `HomeUiState.Success`
    - Add `val wsConnectionState: WsConnectionState = WsConnectionState.Idle` to `HomeUiState.Success`
    - Verify all existing `HomeUiState.Success(...)` call sites compile without changes
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ] 2.2 Write property test for `HomeUiState.Success` default compatibility
    - **Property 13: UiState Default Compatibility**
    - Construct `HomeUiState.Success` without the new fields; assert `liveQuotes == emptyMap()` and `wsConnectionState == WsConnectionState.Idle`
    - **Validates: Requirements 1.3**

  - [ ] 2.3 Integrate `LiveMarketRepository` into `HomeViewModel`
    - Add `private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository` constructor parameter
    - In `init {}`, launch a coroutine that collects `liveMarketRepository.quoteUpdates` and applies `copy(liveQuotes = ...)` to the current `HomeUiState.Success`
    - In `init {}`, launch a coroutine that collects `liveMarketRepository.connectionState` and applies `copy(wsConnectionState = ...)` to the current `HomeUiState.Success`
    - In `loadDashboard()`, after a non-empty watchlist resolves, call `liveMarketRepository.subscribe(symbols)`
    - Do **not** add any `unsubscribe()` call anywhere in `HomeViewModel`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 13.2_

  - [ ] 2.4 Write property test for quote round-trip in `HomeViewModel`
    - **Property 1: Quote Emission Round-Trip (Home)**
    - Use a fake `LiveMarketRepository` that emits a known `LiveQuote`; assert the resulting `HomeUiState.Success.liveQuotes` contains it keyed by `symbol.uppercase()`
    - **Validates: Requirements 2.3, 14.2**

  - [ ] 2.5 Write property test for connection state propagation in `HomeViewModel`
    - **Property 5: Connection State Propagation**
    - Emit each `WsConnectionState` variant from a fake repository; assert `HomeUiState.Success.wsConnectionState` matches exactly
    - **Validates: Requirements 2.4**

  - [ ] 2.6 Write property test for subscribe-on-load in `HomeViewModel`
    - **Property 6: Subscribe-on-Load (Home Watchlist)**
    - Assert that `liveMarketRepository.subscribe()` is called with exactly the watchlist symbols after `loadDashboard()` resolves a non-empty watchlist
    - **Validates: Requirements 2.2**

- [ ] 3. Checkpoint — Home ViewModel compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Home screen — Fragment binding
  - [ ] 4.1 Apply live quote overlays and connection label in `HomeFragment`
    - In `bindSuccessState()`, after `bindWatchlist()`, call `applyLiveQuotesToWatchlist(state)` that iterates watchlist items and updates price/change text views for any symbol present in `state.liveQuotes`
    - Add `bindLiveConnectionLabel(state.wsConnectionState)` that appends a "● Live" suffix to `tvWatchlistLastUpdated` when state is `WsConnectionState.Live`
    - Store the REST-fetched timestamp string in `tvWatchlistLastUpdated.tag` inside `bindWatchlist()` so the suffix function can retrieve the base text
    - Leave unchanged any row whose symbol is absent from `liveQuotes` (Requirement 3.2)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 5. Market Dashboard — UiState and ViewModel
  - [x] 5.1 Add live fields to `MarketDashboardUiState.Success`
    - Add `val liveQuotes: Map<String, LiveQuote> = emptyMap()` to `MarketDashboardUiState.Success`
    - Add `val wsConnectionState: WsConnectionState = WsConnectionState.Idle` to `MarketDashboardUiState.Success`
    - Verify existing `MarketDashboardUiState.Success(...)` call sites compile without changes
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ] 5.2 Write property test for `MarketDashboardUiState.Success` default compatibility
    - **Property 13: UiState Default Compatibility**
    - Construct `MarketDashboardUiState.Success` without new fields; assert `liveQuotes == emptyMap()` and `wsConnectionState == WsConnectionState.Idle`
    - **Validates: Requirements 4.3**

  - [ ] 5.3 Integrate `LiveMarketRepository` into `MarketDashboardViewModel`
    - Add `private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository` constructor parameter
    - In `init {}`, launch coroutines to collect `quoteUpdates` (copy liveQuotes) and `connectionState` (copy wsConnectionState) into current `MarketDashboardUiState.Success`
    - In `loadMarketData()`, after both constituent and top-symbol results resolve, collect all symbols into a set and call `liveMarketRepository.subscribe(allSymbols)`
    - In `setFilter()`, after top-symbol results resolve, call `liveMarketRepository.subscribe(topSymbols)`
    - In `selectConstituentIndex()`, after constituents resolve, call `liveMarketRepository.subscribe(constituentSymbols)`
    - Do **not** add any `unsubscribe()` call anywhere in `MarketDashboardViewModel`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 13.3_

  - [ ] 5.4 Write property test for quote round-trip in `MarketDashboardViewModel`
    - **Property 2: Quote Emission Round-Trip (Market Dashboard)**
    - Emit a known `LiveQuote` from a fake repository; assert `MarketDashboardUiState.Success.liveQuotes` contains it keyed by `symbol.uppercase()`
    - **Validates: Requirements 5.4, 14.3**

  - [ ] 5.5 Write property test for subscribe-on-load in `MarketDashboardViewModel`
    - **Property 7: Subscribe-on-Load (Market Dashboard)**
    - Assert `liveMarketRepository.subscribe()` is called with constituent + top-symbol symbols after `loadMarketData()` completes
    - **Validates: Requirements 5.2, 5.3**

- [ ] 6. Market Dashboard — Adapters
  - [ ] 6.1 Add shadow list and `updateQuote()` to `MarketStockAdapter`
    - Add `private val internalList = mutableListOf<MarketStock>()`
    - Override `submitList()` to sync `internalList` before delegating to super
    - Add `PAYLOAD_LIVE_QUOTE` constant in companion object
    - Override `onBindViewHolder(holder, position, payloads)` to call `holder.bindLiveUpdate(internalList[position])` when payload is `PAYLOAD_LIVE_QUOTE`
    - Add `bindLiveUpdate()` on the ViewHolder that updates only price and change text views
    - Add `fun updateQuote(symbol: String, quote: LiveQuote)` that finds the position via `currentList.indexOfFirst`, updates `internalList[position]`, and calls `notifyItemChanged(position, PAYLOAD_LIVE_QUOTE)`; returns silently if position is -1
    - _Requirements: 6.1, 6.2, 6.3_

  - [ ] 6.2 Write property test for `MarketStockAdapter.updateQuote()` — match
    - **Property 11: Adapter Targeted Update — Match**
    - Assert that calling `updateQuote(symbol, quote)` for a present symbol updates only that position and does not call `notifyDataSetChanged()`
    - **Validates: Requirements 6.2**

  - [ ] 6.3 Write property test for `MarketStockAdapter.updateQuote()` — no-op for missing symbol
    - **Property 12: Adapter No-Op for Missing Symbol**
    - Assert that calling `updateQuote` with a symbol not in the list throws no exception and leaves the list unchanged
    - **Validates: Requirements 6.3**

  - [ ] 6.4 Add shadow list and `updateQuote()` to `ConstituentAdapter`
    - Same pattern as `MarketStockAdapter` but item type is `IndexConstituent`; update `current` and `changePct` fields on `bindLiveUpdate()`
    - Add `fun updateQuote(symbol: String, quote: LiveQuote)` with identical no-op-if-missing behavior
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ] 6.5 Write property test for `ConstituentAdapter.updateQuote()` — match
    - **Property 11: Adapter Targeted Update — Match**
    - Same assertion pattern as 6.2, applied to `ConstituentAdapter`
    - **Validates: Requirements 7.2**

  - [ ] 6.6 Write property test for `ConstituentAdapter.updateQuote()` — no-op for missing symbol
    - **Property 12: Adapter No-Op for Missing Symbol**
    - Same assertion pattern as 6.3, applied to `ConstituentAdapter`
    - **Validates: Requirements 7.3**

- [ ] 7. Checkpoint — Adapters compile and adapter tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Market Dashboard — Fragment and XML layout
  - [ ] 8.1 Add `chipConnectionStatus` TextView to `fragment_market_dashboard.xml`
    - Inside the app bar `ConstraintLayout`, add a `TextView` with `android:id="@+id/chipConnectionStatus"`, `android:background="@drawable/bg_connection_chip"`, `android:visibility="gone"`, `android:clickable="false"`, `android:focusable="false"`
    - Constrain end to start of the notifications icon with 8dp margin; center vertically with the icon
    - _Requirements: 12.1, 12.6_

  - [ ] 8.2 Bind live quote updates and connection chip in `MarketDashboardFragment`
    - Add `private var previousLiveQuotes: Map<String, LiveQuote> = emptyMap()` field
    - In the `Success` branch of `observeUiState()`, call `applyLiveQuoteUpdates(state.liveQuotes)` then `bindConnectionChip(state.wsConnectionState)`, then assign `previousLiveQuotes = state.liveQuotes`
    - `applyLiveQuoteUpdates()` iterates `liveQuotes`, reference-checks against `previousLiveQuotes`, and calls `stockAdapter.updateQuote()` and `constituentAdapter.updateQuote()` only for changed entries
    - `bindConnectionChip()` sets chip text and background per `WsConnectionState`: "Live"/green for `Live`, "Connecting"/amber for `Connecting`/`Reconnecting`, "Delayed"/amber for `Delayed`, `GONE` for `Idle`/`Closed`
    - _Requirements: 8.1, 8.2, 8.3, 12.2, 12.3, 12.4, 12.5, 12.6_

- [ ] 9. Stock Detail — UiState and ViewModel
  - [x] 9.1 Add live price fields to `StockDetailUiState.Success`
    - Add `val livePrice: Double? = null`
    - Add `val liveChangePct: Double? = null`
    - Add `val liveChange: Double? = null`
    - Add `val liveVolume: Long? = null`
    - Add `val isLivePriceStreaming: Boolean = false`
    - Verify all existing `StockDetailUiState.Success(...)` call sites compile without changes
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ] 9.2 Write property test for `StockDetailUiState.Success` default compatibility
    - **Property 13: UiState Default Compatibility**
    - Construct `StockDetailUiState.Success` without new fields; assert all live fields are null/false
    - **Validates: Requirements 9.6**

  - [ ] 9.3 Integrate `LiveMarketRepository` into `StockDetailViewModel`
    - Add `private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository` constructor parameter
    - Expose `val liveConnectionState: StateFlow<WsConnectionState> = liveMarketRepository.connectionState`
    - In `init {}`, launch a coroutine that collects `liveMarketRepository.quoteUpdates`, filters by `quote.symbol.uppercase() == currentSymbol.uppercase()`, and copies `livePrice`, `liveChangePct`, `liveChange`, `liveVolume`, `isLivePriceStreaming = true` into the current `StockDetailUiState.Success`
    - In `loadStock(symbol)`, if `previousSymbol` is non-blank and differs from `symbol`, call `liveMarketRepository.unsubscribe(listOf(previousSymbol))` before the REST fetch; after a successful REST fetch, call `liveMarketRepository.subscribe(listOf(symbol.uppercase()))`
    - In `onCleared()`, if `currentSymbol` is non-blank, call `liveMarketRepository.unsubscribe(listOf(currentSymbol))`
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 13.1, 13.4_

  - [ ] 9.4 Write property test for quote round-trip in `StockDetailViewModel`
    - **Property 3: Quote Emission Round-Trip (Stock Detail)**
    - Emit a `LiveQuote` whose symbol matches `currentSymbol`; assert all five live fields are updated correctly and `isLivePriceStreaming == true`
    - **Validates: Requirements 10.3, 10.4, 14.4**

  - [ ] 9.5 Write property test for source transparency in `StockDetailViewModel`
    - **Property 4: Source Transparency**
    - Emit two `LiveQuote` objects identical except for `source` (WEBSOCKET vs REST_FALLBACK); assert the resulting `StockDetailUiState.Success` live fields are identical
    - **Validates: Requirements 14.4**

  - [ ] 9.6 Write property test for unsubscribe-on-symbol-change
    - **Property 9: Unsubscribe-on-Symbol-Change**
    - Call `loadStock("A")` then `loadStock("B")`; assert `unsubscribe(["A"])` was called before `subscribe(["B"])`
    - **Validates: Requirements 10.5, 13.4**

  - [ ] 9.7 Write property test for unsubscribe-on-clear
    - **Property 10: Unsubscribe-on-Clear**
    - After `loadStock("OGDC")` succeeds, call `onCleared()`; assert `unsubscribe(["OGDC"])` was called
    - **Validates: Requirements 10.6, 13.1**

  - [ ] 9.8 Write property test for subscribe-on-load in `StockDetailViewModel`
    - **Property 8: Subscribe-on-Load (Stock Detail)**
    - Assert `subscribe(listOf(symbol.uppercase()))` is called after `loadStock(symbol)` resolves successfully
    - **Validates: Requirements 10.2**

- [ ] 10. Checkpoint — Stock Detail ViewModel compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Stock Detail — Fragment and XML layout
  - [ ] 11.1 Add `chipConnectionStatus` TextView to `fragment_stock_detail.xml`
    - In the header area, add a `TextView` with `android:id="@+id/chipConnectionStatus"`, `android:background="@drawable/bg_connection_chip"`, `android:visibility="gone"`, `android:clickable="false"`, `android:focusable="false"`
    - Position with `android:layout_marginStart="8dp"` adjacent to `tvTopCode` or within the header row
    - _Requirements: 12.1, 12.6_

  - [ ] 11.2 Apply live overlay and bind connection chip in `StockDetailFragment`
    - In the `Success` branch of `observeUiState()`, after existing bindings, call `applyLiveOverlay(state)`
    - `applyLiveOverlay()` returns early if `!state.isLivePriceStreaming`; otherwise updates `tvPrice`, `tvChangeAmount`, `tvChangePercent`, and `tvVolCurrent` from `livePrice`, `liveChange`, `liveChangePct`, `liveVolume` (each guarded by null-check)
    - Launch an additional coroutine inside `repeatOnLifecycle` to collect `viewModel.liveConnectionState` and call `bindDetailConnectionChip(connState, isStreaming)` where `isStreaming` is read from current `uiState`
    - `bindDetailConnectionChip()` hides chip if `!isStreaming`; otherwise sets text/background per `WsConnectionState` (same mapping as Market Dashboard chip)
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 12.2, 12.3, 12.4, 12.5, 12.6_

- [ ] 12. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at natural boundaries
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The data layer (`LiveMarketDataSource`, `LiveMarketRepositoryImpl`) is complete and must not be modified
- `HomeViewModel` and `MarketDashboardViewModel` are `activityViewModels()` — they must never call `unsubscribe()`
- `StockDetailViewModel` uses `viewModels()` — it must call `unsubscribe()` both on symbol change and in `onCleared()`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "5.1", "9.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "5.2", "5.3", "6.1", "9.2", "9.3"] },
    { "id": 3, "tasks": ["2.4", "2.5", "2.6", "4.1", "5.4", "5.5", "6.2", "6.3", "6.4", "9.4", "9.5", "9.6", "9.7", "9.8"] },
    { "id": 4, "tasks": ["6.5", "6.6", "8.1", "11.1"] },
    { "id": 5, "tasks": ["8.2", "11.2"] }
  ]
}
```
