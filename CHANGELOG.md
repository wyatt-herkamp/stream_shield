# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [0.1.2] (UNRELEASED)

### Fixed

- The secondary window no longer fails to open with "AWT already initialized in headless
  mode". Minecraft's `Main` class sets `java.awt.headless=true` from a static initializer,
  so `-Djava.awt.headless=false` on the command line was always overwritten, and whichever
  code touched AWT first cached that decision permanently. Stream Shield now settles AWT's
  headless mode from a `preLaunch` entrypoint, before Minecraft's `Main` class is loaded, so
  the `--add-opens=java.desktop/java.awt=ALL-UNNAMED` workaround is no longer needed.
- Querying displays for the settings screen no longer throws when AWT cannot reach a
  display server.

## [0.1.1] (2026-06-27)

- 26.2

## [0.1.0] (2026-05-23)

Initial release.
