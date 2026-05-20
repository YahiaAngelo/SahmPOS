# SahmPOS

A Kotlin Multiplatform Point-of-Sale (POS) reference app targeting **Android** and **iOS**, with shared UI built on **Compose Multiplatform**. It demonstrates a clean, layered architecture for retail workflows: catalog browsing, cart management, simulated card-terminal checkout, receipt printing, barcode scanning, and offline-first order synchronisation with retries.

The app is fully functional against a built-in mock backend (Ktor `MockEngine`), so it runs end-to-end on a fresh checkout without any external services.

---

## Modules & Targets

| Target | Source set | Notes |
|---|---|---|
| Android | `composeApp/src/androidMain` | Real camera + ML Kit barcode scanner, Android `PrintManager` receipt printing |
| iOS (arm64 + simulator arm64) | `composeApp/src/iosMain` | SQLDelight Native driver, Ktor Darwin engine |
| Shared | `composeApp/src/commonMain` | All domain logic, UI, persistence DSL, sync, DI |

Entry points:
- Android: `MainActivity` → `SahmApp` (Application) initialises Koin → `App()`
- iOS: `MainViewController.kt` → initialises Koin once → `App()`

---

## Architecture

The codebase follows a pragmatic **Clean Architecture** layout with three concentric layers in `commonMain`:

```
presentation/   ← Compose UI + ViewModels (StateFlow-driven)
    │
domain/         ← Pure Kotlin: models, repository interfaces, use cases, hardware interfaces
    │
data/           ← Implementations: SQLDelight repositories, Ktor sync, outbox, mock server
hardware/       ← Concrete hardware impls (terminal, printer, scanner, renderer)
di/             ← Koin modules (common + expect/actual platformModule)
```

Layer rules:
- `domain` depends on nothing except Kotlin/coroutines/datetime.
- `data` and `hardware` depend on `domain` and implement its interfaces.
- `presentation` depends on `domain` (and `data` only via DI bindings).
- Platform-specific code lives behind `expect`/`actual` (`Platform`, `DatabaseDriverFactory`, `platformModule`, hardware Compose helpers).

### High-level flow

```
        ┌──────────────────────────────────────────────────────────┐
        │                  Compose UI (App.kt)                     │
        │     PosScreen │ OrdersScreen │ SyncScreen                │
        └──────────────┬───────────────────────────────────────────┘
                       │ StateFlow
        ┌──────────────▼───────────────┐
        │  ViewModels (Koin-injected)  │
        │  PosVM │ OrdersVM │ SyncVM   │
        └──────┬─────────────────┬─────┘
               │ UseCases        │
        ┌──────▼─────────────────▼─────┐      ┌──────────────────────┐
        │ Repositories (interfaces)    │◄────►│ Hardware interfaces  │
        │ Product / Order / Sync       │      │ Scanner/Terminal/    │
        └──────┬───────────────────────┘      │ Printer/Renderer     │
               │                              └──────────┬───────────┘
        ┌──────▼───────────┐  ┌────────────────┐         │
        │ SQLDelight DB    │  │ Sync outbox +  │   ┌─────▼────────────┐
        │ (Orders, Items,  │  │ Ktor SyncApi   │──►│ MockSyncServer   │
        │  Products,       │  │ + SyncManager  │   │ (Ktor MockEngine)│
        │  Payment, Outbox)│  └────────────────┘   └──────────────────┘
        └──────────────────┘
```

---

## Key Architectural Decisions

### 1. Offline-first with an Outbox
Orders are saved locally **before** any network attempt. Each checkout enqueues a row in `sync_outbox` (`commonMain/sqldelight/.../SyncOutbox.sq`).

- `SyncRepositoryImpl.processOnce()` pulls due rows, marks them `IN_FLIGHT`, pushes to `SyncApi`, then transitions to `DONE` / back to `PENDING` (with exponential backoff) / `FAILED` (terminal).
- `SyncManager` runs a coroutine loop that drains the outbox every 5s.
- Backoff: `base 1s, exp doubling, cap 60s, max 6 attempts → marked dead`.
- Conflicts (409) follow a **server-wins** policy: the local order is marked synced at the server's version.

This decouples UX latency from network reliability and means the app is fully usable offline.

### 2. Hardware behind interfaces, simulated for portability
`domain/hardware/Hardware.kt` defines `BarcodeScanner`, `PaymentTerminal`, `ReceiptPrinter`, `ReceiptRenderer`. Implementations in `hardware/` are deliberately deterministic-ish simulations so the app runs anywhere:

