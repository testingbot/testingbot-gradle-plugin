# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-07-01

### Added
- `testingbotUpload` task — upload an APK to TestingBot Storage and capture the
  `tb://<appkey>` URL.
- `testingbotEspresso` task — upload app + test APK, start an Espresso run per device
  capability, poll for completion, download the JUnit report, and fail the build on
  failures.
- `testingbotDevices` task — list available Android devices.
- `testingbot { }` DSL with credential resolution (DSL → Gradle property → environment),
  free-form capabilities, and optional Android Gradle Plugin auto-wiring.

[Unreleased]: https://github.com/testingbot/testingbot-gradle-plugin/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/testingbot/testingbot-gradle-plugin/releases/tag/v0.1.0
