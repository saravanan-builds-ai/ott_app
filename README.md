# Tringapps OTT

[![Android CI](https://github.com/saravanan-builds-ai/ott_app/actions/workflows/android-ci.yml/badge.svg)](https://github.com/saravanan-builds-ai/ott_app/actions/workflows/android-ci.yml)
[![Build APK (manual)](https://github.com/saravanan-builds-ai/ott_app/actions/workflows/android-build.yml/badge.svg)](https://github.com/saravanan-builds-ai/ott_app/actions/workflows/android-build.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3+-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Media3 ExoPlayer](https://img.shields.io/badge/Media3-ExoPlayer%20%2B%20HLS-00C853)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-3DDC84?logo=android&logoColor=white)

A cinema-grade, always-dark Android OTT streaming application powered by the **JW Player Delivery APIs**. Every shelf, hero carousel, ranked Top 10 rail, artwork asset, and adaptive HLS video stream is discovered and rendered dynamically from the live backend at runtime—with **zero hardcoded section IDs, titles, or media assets**.

---

## Table of Contents

- [Experience & Features](#experience--features)
- [Tech Stack](#tech-stack)
- [Quick Start & Local Commands](#quick-start--local-commands)
- [GitHub Actions CI/CD & APK Download](#github-actions-cicd--apk-download)
- [Architecture & Data Flow](#architecture--data-flow)
- [JW Player API Integration](#jw-player-api-integration)
- [Engineering Highlights & Performance](#engineering-highlights--performance)
- [Testing Strategy](#testing-strategy)
- [Known Scope & Trade-offs](#known-scope--trade-offs)

---

## Experience & Features

| Surface | Capabilities |
| :--- | :--- |
| **Brand Intro & Splash** | Android 12+ `SplashScreen` API transitioning seamlessly into an animated Compose brand wordmark while the Home feed prefetches underneath. |
| **Immersive Hero Banner** | Full-bleed 5-item pager driven by the featured playlist. Auto-fades into a silent, SD-capped HLS video preview when settled on screen and pauses immediately when scrolled away or navigated off. |
| **Ranked Top 10 Rail** | Netflix-style outlined gradient numerals tucked behind high-contrast vertical posters from the featured collection. |
| **Dynamic Content Rails** | Horizontally paged shelves built in config order with shimmer skeletons, progressive reveal, and inline retry footers. |
| **Endless Pagination** | Both horizontal rails and the **See all** adaptive grid page on demand with **Paging 3** (`page_limit=25`, `page_offset` 1-based item indexing via JW's `links.next`). |
| **Title Details** | Dedicated cinema sheet/screen with 16:10 backdrop, metadata chips (age rating, formatted duration), **Play now / Resume** with progress bar, **My List** toggle, and full synopsis. |
| **Media3 HLS Player** | Full-screen immersive `ExoPlayer` with adaptive HLS (`.m3u8`) and MP4 fallback, hardware/software decoder fallback, audio-focus management, rotation continuity without rebuffering, and automatic 10s + on-exit watch-progress persistence. |
| **On-Device Library** | **Continue Watching** (with progress bar & instant resume) and **My List**, persisted locally via Jetpack **Preferences DataStore** and surfaced in English whenever declared by the remote config. |
| **Instant Search** | Debounced (300 ms), accent- and case-insensitive search across all playable titles discovered during the session, with curated top picks when the query is empty. |
| **Pull-to-Refresh** | Clears in-memory page/video caches and refreshes the entire catalog while keeping existing content visible; surfaces a non-intrusive snackbar if the network is unreachable. |

---

## Tech Stack

| Layer | Libraries & Tools |
| :--- | :--- |
| **UI & Design System** | Jetpack Compose, Material 3 (Custom Always-Dark Cinema Palette), Edge-to-Edge Window Insets, AndroidX SplashScreen |
| **Navigation** | Navigation Compose (`2.9+`) with Kotlin Serialization type-safe routes |
| **Media Playback** | Jetpack Media3 (`ExoPlayer`, `media3-exoplayer-hls`, `PlayerView`) |
| **Pagination** | Jetpack Paging 3 (`paging-runtime`, `paging-compose`) |
| **Networking & Caching** | Retrofit 3, OkHttp 5 (20 MB HTTP disk cache + shared connection pool), `kotlinx.serialization` |
| **Image Loading** | Coil 3 (`coil-compose`, `coil-network-okhttp` sharing OkHttp pool, 25% RAM cache + 64 MB disk cache) |
| **Local Persistence** | Jetpack DataStore (`datastore-preferences`) |
| **Dependency Injection** | Dagger Hilt (KSP) |
| **Testing** | JUnit 4, `kotlinx-coroutines-test`, Turbine, OkHttp `MockWebServer`, `paging-testing`, Robolectric + Compose UI Test |
| **Build & CI/CD** | AGP `9.x`, Compile/Target SDK `37`, Min SDK `24`, GitHub Actions (`android-ci.yml`, `android-build.yml`) |

---

## Quick Start & Local Commands

### Prerequisites
- **Android Studio** (supporting AGP 9.x)
- **JDK 17+** (JDK 21 recommended for CI parity)
- **Android SDK 37**

### Common Gradle Tasks

```bash
# Build the debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Install the debug APK on a connected device or running emulator
./gradlew installDebug

# Run Android Lint + all 136 Unit & Robolectric Compose UI tests (no emulator required)
./gradlew lintDebug testDebugUnitTest
```

---

## GitHub Actions CI/CD & APK Download

Automated workflows are configured in [`.github/workflows/`](.github/workflows/) with least-privilege permissions (`contents: read`) and Gradle dependency caching (`gradle/actions/setup-gradle@v6`):

1. **[`android-ci.yml`](.github/workflows/android-ci.yml) (`Android CI`)**
   - **Triggers**: Every `push` to `main` and every `pull_request` targeting `main`.
   - **Pipeline**: Checkout → JDK 21 (Temurin) → Gradle Setup & Cache → `./gradlew lintDebug` → `./gradlew testDebugUnitTest` → `./gradlew assembleDebug` → Publish JUnit check report → Upload HTML lint & test reports (7-day retention).
   - **Quality Gate**: Fails immediately if any lint check, unit/UI test, or build step fails.

2. **[`android-build.yml`](.github/workflows/android-build.yml) (`Build APK (manual)`)**
   - **Trigger**: On-demand via `workflow_dispatch`.
   - **Pipeline**: Checkout → JDK 21 → Gradle Cache → `./gradlew testDebugUnitTest` → `./gradlew assembleDebug` → Upload signed debug APK artifact (`tringapps-debug-<sha>-run<n>`, 14-day retention).

### How to Trigger a Manual Build & Download the APK

1. Open the repository on GitHub and navigate to the **Actions** tab.
2. Select **Build APK (manual)** from the left workflow sidebar.
3. Click the **Run workflow** dropdown button on the right, keep the `main` branch selected, and click **Run workflow**.
4. Wait for the **Test & build debug APK** job to finish (~2–4 minutes with a warm Gradle cache).
5. Click into the completed workflow run and scroll down to the **Artifacts** section at the bottom of the run summary page.
6. Click **`tringapps-debug-<sha>-run<n>`** to download the archive, unzip it, and install `app-debug.apk`:
   ```bash
   adb install app-debug.apk
   ```

---

## Architecture & Data Flow

The project follows **Clean Architecture + MVVM** within a single `:app` module, organized by feature and domain boundaries with strict unidirectional data flow (`StateFlow<UiState>`):

```text
in.sms.tringapps
├── core/
│   ├── common/          Cancellation-safe suspendRunCatching, coroutine dispatchers, app scope, clock
│   ├── network/         JwPlayerApi (Retrofit), DTOs, NetworkModule (OkHttp + HTTP disk cache)
│   ├── designsystem/    Cinema dark theme, typography, gradient/glass buttons, chips, shimmer, RemoteImage
│   ├── media/           Video -> Media3 MediaItem mapper, silent SD MutedPreviewPlayer
│   └── ui/              Shared VideoCard (16:9 landscape + progress bar), BackTopBar
├── catalog/             Remote JW Player catalog layer
│   ├── domain/          Models (Section, HomeLayout, Playlist, PlaylistPage, Video, Stream),
│   │                    CatalogRepository interface, UseCases (GetHomeContent, GetPlayablePlaylist,
│   │                    GetPagedPlaylist, GetVideo, SearchCatalog)
│   ├── data/            DTO-to-domain mappers, PlaylistPagingSource (Paging 3), CatalogRepositoryImpl
│   └── di/              Hilt bindings for catalog repository
├── library/             On-device user library (Continue Watching & My List)
│   ├── domain/          ContinueWatchingItem model, LibraryRepository interface, library use cases
│   ├── data/            DataStore-backed LibraryRepositoryImpl & serializable storage models
│   └── di/              Hilt bindings for DataStore & library repository
├── feature/
│   ├── intro/           Cold-start animated BrandIntro overlay
│   ├── home/            Hero carousel, Top 10 rail, Continue Watching / My List rails, paged VideoRails
│   ├── seeall/          Full-screen adaptive vertical grid for any playlist
│   ├── search/          Debounced, accent-insensitive search & session suggestions
│   ├── details/         Title synopsis, metadata chips, Play/Resume CTA, My List toggle
│   └── player/          Full-screen immersive Media3 ExoPlayer & progress persistence
└── navigation/          TringappsNavHost wiring type-safe destinations
```

### Architectural Rules Enforced
- **Zero DTO Leakage**: Retrofit DTOs stay strictly inside `core/network` and `catalog/data`; `presentation` only consumes domain models and use cases.
- **ID-Only Navigation**: Routes pass lightweight identifiers only (`DetailsDestination(mediaId)`, `PlayerDestination(mediaId)`, `SeeAllDestination(playlistId, title)`). `Details` and `Player` resolve the full `Video` and stream URL from the repository's in-memory cache—or fall back to `GET /v2/media/{mediaId}` after process death—keeping the navigation back stack small and process-death safe.

---

## JW Player API Integration

```text
GET /apps/configs/kxqzxtpq.json
  │
  ▼
GetHomeContentUseCase  ──(content[].contentId)──►  GET /v2/playlists/{id}?page_offset=1&page_limit=25
                                                     │
                                                     ├──► links.next  ──► PlaylistPagingSource (Paging 3)
                                                     └──► sources[]   ──► HLS (.m3u8) / MP4 Fallback ──► Media3 ExoPlayer
```

1. **App Config (`GET apps/configs/kxqzxtpq.json`)**
   - Parsed dynamically into `Section` items (`playlistId`, `title`, `type`, `featured`).
   - `type = "playlist"` sections are de-duplicated in config order; the section flagged `featured: true` (or the first loaded playable playlist if the featured feed contains only non-streamable series hubs) powers the **Hero Carousel** and **Top 10** rail.
   - `type = "continue_watching"` and `type = "favorites"` are user-specific collections rendered from on-device DataStore under English UI headers (**Continue Watching** and **My List**).
2. **Playlists (`GET v2/playlists/{playlistId}?page_offset={n}&page_limit=25`)**
   - Maps `mediaid`, `title`, sanitized `description`, `duration`, `agerating`, responsive artwork (`images[]` picking `~640px` for cards and `~1280px` for backdrops), and `sources[]`.
   - **Stream Selection**: Prioritizes adaptive HLS (`application/vnd.apple.mpegurl`, `application/x-mpegurl`, or `.m3u8`). Falls back to the highest-resolution progressive `video/mp4` when HLS is absent. Non-HTTP placeholders (such as `"None"` on series container items) are filtered out automatically.
3. **Deterministic Pagination (`links.next`)**
   - JW's `page_offset` is a 1-based item index. `PlaylistPagingSource` extracts the next offset exclusively from `links.next` (requiring strictly increasing offsets) so the app never guesses or loops infinitely.
   - Page 1 fetched for the Home screen is cached in `CatalogRepositoryImpl` and reused by the rail's `Pager` and the **See all** screen, avoiding duplicate network calls. Duplicate `mediaId`s across pages are filtered out so Compose lazy layouts can use `mediaId` as a stable item key.
4. **Single-Media Fallback (`GET v2/media/{mediaId}`)**
   - Resolves individual titles on demand if the user opens `Details` or `Player` after process death when the in-memory cache is cold.

---

## Engineering Highlights & Performance

- **Language Consistency**: All app-owned chrome, buttons, empty states, error messages, and library headers (**Continue Watching**, **My List**) are strictly in English, while dynamic catalog titles and descriptions from the API are rendered verbatim.
- **Clean Typography & Layout**: Multiline API descriptions containing blank lines (`\n\n`) are normalized into compact single-paragraph previews on cards and the hero banner so 2-line ellipses never cut off on an empty line, and missing/placeholder descriptions (`"..."`, `"N/A"`, `"None"`) are hidden cleanly.
- **Fast Startup & Smooth Scrolling**:
  - Up to 10 playlists are prefetched in parallel via a coroutine `Semaphore`, and the Hero banner promotes the first playable playlist immediately if the designated `featured` feed has no playable streams.
  - Coil 3 and Retrofit share a single `OkHttpClient` connection pool with a **20 MB HTTP cache**, **25% RAM image cache**, and **64 MB disk image cache** for HTTP/2 multiplexing over `cdn.jwplayer.com`.
- **Conflict-Free Audio & Video Lifecycle**:
  - `MutedPreviewPlayer` disables audio track selection (`C.TRACK_TYPE_AUDIO`) and audio focus, and is immediately disposed via `LifecycleStartEffect` when navigating away from `HomeScreen` so it never competes with `VideoPlayer`'s audio decoder or `AudioTrack`.
  - `MainActivity` handles configuration changes directly in Compose so screen rotation during video playback is instantaneous and never rebuffers.

---

## Testing Strategy

All **136 unit and Compose UI tests** run on the local JVM (`./gradlew testDebugUnitTest`) using JUnit 4, Coroutines Test, Turbine, MockWebServer, Paging Testing, and Robolectric—requiring no external emulator:

- **Data Layer**:
  - `CatalogMapperTest` — HLS selection, MP4 resolution fallback, placeholder source filtering, description sanitization, image size selection, and `links.next` `page_offset` extraction.
  - `CatalogRepositoryImplTest` — MockWebServer verification of endpoints, page/video caching, cache invalidation (`clearCache`), and 404/network error handling.
  - `PlaylistPagingSourceTest` — multi-page traversal, end-of-pagination when `links.next` is absent, cross-page de-duplication, and error propagation.
  - `LibraryRepositoryImplTest` — DataStore persistence for Continue Watching progress and My List ordering.
- **Domain Layer**:
  - `CatalogUseCasesTest`, `SearchCatalogUseCaseTest`, `LibraryUseCasesTest` — Home layout assembly, playable-stream filtering, accent-insensitive search ranking, and watch-progress completion thresholds (5%–95%).
- **Presentation & Compose UI Layer**:
  - ViewModel unit tests (`HomeViewModelTest`, `DetailsViewModelTest`, `PlayerViewModelTest`, `SeeAllViewModelTest`, `SearchViewModelTest`).
  - Robolectric Compose UI tests (`HomeScreenTest`, `DetailsScreenTest`, `PlayerScreenTest`, `SeeAllScreenTest`, `SearchScreenTest`, `BrandIntroTest`).

---

## Known Scope & Trade-offs

- **Session-Scoped Search**: This JW Player property does not expose a public search endpoint, so search queries filter across all playable titles discovered during the current session.
- **Local Library**: Continue Watching and My List are stored on-device with Jetpack DataStore (no cloud user account backend).
- **Direct Media3 Playback**: Streams play directly through Jetpack Media3 `ExoPlayer` rather than the proprietary JW Player SDK; server-side JW ad schedules (`csai`) and analytics tokens from the config are intentionally omitted.
