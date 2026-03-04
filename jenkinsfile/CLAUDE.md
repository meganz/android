# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This directory contains Jenkins CI/CD pipeline scripts (Groovy) for the MEGA Android app. These scripts run on Jenkins and are not executed locally. All pipelines are designed to run on Mac Jenkins agents with a specific toolchain at `/opt/buildtools/`.

## File Roles

| File | Purpose |
|------|---------|
| `common.groovy` | Shared utility library loaded by all other pipelines via `load('jenkinsfile/common.groovy')` |
| `android_build_status.groovy` | CI pipeline — runs on every MR push; builds GMS+QA APKs, unit tests with coverage, and lint in parallel |
| `android_upload.groovy` | CD pipeline — triggered by MR comments (`deliver_qa`, `publish_sdk`, `upload_coverage`) or push to develop |
| `android_release.groovy` | CD release pipeline — triggered by MR comments on `release/*` branches (`deliver_appStore`, `upload_symbol`, etc.) |
| `android_release_internal.groovy` | Scheduled daily CD — builds and uploads to Google Play Internal + Firebase |
| `android_release_internal_app_sharing.groovy` | CD pipeline — triggered by `deliver_internalAppSharing` MR comment |
| `android_promote.groovy` | Manual promotion pipeline — promotes builds between Play Store tracks (Alpha→Beta→Production) |
| `android_lint_and_warning.groovy` | Weekly lint + build warnings report pipeline, posts to Slack |
| `download_webrtc.sh` | Helper script to download/cache WebRTC libraries from Artifactory |
| `default_release_notes.json` | Default release notes for internal builds |

## Architecture

### Pipeline trigger pattern

All pipelines check trigger conditions using `env.gitlabTriggerPhrase` and `env.gitlabActionType`. Stages are guarded by `when { expression { ... } }` blocks that call `triggered*()` helper functions. This allows a single pipeline script to handle multiple commands.

### common.groovy usage

Every pipeline loads `common.groovy` as a shared library:
```groovy
common = load('jenkinsfile/common.groovy')
```
`common.groovy` must end with `return this` so it can be loaded as an object.

### Shared library (`jenkins-android-shared-lib`)

Some pipelines also use `@Library('jenkins-android-shared-lib') _` which provides a `util` global object with methods like `util.useArtifactory()`, `util.useGitLab()`, `util.useGpg()`, `util.failPipeline()`.

### Version code scheme

`APK_VERSION_CODE_FOR_CD` is generated as `new Date().format('yyDDDHHmm', TimeZone.getTimeZone("GMT"))`. Version names come from Gradle tasks (`printAppVersionName`, `printAppVersionNameChannel`, `printAppGitHash`).

### Key Gradle tasks invoked by pipelines

- `./gradlew app:assembleGmsDebug` — debug APK
- `./gradlew app:assembleGmsRelease` — release APK
- `./gradlew app:assembleGmsQa` — QA APK
- `./gradlew app:bundleGmsRelease` — release AAB
- `./gradlew lint` — lint all modules (uses `lint.xml` renamed from `custom_lint.xml`)
- `./gradlew runAllUnitTestsWithCoverage` — all unit tests with coverage
- `./gradlew collectCoverage --modules "..." --html-output coverage.html` — coverage report
- `./gradlew generateLintReport --lint-results <file> --target-file <file>` — parse lint XML to JSON
- `./gradlew printSubprojectPaths` — list all module paths
- `./gradlew printModulesWithUnitTest` — list modules with unit tests
- `./gradlew sdk:artifactoryPublish` — publish prebuilt SDK AAR to Artifactory

### SDK submodules

The SDK lives at `sdk/src/main/jni/mega/sdk` and MEGAchat at `sdk/src/main/jni/megachat/sdk`. Both are git submodules. The `publish_sdk` command in `android_upload.groovy` builds the native SDK from source using `sdk/src/main/jni/build.sh`.

### Artifact storage

All build artifacts are uploaded to an internal Artifactory instance (`env.ARTIFACTORY_BASE_URL`). Pipeline logs, lint reports, unit test results, APKs, and AABs all go to different paths under `android-mega/`.

### MR communication

Results are posted back to GitLab MR comments via `common.sendToMR()` which calls the GitLab API (`/api/v4/projects/199/merge_requests/{mrNumber}/notes`). Slack notifications use the `slackSend` Jenkins plugin.

### Build environment (Mac agents)

```
NDK_ROOT = /opt/buildtools/android-sdk/ndk/27.1.12297006
JAVA_HOME = /opt/buildtools/zulu21.40.17-ca-jdk21.0.6-macosx
ANDROID_HOME = /opt/buildtools/android-sdk
```