- `PaymentTerminalImpl` emits a realistic `Flow<PaymentEvent>` (`AwaitingCard → Reading → Authorizing → Approved/Declined/Timeout`) with configurable rates.
- `BarcodeScannerImpl` exposes a shared `MutableSharedFlow<String>` — the camera (Android) feeds it via ML Kit; tests/UI can also `emit()` directly.
- `ReceiptPrinterImpl` keeps in-memory history; Android `SystemHardware.android.kt` provides an actual `PrintManager`-based PDF render for physical printing.

Swapping in a real card reader or thermal printer is a single DI binding change.

### 3. Money as integer cents
`domain/model/Money.kt` represents amounts in integer cents to avoid float rounding. All arithmetic (line totals, tax, discounts, totals) stays in `Long`.

### 4. Compose Multiplatform UI with `expect`/`actual` for native pieces
The UI tree is fully shared. The only platform-conditional UI surfaces are declared in `presentation/hardware/SystemHardware.kt`:

- `CameraQrScanner(...)` — Android uses CameraX + ML Kit; iOS provides a stub.
- `rememberReceiptPrintAction(...)` — Android renders a multi-page PDF via `PrintManager`; iOS stub is a no-op.

### 5. Dependency Injection with Koin
A single `commonModule` (`di/Modules.kt`) wires the entire app; an `expect val platformModule` adds platform-specific bindings (e.g. `DatabaseDriverFactory`). The app entry points call `initKoin { androidContext(...) }` on Android or `initKoin()` on iOS.

Repositories and hardware are singletons (so state — like the scanner's `SharedFlow` and the printer's history — is shared). Use cases are factories. ViewModels are registered via `viewModelOf` for `koin-compose-viewmodel`.

### 6. SQLDelight as the single source of truth
Schemas live in `commonMain/sqldelight/.../db/`:
- `Product.sq` — catalog
- `Orders.sq` — orders + order items (cascade delete, indexed on `sync_state`)
- `Payment.sq` — payment records (one per order)
- `SyncOutbox.sq` — outbox rows with status / attempts / backoff

Reactive reads are exposed as `Flow<List<…>>` via `asFlow().mapToList(ioDispatcher)`.

### 7. ViewModels expose immutable `StateFlow<UiState>`
e.g. `PosUiState(catalog, cart, checkout, lastReceipt, toast)`. UI is a pure function of state; all mutations go through ViewModel methods. `CheckoutPhase` is a sealed hierarchy so the UI renders the terminal handshake (`AwaitingCard → Reading → Authorizing → Approved/Declined`) in real time.

---

## Package Layout (shared code)

```
io.github.yahiaangelo.sahmpos/
├── App.kt                          ← root Composable with bottom nav
├── Platform.kt                     ← expect Platform (per-target build info)
├── di/
│   └── Modules.kt                  ← commonModule + expect platformModule
├── domain/
│   ├── model/                      ← Money, Product, Cart, Order, Payment, Receipt, SyncStatus
│   ├── repository/Repositories.kt  ← Product/Order/Sync repository interfaces
│   ├── hardware/Hardware.kt        ← Scanner/Terminal/Printer/Renderer interfaces + PaymentEvent
│   ├── usecase/UseCases.kt         ← ObserveCatalog, ObserveOrders, ScanBarcode, CheckoutOrder
│   └── util/Ids.kt                 ← prefix-tagged id generator
├── data/
│   ├── local/
│   │   ├── DatabaseDriverFactory.kt        ← expect class
│   │   └── db/ (generated SQLDelight)
│   ├── remote/
│   │   ├── Dtos.kt                 ← network DTOs + toDto() mappers
│   │   ├── SyncApi.kt              ← Ktor client wrapping PushResult sealed
│   │   └── MockSyncServer.kt       ← Ktor MockEngine; configurable failure/conflict rates
│   ├── repository/                 ← Product/Order repository implementations
│   └── sync/
│       ├── SyncRepositoryImpl.kt   ← outbox processor with backoff
│       └── SyncManager.kt          ← periodic drain loop
├── hardware/                       ← Scanner/Terminal/Printer/Renderer implementations
└── presentation/
    ├── theme/Theme.kt              ← Material 3 + Material Icons
    ├── hardware/SystemHardware.kt  ← expect Composables: CameraQrScanner, print action
    ├── pos/                        ← PosScreen, PosViewModel (Idle → Processing → Done)
    ├── orders/                     ← OrdersScreen, OrdersViewModel
    └── sync/                       ← SyncScreen, SyncViewModel (live status + retry)
```

