# Tringapps OTT

A premium, Netflix-style Android streaming app powered by the **JW Player delivery APIs**.

## Features

- **Dynamic home** built from the JW app config (`apps/configs/kxqzxtpq.json`): every `playlist` section becomes a horizontally scrolling rail.
- **Paging 3** on every rail and on a **See all** grid, using JW's `page_offset` (1-based item index) / `page_limit` and `links.next`.
- **Trending Now Top 10**: ranked rail with oversized outlined numerals (playlist `Mye5LqB6`).
- **Hero carousel** with a **muted HLS preview** that fades in after the artwork settles, then auto-advances.
- **Continue Watching** (with progress bars and resume) and **My List**, stored on device with DataStore.
- **Search**: debounced, accent-insensitive, across everything loaded in the session.
- **Player**: Media3 ExoPlayer, HLS `.m3u8` preferred (MP4 fallback), immersive full-screen, survives rotation, resumes after errors and process death.
- **Splash + animated brand intro**, custom adaptive icon, always-dark premium theme.

## Architecture

Single `:app` module, Clean Architecture + MVVM.

```
core/        common (dispatchers, scopes), network (Retrofit + DTOs), designsystem, media, ui
catalog/     domain (models, repository, use cases) · data (mapper, paging source, repository) · di
library/     on-device Continue Watching / My List (domain · data · di)
feature/     home · seeall · search · player · intro   (presentation + navigation)
navigation/  NavHost with type-safe routes
```

Stack: Kotlin, Jetpack Compose (Material 3), Hilt, Retrofit + kotlinx.serialization, Paging 3, DataStore, Coil 3, Media3, Navigation Compose.

## Build & test

```bash
./gradlew assembleDebug
./gradlew lintDebug testDebugUnitTest   # unit + Robolectric Compose UI tests, no emulator needed
```

Requires JDK 17+ and Android SDK 37.
