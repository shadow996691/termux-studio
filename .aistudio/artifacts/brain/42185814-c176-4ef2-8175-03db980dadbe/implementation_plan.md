# Termux studio v1.1 — Fixes, Multi-Language App Packaging, Settings & Compact Editor Popups

This update resolves the seven workflow issues raised across CDN libraries, user-folder `App` packaging for Zig/ASM/Rust/C/C++, editor auto-indentation, multi-theme IDE settings, compact Editor suggestion/build popups, offline storage imports, Termux-native (`pkg`/`curl`/`wget`) library downloads, and the companion `pkg install termux-studio` package.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All four architectural choices below were confirmed in Phase 1 and govern this update.

- **Confirmed Decision 1 — Live Compile-Time CDN Libraries**: CDN entries store remote API/header/library URLs without pre-downloading offline copies. Compiling with an active CDN library requires an active internet connection so Termux fetches remote headers/objects live at compile time.
- **Confirmed Decision 2 — Themes & Customizable Accents**: The new Settings view adds multiple dark studio themes (*Deep Obsidian*, *Midnight Nord*, *Dracula Slate*, *Solarized Dark*, *Terminal Green*) combined with selectable accent colors (*Electric Cyan*, *Mint Emerald*, *Warm Amber*, *Soft Violet*, *Coral*).
- **Confirmed Decision 3 — Compact Editor UI**: Replace the half-screen suggestion list with a small floating suggestion popup box, and show compile progress/results inside a collapsible slide-up build drawer right in the Editor while mirroring full logs to the Terminal tab.
- **Confirmed Decision 4 — Multi-Language `App` Packaging via Object Linking**: Upgrade the `App` C/C++ library (stored cleanly in the user folder `libs/app/` without over-featured UI treatment) so **Zig**, **Assembly (ASM)**, **Rust**, **C**, and **C++** sources compile into object files linked together with the `app` runtime before producing target packages.

---

## 1. Overview & Core Concept

- **What It Does**: Delivers a compact, indentation-aware mobile IDE experience backed by Termux execution and Termux-powered library tooling. Online CDN libraries stay lightweight and resolve live over the network at compile time, while offline users can import libraries directly from Android internal storage or download packages using Termux (`pkg`, `curl`, `wget`).
- **Target Audience / Persona**: Mobile C, C++, Zig, Rust, Assembly, and Java developers compiling native binaries, Raylib visuals, and cross-platform `App` bundles on Android via Termux.
- **Key Value**: Keeps the Editor spacious during mobile typing, makes Terminal/build results consistently appear inside Termux studio, works both online (CDN + `pkg`/`curl`/`wget`) and offline (**Import from Internal Storage**), and unlocks full theme, compiler, indentation, and language customization.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Compact Editor Typing, Auto-Indentation & Slide-Up Build Drawer**:
   - Pressing **Enter** automatically carries forward the leading indentation of the active line and adds an extra indent block after `{` or `:`. Quick accessory keys include smart **Indent** and **Outdent**.
   - Code suggestions render inside a small floating popup window (compact height capped around three visible rows with a dismiss toggle) anchored cleanly above the accessory bar so code remains visible while typing.
   - Tapping **Run** or **Build** executes inside Termux and opens a **slide-up build drawer** at the bottom of the Editor showing live build status, exit code, stdout/stderr, clickable error lines, and an **"Open Terminal"** shortcut—while also recording the complete session in the **Terminal** tab.

2. **Settings Hub (Themes, Compiler, Editor, Languages)**:
   - Accessible cleanly from the top bar Settings icon:
     - **Themes**: Switch between five curated dark code palettes (*Deep Obsidian*, *Midnight Nord*, *Dracula Slate*, *Solarized Dark*, *Terminal Green*) and pick any primary accent color, updating UI surfaces and syntax highlighting live.
     - **Compiler**: Configure default C/C++/multi-language toolchain, optimization flag (`-O0`, `-O2`, `-O3`), warning flags, and standards (`c17`/`c23`, `c++20`/`c++23`).
     - **Editor**: Adjust font size, indentation width (`2`, `4`, `8` spaces or tabs), auto-indent toggle, line numbers visibility, word wrap, and floating suggestion popup toggle.
     - **Languages**: Toggle active language toolchains (`C`, `C++`, `Zig`, `Assembly`, `Rust`, `Go`, `Java`, `Kotlin`, `Fortran`) and default project template language.

