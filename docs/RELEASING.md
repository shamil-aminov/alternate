# Releasing

## One-time setup

Generate a keystore. Keep it and its passwords out of the repository — losing
it means never being able to update the app for anyone who installed it.

```bash
keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alternate
```

`keytool` ships with the JDK and is rarely on `PATH` on Windows. Android
Studio's bundled runtime has it:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alternate
```

Add four repository secrets on GitHub (Settings → Secrets and variables →
Actions):

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 release.jks`, or on PowerShell `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))` |
| `KEYSTORE_PASSWORD` | the store password |
| `KEY_ALIAS` | `alternate` |
| `KEY_PASSWORD` | the key password |

Without them the release still compiles, but the APK is unsigned — fine for a
dry run, and the shape F-Droid builds from, but not installable as-is.

## Cutting a release

```bash
git tag -a v1.0.0 -m "1.0.0"
git push origin v1.0.0
```

The tag triggers `.github/workflows/release.yml`: it runs the tests, builds a
signed release APK, names it after the tag and publishes it as a GitHub
release. The keystore is written to disk only for the length of that job and
deleted afterwards.

## What signing does and does not fix

Signing identifies the author and lets one build update another. It does **not**
remove Android's "unsafe app" warning: that warning is about where the file
came from, not about how it was signed. Any APK installed from outside a store
gets it. It goes away when the app is installed from a store the phone already
trusts.

## F-Droid

F-Droid is free and fits this app: MIT licensed, no proprietary dependencies,
no network access, no analytics. It builds from source on its own infrastructure
and signs with its own key, so an F-Droid install and a GitHub-release install
cannot update one another — a user has to pick one.

Submission is a merge request against
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) adding
`metadata/sh.aminov.alternate.yml`. That file is ready to copy at
[docs/fdroid/sh.aminov.alternate.yml](fdroid/sh.aminov.alternate.yml). Listing
text and screenshots are already in `fastlane/metadata/`, which is where
F-Droid looks for them.

Two things must be true before the merge request is worth opening: the tag the
build points at (`v1.0.0`) has to exist on GitHub, and F-Droid's buildserver
has to support the toolchain this project uses — AGP 9.4.0 and compile SDK 37
are recent, and their images lag behind. If the build fails on their side the
fix is usually to pin an older AGP, not to change the app.

## Google Play

The developer fee is 25 USD, once, not a subscription. The harder parts are the
identity verification and — for new personal accounts — the requirement to run
a closed test with twelve testers for fourteen days before the app may go
public.
