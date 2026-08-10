# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:sign-in-external` module.

> Module path: `:feature:sign-in-external` · Build file: `feature/sign-in-external/sign-in-external.gradle.kts` · Namespace: `mega.privacy.android.feature.signin.external`

## Overview
This module provides external (third-party) sign-in for the app. Today it implements **Google Sign-In** via AndroidX Credential Manager + the Google Identity `googleid` library. It exposes a reusable Compose button and a launcher that drives the Credential Manager flow, retrieves a Google ID token (JWT), and maps it into a domain `GoogleSignInResult`.

The module is intentionally small and UI-focused: it owns the platform integration (Credential Manager picker, ID token retrieval, JWT decoding) and binds a `GoogleSignInRepository` implementation, but the broader login/account orchestration lives in `:domain` and the consuming login feature.

## Architecture & Layout
Standard Clean Architecture split, all under `mega.privacy.android.feature.signin.external`:
- `data/mapper/login/` — `GoogleIdTokenMapper` (JWT payload → domain entity)
- `data/repository/security/` — `DefaultGoogleSignInRepository` (implements the domain interface)
- `di/` — `GoogleSignInModule` (Hilt `@Binds`)
- `ui/` — Compose button + launcher + Credential Manager extension function

The repository interface (`GoogleSignInRepository`), entity (`GoogleSignInResult`), and exceptions (`GoogleSignInException`) all live in `:domain`, not here.

## Key Components
- **ViewModels**: None.
- **Use Cases**: None (use cases live in `:domain`).
- **Repositories / Gateways / Data sources**:
  - `DefaultGoogleSignInRepository` — `internal @Singleton`, implements `domain...GoogleSignInRepository`; delegates token decoding to `GoogleIdTokenMapper` and wraps failures as `GoogleSignInException.Unknown`.
  - `GoogleIdTokenMapper` — `internal`, decodes the base64url JWT payload (no signature verification — Play Services already verified it) and extracts `email`/`sub`/`given_name`/`family_name` into `GoogleSignInResult`.
- **Navigation**: None (no NavKey/Destination; consumed by embedding the UI directly).
- **UI** (`ui/`):
  - `GoogleSignInButton` + `GoogleSignInButtonPlaceholder` — Compose button (shimmer placeholder while the feature flag resolves); built on `MegaOutlinedButton`. Test tags exposed as `GOOGLE_SIGN_IN_BUTTON_TAG` / `GOOGLE_SIGN_IN_PLACEHOLDER_TAG`.
  - `rememberGoogleSignInLauncher(onIdToken, onError)` → `GoogleSignInLauncher` — stable, invokable handle that launches the flow; no-op when `LocalActivity` is null, silently swallows `GoogleSignInException.Cancelled`.
  - `Activity.getGoogleIdToken()` — `internal suspend` extension; runs the Credential Manager `GetSignInWithGoogleOption` request and maps Credential Manager exceptions to `GoogleSignInException` (`Cancelled` / `NoCredential` / `Unknown`).

## Module Dependencies
- Project: `:domain`, `:data`, `:core:feature-flags`, `:resources:string-resources`, `:resources:icon-pack`, `:core:ui-components:shared-components`, `lib.mega.core.ui`; `lintChecks(:lint)`.
- External: AndroidX Credential Manager (`androidx.credentials`, `androidx.credentials.play`), Google Identity (`google.identity.googleid`), Compose BOM + activity + Material3, Timber, compose-state-events, kotlinx-serialization.
- Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, kotlin-serialisation.

## Testing
JUnit5 + Mockito + Turbine + Truth (per root conventions). Existing tests cover `GoogleIdTokenMapper` and `DefaultGoogleSignInRepository`.
Run: `./gradlew feature:sign-in-external:testDebugUnitTest`

## Notes & Gotchas
- **OAuth client ID**: `BuildConfig.GOOGLE_SERVER_CLIENT_ID` is read from `local.properties` (`googleServerClientId=...apps.googleusercontent.com`) at build time and falls back to `placeholder.apps.googleusercontent.com`. Sign-in will not work until a real Web client ID is configured locally; the key is never committed. `buildConfig = true` is enabled for this.
- The JWT is **not** signature-verified here by design — Play Services / Credential Manager already validated it. The mapper only decodes and extracts claims.
- Cancellation is treated as a non-error: the launcher swallows `GoogleSignInException.Cancelled` and does not call `onError`.
- The launcher requires a live `LocalActivity` (Credential Manager needs an activity to host the picker); it is a silent no-op otherwise.
- See root `.claude/CLAUDE.md` for global architecture, naming, and testing rules.