3. **Libs Tab — Import from Internal Storage, `pkg`/`curl`/`wget` Downloader & `App` Library**:
   - **Import from Internal Storage**: Opens the Android system file/folder picker so offline users can browse internal storage and copy or extract `.h`/`.hpp` headers, `.so`/`.a` binaries, `.tar.gz`/`.zip` archives, or entire library directories directly into `<UserFolder>/libs/`.
   - **Download via Termux (`pkg` / `curl` / `wget`)**: Dedicated Termux download card letting users run explicit `pkg install`, `curl`, or `wget` commands inside Termux to pull real libraries into Termux or `<UserFolder>/libs/`.
   - **`App` Library in User Folder**: Positioned naturally alongside Raylib and user libraries in `libs/app/` without occupying the whole Libs screen hero. Supports compiling **Zig**, **ASM**, **Rust**, **C**, and **C++** sources into `.o` object files linked with the `app` C runtime before `app build` produces `.apk`, `.deb`, `.exe`, or `.app` packages.

4. **Online CDN Library Workflow (Wi-Fi / Internet Required at Compile Time)**:
   - In the Compiler tab's **CDN Libs** panel, users register a library **Name** + **API / Header / Archive URL** (without downloading anything upfront).
   - When compiling a project that references enabled CDN libraries, `termux-studio` checks network availability inside Termux first, fails clearly if offline, or fetches the CDN headers/objects live over HTTPS into a transient Termux build session and passes their `-I` / `-L` flags to the compiler.

### Visual Identity & Theme

- **Aesthetic Direction**: Modular dark code-studio precision with live-swappable dark colorways and customizable neon/pastel accents.
- **Color Palette & Mood**: Dynamic theme tokens driving background canvas (`#0B0E14` Deep Obsidian default, plus Nord, Dracula, Solarized Dark, and Terminal Black/Green), elevated card surfaces, muted gutters, and user-chosen accent (`#38BDF8` Cyan default, Mint, Amber, Violet, Coral).
- **Typography & Hierarchy**: `Space Grotesk` for crisp section titles, `Inter` for settings controls and metadata, and `JetBrains Mono` for code, floating suggestions, slide-up compiler logs, and Terminal output.
- **Component Styling & Layout**: Material 3 responsive layout (`NavigationBar` on phones, `NavigationRail` on tablets), small overlay surfaces (`Surface` with elevation & border), 48dp touch targets, and smooth `AnimatedVisibility` slide-up transitions under 250ms.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Live Compile-Time Fetch for CDN vs. Persistent Storage for Downloaded/Imported Libraries**:
  - *Chosen Approach*: Treat **CDN Libs** strictly as online URL providers fetched on-demand when compiling with internet active, while **Libs -> Import from Internal Storage** and **Termux (`pkg`/`curl`/`wget`) Download** handle persistent offline libraries inside `<UserFolder>/libs/`.
  - *Why*: Matches standard CDN behavior—zero upfront storage bloat for online libraries, explicit requirement for internet at compile time, and clean separation from offline library imports.

- **Decision 2 — Object-File Linking (`ld`/compiler driver) for Multi-Language `App` Packaging**:
  - *Chosen Approach*: Compile each enabled language entry (**Zig** `zig build-obj`, **ASM** `nasm -f elf64` / `clang -c`, **Rust** `rustc --emit=obj`, **C** `clang/gcc -c`, **C++** `clang++/g++ -c`) into `.o` object files and link them together with compiled `app_runtime.o` into the final executable or shared object packaged by `app build`.
  - *Why*: Every compiled language exposing C ABI symbols can share `AppConfig`, `app_init()`, and GPU initialization seamlessly without duplicating the `App` runtime per language.

- **Decision 3 — Reliable Termux Result Delivery Back into Termux studio Terminal**:
  - *Chosen Approach*: Execute compilation through `termux-studio` (`pkg install termux-studio` v1.1.0), which both returns exit status/output via `RUN_COMMAND` `PendingIntent` AND writes structured job logs into `<UserFolder>/.termux-ipc/` so Termux studio's **Terminal** tab and **Editor slide-up drawer** always display exact compile and program output inside the app.
  - *Why*: Guarantees compile and program execution output is never lost inside the external Termux app even when switching apps or under background restrictions.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        TERMUX STUDIO ANDROID APP                           │
