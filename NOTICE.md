# NOTICE — Fork of TV Bro with uBlock-Compatible Adblocking

This project is a modified distribution of **TV Bro** by Fedir Tsapana.

*   Original project: https://github.com/truefedex/tv-bro
*   Original license: `LICENSE.md` (Copyright (c) 2019, Fedir Tsapana) — redistributions in source form must retain notice; modified binary redistributions must not contain "TV Bro" in app name, must not use same `applicationId`/`ic_launcher`, and must contain an About window stating it uses TV Bro sources with link (see `app/src/main/java/com/phlox/tvwebbrowser/activity/main/dialogs/settings/VersionSettingsView.kt`).

## Modifications in this fork

*   Enhanced ad-blocking engine to fetch multiple **uBlock Origin default** filter lists (EasyList, EasyPrivacy, Peter Lowe's, Online Malicious URL Blocklist, uBlock filters) and concatenate them for `com.github.truefedex:ad-block` `AdBlockClient.parse()` — trust-first, shipping upstream lists verbatim (`app/common/src/main/java/com/phlox/tvwebbrowser/Config.kt:DEFAULT_UBLOCK_LIST_URLS`, `app/src/main/java/com/phlox/tvwebbrowser/activity/main/AdblockModel.kt`).
*   Dedicated `Settings → Ad Blocking` section that **expands** the existing master toggle (single source of truth `Config.adBlockEnabled`) with per-list toggles, custom URL, live stats and last-update (`app/src/main/java/com/phlox/tvwebbrowser/activity/main/dialogs/settings/MainSettingsView.kt`).
*   License attribution added to About screen and this NOTICE.

## Filter list licenses

Filter lists are data, not code, fetched at runtime and cached in `filesDir/adblock_ser.dat`:

*   EasyList / EasyPrivacy — https://easylist.to/ (CC BY-SA / GPL-compatible)
*   Peter Lowe's — https://pgl.yoyo.org/ (free for non-commercial)
*   uAssets (uBO filters, badware, privacy, annoyances) — https://github.com/uBlockOrigin/uAssets (GPL-3.0 / CC)

Lists are not redistributed in binary except as optional bundled cache; runtime fetching preserves upstream attribution.

## Engine

*   `com.github.truefedex:ad-block:0.0.4` (Brave-derived) — MIT / MPL-2.0 compatible.
