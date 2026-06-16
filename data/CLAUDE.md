# CLAUDE.md

This file provides guidance to Claude Code when working in the `:data` module.

> Module path: `:data` · Build file: `data/data.gradle.kts` · Namespace: `mega.privacy.android.data`

## Overview
`:data` is the data layer of the project's clean architecture. It provides concrete implementations of the repository interfaces declared in `:domain`, sourcing data from the MEGA SDK (via gateways/facades), the Room/SQLCipher database, DataStore preferences, the file system, and network/Firebase services. It is the only module that talks to the prebuilt MEGA SDK directly; everything above it depends on `:domain` abstractions.

This is a large module (~1000+ Kotlin files). Do not attempt to read it exhaustively — use `grep`/`find` to locate the specific repository, gateway, or mapper you need.

## Architecture & Layout
Source root: `data/src/main/java/mega/privacy/android/data/`

- `repository/` — `*RepositoryImpl` / `Default*Repository` implementations of `:domain` interfaces (also organized into feature subpackages: `chat/`, `account/`, `photos/`, `files/`, `security/`, etc.).
- `gateway/` — interfaces abstracting SDK and platform access (e.g. `gateway/api/MegaApiGateway`, `MegaChatApiGateway`, `MegaApiFolderGateway`, `StreamingGateway`, plus `FileGateway`, `CacheGateway`, `MegaLocalRoomGateway`, `MegaLocalStorageGateway`).
- `facade/` — concrete gateway implementations wrapping the SDK / Android APIs (e.g. `MegaApiFacade`, `MegaChatApiFacade`, `FileFacade`, `MegaLocalRoomFacade`).
- `mapper/` — entity/model mappers, grouped by domain (`node/`, `transfer/`, `chat/`, `account/`, `node/`, `meeting/`, etc.).
- `database/` — Room database (`MegaDatabase`), `database/dao/` (~24 DAOs), `database/entity/` (~24 entities), `converter/`, plus SQLCipher integration (`SQLCipherManager`, `MegaOpenHelperFactor`) and legacy migration (`DatabaseHandler`, `LegacyDatabaseMigration`).
- `preferences/` — DataStore-backed preference stores (with `base/`, `security/`, `migration/`).
- `di/` — Hilt modules wiring everything together.
- Supporting packages: `cryptography/`, `cache/`, `listener/` (SDK request/transfer listeners), `worker/` (WorkManager workers), `logging/`, `featuretoggle/`, `wrapper/`, `model/`.

## Key Components
- **Repository implementations**: implement `:domain` repository interfaces; named `Default{Feature}Repository` or `{Feature}RepositoryImpl` (e.g. `DefaultContactsRepository`, `ChatRepositoryImpl`, `CameraUploadsRepositoryImpl`). They depend on gateways + mappers, not on the SDK directly.
- **Gateways**: interfaces under `gateway/` (notably `gateway/api/MegaApiGateway`, `MegaChatApiGateway`) abstract the prebuilt MEGA SDK and platform APIs; their implementations live in `facade/` (e.g. `MegaApiFacade` implements `MegaApiGateway`). Async SDK callbacks are bridged via listeners in `listener/`.
- **Mappers**: classes with `@Inject` constructor and `operator fun invoke`, converting SDK/Room types <-> `:domain` entities (e.g. mappers under `mapper/transfer/`, `mapper/node/`).
- **Database**: Room (`MegaDatabase`) encrypted with SQLCipher (`SQLCipherManager`); DAOs and entities in `database/dao` / `database/entity`; schemas exported under `data/schemas`. Sensitive fields are encrypted/decrypted in mappers via `cryptography/EncryptData` and `DecryptData`.
- **DI**: Hilt modules in `di/` (`RepositoryModule`, `GatewayModule`, `MapperModule`, `RoomDatabaseModule`, `DataStoreModule`, `WorkerModule`, etc.) provide `@Binds`/`@Provides` bindings, all `@InstallIn(SingletonComponent::class)`.

## Module Dependencies
- `:domain` (repository interfaces + entities), `:core:coroutine`, `:shared:sync`, `:third-party-lib:pdfiumAndroid` (api).
- Prebuilt MEGA SDK via `preBuiltSdkDependency(rootProject.extra)`.
- Convention plugins: `mega.android.library`, `mega.android.room`, `mega.android.hilt`; Kotlin serialisation plugin.
- Notable libs: SQLCipher, AndroidX DataStore/WorkManager/Paging/Security-Crypto, Tink, Gson, kotlinx-serialization, Firebase Performance, Guava, protobuf.
- Tests: `:core-test`.

## Testing
JUnit 5 + Mockito + Turbine + Truth, `runTest` for coroutines. Tests live under `data/src/test/`. Run:
`./gradlew data:testDebugUnitTest`

## Notes & Gotchas
- Lint is strict here: `abortOnError = true` and `warningsAsErrors = true`.
- Never bypass the gateway/facade abstraction to call the SDK from a repository — always go through a gateway interface.
- Encrypt sensitive fields with `EncryptData` (and `DecryptData`) when persisting to the database; mappers commonly return null when encryption fails.
- Room schemas are exported to `data/schemas`; update/verify them when changing entities or migrations.
- Global conventions (naming, DI, mapper/repository patterns) live in the root `.claude/CLAUDE.md` — follow them; this file only covers `:data` specifics.
