# Tringapps OTT

Premium OTT streaming app (interview task) powered by JW Player delivery APIs:
config `https://cdn.jwplayer.com/apps/configs/kxqzxtpq.json` -> `content[].contentId` ->
`https://cdn.jwplayer.com/v2/playlists/{id}` -> HLS `.m3u8` source -> Media3 player.

## Android project profile
- Name: Tringapps
- Package: in.sms.tringapps (note: `in` is a Kotlin keyword, write it as `` `in` ``)
- Platform/UI: Jetpack Compose (Android), Material 3, always-dark premium theme
- Module structure: single module (`:app`) with packages `core.{common,network,designsystem,media,ui}`, `catalog.{domain,data,di}`, `library.{domain,data,di}`, `feature.{home,seeall,search,player,intro}`, `navigation`
- Architecture: Clean Architecture + MVVM (presentation -> domain use cases <- data; ViewModels expose `StateFlow<UiState>`, depend on use cases only)
- Database: none. Preferences DataStore for the on-device library (Continue Watching, My List); in-memory page/video cache in the catalog repository
- Paging: Paging 3 (`PlaylistPagingSource`, JW `page_offset` 1-based + `links.next`); rails and See all grid
- Trending Now playlist: `Mye5LqB6` (BuildConfig `JW_TRENDING_PLAYLIST_ID`)
- Networking: Retrofit + kotlinx.serialization (OkHttp)
- DI: Hilt (KSP)
- Images: Coil 3, only via `core/designsystem/component/RemoteImage`
- Video: Media3 ExoPlayer + HLS + `PlayerView` (in `feature/player/presentation/VideoPlayer.kt`)
- Navigation: Navigation Compose type-safe routes (`HomeDestination`, `SeeAllDestination`, `SearchDestination`, `PlayerDestination(mediaId)`)
- Splash: core-splashscreen + Compose `BrandIntro`
- Min SDK / Target SDK: 24 / 37 (compileSdk 37)
- Reference feature: `app/src/main/java/in/sms/tringapps/feature/home`
- Tests: JUnit4, coroutines-test, Turbine, MockWebServer, paging-testing, Robolectric + Compose UI tests in `src/test`
- Verify command: lintDebug testDebugUnitTest
- Rules: follow the `android-dev` skill. Do not add libraries or patterns outside this profile without asking.
