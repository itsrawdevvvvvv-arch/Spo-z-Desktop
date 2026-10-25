# Spo~z Desktop

A native Windows desktop music player — the desktop sibling of the Spo~z Android app.
Search YouTube Music and stream it, with a queue and a now-playing bar. No emulator,
no APK: a real `.exe` / `.msi` installer.

## Download

Grab the latest installer from the [**Releases**](../../releases) page:
`Spoz-1.0.0.msi` (recommended) or `Spoz-1.0.0.exe`.

> The installer is **not code-signed**, so Windows SmartScreen will show
> "Windows protected your PC" on first run. Click **More info → Run anyway**.
> Code-signing certificates cost money; this is a free build.

## Build it yourself

```bash
./gradlew packageMsi      # Windows installer
./gradlew run             # run without packaging
```

Requires JDK 21. The Windows installer can only be produced on Windows — that is what
the CI workflow uses.

## How it works

- **Search** — YouTube Music's InnerTube API (`music.youtube.com/youtubei/v1/search`),
  using the `WEB_REMIX` client, parsed as JSON.
- **Stream** — the same API's `player` endpoint, using the `IOS` client, which returns
  a directly playable audio URL.
- **Audio** — JavaFX Media. Its native decoders are bundled inside the jars, so the
  installed app needs nothing else on the machine.
- **UI** — Kotlin + Compose Multiplatform, so it can share the phone app's look.

## Legal

GPL-3.0. This is a client for YouTube Music; use it in line with YouTube's terms.