│                                                                            │
│  ┌──────────────┐   ┌───────────────────────────────────────────────────┐  │
│  │ Settings     │   │                     Editor Tab                    │  │
│  │ • Themes     │   │  • Auto-Indentation (2/4/8 spaces or Tab)         │  │
│  │ • Accents    │   │  • Small Floating Suggestion Popup (~3 rows)      │  │
│  │ • Compiler   │   │  • Compact Slide-Up Build Drawer (Run/Build log)  │  │
│  │ • Editor     │   └─────────────────────────┬─────────────────────────┘  │
│  │ • Languages  │                             │                            │
│  └──────┬───────┘                             │                            │
│         │         ┌───────────────────────────▼──────────────────────────┐ │
│         │         │   Compiler Tab (Install │ Choose │ CDN Online URLs)  │ │
│         │         │   Libs Tab (User Folder │ Import Storage │ pkg/curl) │ │
│         │         │   Terminal Tab (Always displays compile & run logs)  │ │
│         │         └───────────────────────────┬──────────────────────────┘ │
└─────────┼─────────────────────────────────────┼────────────────────────────┘
          │ Live Theme/Editor Prefs (DataStore) │ Termux RUN_COMMAND + IPC
┌─────────▼─────────────────────────────────────▼────────────────────────────┐
│                   TERMUX + `termux-studio` PACKAGE (v1.1.0)                │
│                                                                            │
│  $PREFIX/bin/termux-studio                                                 │
│   ├── probe / listen   ──► Writes compiler status & stdout/stderr to IPC   │
│   ├── cdn-resolve      ──► Requires Wi-Fi/Net -> live curl fetch on build  │
│   ├── lib-fetch        ──► Downloads libs via explicit pkg / curl / wget   │
│   └── compile          ──► Runs Termux clang/gcc/rustc/zig/nasm/javac      │
│                                                                            │
│  Shared User Folder: /sdcard/TermuxStudio ◄══symlink══► ~/TermuxStudio     │
│   ├── libs/app/   (Multi-lang C/C++/Zig/ASM/Rust -> .o + app_runtime.o)    │
│   ├── libs/raylib/(ARM64 Raylib 6.2 headers & binaries)                    │
│   ├── libs/<imp>/ (Imported offline from Internal Storage or wget/curl)    │
│   └── termux-repo/(Updated termux-studio_1.1.0_all.deb + GitHub APT meta)  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **Settings Preferences (DataStore)**: Stores active dark theme ID, custom accent preset, editor indentation width & space/tab mode, auto-indent flag, font size, compact suggestion popup toggle, default compiler & optimization level, word wrap, line numbers toggle, and enabled languages.
- **CDN Online Library Records (Room)**: Stores library name, online URL/API endpoint, compile flags (`-I`/`-L`/`-l`), and enabled-for-build toggle (no local library archive downloaded into user `libs/`).
- **Slide-Up Build Drawer State**: Tracks live compilation job status, command string, trimmed stdout/stderr lines, exit code, diagnostics, and open/collapsed visibility directly inside the Editor view.

### Interactive Component & State Mapping

- **Editor Input & Enter Key Handler**: Intercepts newline insertions to compute matching leading indentation plus `{` / `:` block indent depth, updates `TextFieldValue` selection seamlessly, and triggers the compact floating suggestion overlay.
- **Settings Sheet / Screen Navigation**: Tapping the TopAppBar Settings icon opens the tabbed Settings view (*Themes*, *Compiler*, *Editor*, *Languages*); theme & accent changes propagate immediately through `MaterialTheme`.
- **Offline Storage Importer**: Launches Android's system document/tree picker in the **Libs** tab, inspects chosen `.so`/`.a`/`.h`/`.zip`/`.tar.gz` or directory URIs, extracts or copies contents into `<UserFolder>/libs/<name>/`, and rescans **Add Library**.
- **`App` Multi-Language Object Build Pipeline**: Updated `app build` reads `ENTRY_SOURCE` (or `ENTRY_SOURCES` across `.c`, `.cpp`, `.zig`, `.asm`/`.s`, `.rs`), compiles each file to `.o` using its native Termux compiler, links with `app_runtime.o`, and packages the target platform output.