---

## Domain Model Summary

- **Money** — integer cents, with `+ - *` operators and `Money.ZERO`.
- **Product** — `id`, `name`, `barcode`, `price`, `taxRate`.
- **Cart / CartLine** — immutable, computes `subtotal`, `tax`, `total`, `itemCount`.
- **Order / OrderItem** — persisted entity with `status`, `syncState`, `version`.
- **Payment / PaymentMethod / PaymentStatus** — captured per order.
- **Receipt** — order + payment + rendered text (printable).
- **SyncState** — `PENDING | SYNCED | FAILED` (per order).
- **SyncStatusSummary** — counts of outbox rows by status, driven by `countByStatus`.

---

## Checkout Flow

`CheckoutOrderUseCase` (`domain/usecase/UseCases.kt`) is the single transactional pipeline:

1. Refuse empty cart.
2. Stream `PaymentTerminal.charge(total, method)` events through a UI callback.
3. Wait for a terminal event (`Approved` / `Declined` / `Timeout`).
4. Build `Order` + `Payment` with cents-precise totals and `SyncState.PENDING`.
5. Persist atomically via `OrderRepository.save(order, payment)`.
6. Enqueue the order in the sync outbox.
7. Render a receipt string and hand it to the printer.

The same use case works on Android and iOS — only the terminal/printer/scanner implementations differ.

---

## Sync Protocol (Mock)

`SyncApi.pushOrder(OrderDto): PushResult` returns one of:

- `Accepted(version)` — outbox row → `DONE`, order → `SYNCED`
- `Conflict(serverVersion)` — server-wins, order → `SYNCED` at server version
- `RetryableError(code, message)` — outbox row → `PENDING` with exponential backoff
- `FatalError(code, message)` — outbox row → `FAILED` (dead), order → `FAILED`

`MockSyncServer` rolls a configurable RNG (defaults: `15%` failure, `10%` conflict) so the Sync screen demonstrates realistic retry/conflict behaviour out of the box.

---

## Building & Running

### Android
```bash
./gradlew :composeApp:assembleDebug
# or
./gradlew :composeApp:installDebug
```
Camera permission is declared in `AndroidManifest.xml`; the in-app scanner requests it at runtime.

### iOS
Open `iosApp/` in Xcode and run, **or** use the Kotlin Multiplatform Mobile plugin's run configuration in IntelliJ / Android Studio. The shared framework is exposed as `ComposeApp` (static).

### Tests
```bash
./gradlew :composeApp:allTests
```

---

## Tech Stack

| Concern | Library |
|---|---|
| UI | Compose Multiplatform (Material 3 + Material Icons Extended) |
| State / lifecycle | AndroidX Lifecycle ViewModel (multiplatform) |
| DI | Koin (`core`, `compose`, `compose-viewmodel`, `android`) |
| Persistence | SQLDelight (Android + Native drivers) |
| Networking | Ktor client (OkHttp on Android, Darwin on iOS, MockEngine in shared code) |
| Serialization | `kotlinx.serialization.json` |
| Concurrency | `kotlinx.coroutines` (Flow / StateFlow / SharedFlow) |
| Time / IDs | `kotlinx.datetime` + custom prefix-tagged ID generator |
| Camera / Barcode (Android) | CameraX + ML Kit Barcode Scanning |
| Printing (Android) | `android.print.PrintManager` + `PrintedPdfDocument` |

Plugin and version definitions live in `gradle/libs.versions.toml`.

---

## Extending the App

- **Real backend**: replace `MockSyncServer` with a Ktor client pointing at a real URL. Keep `SyncApi`'s sealed `PushResult` contract intact and the outbox keeps working.
- **Real card terminal**: implement `PaymentTerminal` for the SDK and replace the binding in `di/Modules.kt`.
- **Thermal printer**: implement `ReceiptPrinter` (and optionally a platform-specific `rememberReceiptPrintAction`). `ReceiptRenderer` already produces a monospace receipt string suitable for ESC/POS-style output.
- **Inventory / customers / multi-tenant**: add SQLDelight schemas in `commonMain/sqldelight/...`, a domain repository interface, and an implementation + DI binding — UI changes stay isolated to a new screen + ViewModel.
- **New target (Desktop / Web)**: add a source set, provide `actual` for `Platform`, `DatabaseDriverFactory`, `platformModule`, and the `SystemHardware` Composables.
