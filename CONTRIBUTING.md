# Contributing

Thanks for your interest in improving the TestingBot Gradle Plugin! 🎉

## Getting started

1. Fork and clone the repository.
2. Use JDK 17 or newer.
3. Build and run the tests:

   ```bash
   ./gradlew build
   ```

   This compiles the plugin and runs both the unit tests (`src/test`) and the TestKit
   functional tests (`src/functionalTest`).

## Development notes

- The plugin is written in **Kotlin** and uses the lazy Gradle `Property`/`Provider` API so
  it stays configuration-cache compatible. Keep task actions free of `Project` references.
- HTTP is done with `java.net.HttpURLConnection` (see `internal/TestingBotClient.kt`) to
  avoid adding HTTP dependencies to consumers' build classpaths. The only runtime dependency
  is Gson.
- The functional tests run a real in-process mock of the TestingBot API
  (`com.sun.net.httpserver.HttpServer`) and drive the plugin through `GradleRunner`. Add
  coverage there for new tasks or behaviors.

## Pull requests

- Keep changes focused; include tests for new behavior.
- Update `README.md` and `CHANGELOG.md` (under `## [Unreleased]`) when user-facing behavior
  changes.
- Make sure `./gradlew build` passes before opening the PR. CI runs the build on JDK 17 and
  21, plus CodeQL and dependency review.
- By contributing, you agree that your contributions are licensed under the project's
  [MIT License](LICENSE).

## Releasing (maintainers)

1. Bump `VERSION_NAME` in `gradle.properties` and update `CHANGELOG.md`.
2. Commit, then push a tag `v<version>` (e.g. `v0.1.0`). The `publish` workflow triggers on
   the tag: it checks the tag matches `VERSION_NAME`, publishes to the Gradle Plugin Portal
   and Maven Central, and creates a GitHub Release with the built artifacts.
3. Required secrets (the Maven Central + GPG ones are shared with `testingbot-java`, so they
   are typically configured at the organization level):
   - `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET` — Gradle Plugin Portal API key/secret.
   - `MAVEN_USERNAME`, `MAVEN_PASSWORD` — Sonatype Central Portal user token.
   - `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` — ASCII-armored signing key and its passphrase.
