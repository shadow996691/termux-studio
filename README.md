Termux studio — About the App (Latest Version)
Termux studio (com.termux.studio) is a mobile polyglot IDE and Termux companion addon built for compiling, running, graphics rendering, and packaging native software directly on Android.
1. Identity & Termux Addon Architecture
App Name: Termux studio
Package ID: com.termux.studio (Termux Addon namespace)
Visual Theme: Deep Obsidian Code Studio (JetBrains Mono, Space Grotesk, Inter)
Folder Sharing (Termux:X11 Style):
Shares your Termux $HOME directory cleanly through Termux's com.termux.documents Storage Access Framework provider—no root required and no sharedUserId signing-key conflict with F-Droid Termux.
Supports ~/storage/shared (/sdcard) via termux-setup-storage and Full External Storage Access for code projects, headers, .so shared objects, and .a static archives.
F-Droid Onboarding: Automatically detects com.termux, com.termux.api, and com.termux.x11, linking directly to Termux on F-Droid when not installed.
2. Supported Languages & Compilers
Termux studio provides language-aware syntax highlighting, contextual autocomplete, templates, and automatic Termux compilation/execution for compiled and middle-level languages:
Language	Extensions	Termux Compiler / Runtime
C	.c	gcc / clang
C++	.cpp, .cc, .cxx	g++ / clang++
Headers	.h, .hpp, .hh, .hxx	Auto-indexed for #include "..." & symbol completion
Java	.java	javac + java
Kotlin	.kt, .kts	kotlinc + java -jar
Rust	.rs	rustc
Go	.go	go run / go build
Zig	.zig	zig run / zig build-exe
Assembly	.s, .asm	clang / nasm + ld
Fortran	.f90, .f95, .f	gfortran
D / Nim / Pascal / C#	.d, .nim, .pas, .cs	ldc2, nim, fpc, mcs + mono
Build & Config	Makefile, build.config, .sh	make, app build, bash
Supports building Executables, Shared Objects (.so with -shared -fPIC), and Static Archives (.a via ar rcs).
3. Built-In Libraries: Raylib & Custom App Packager
Bundled inside the application and synced automatically to your workspace (libs/) and Termux (~/.termux_studio/):
Raylib (libs/raylib/raylib.h)
Pre-bundled header declarations, autocomplete symbols, and automatic linker flags (-lraylib -lGLESv2 -lEGL) for 2D/3D hardware-accelerated graphics and Termux:X11 rendering.
Custom App Library & CLI (libs/app/app.h, app_runtime.c, app)
Turns C/C++ projects into target application packages configured via build.config.
Execute from the Libraries & Packager UI or directly in the Terminal:
app Android → Generates build.config for Android (.apk)
app Linux [debian|fedora|arch|appimage] → Generates build.config for Linux desktop with selectable distro (.deb, .rpm, .pkg.tar.zst, .AppImage)
app Window → Generates build.config for Windows (.exe)
app macOS → Generates build.config for macOS (.app bundle)
app build → Reads build.config, compiles sources with GPU & Raylib flags, and outputs the packaged build artifact into dist/.
4. Automatic GPU API Driver Access
Declares OpenGL ES 3.0 and Vulkan hardware features (required="false" for broad device compatibility).
Probes real device EGL/GLES & Vulkan capabilities (GpuDriverDetector) and automatically exports GPU driver variables (TERMUX_STUDIO_GPU=1, DISPLAY=:0, GALLIUM_DRIVER=zink, MESA_LOADER_DRIVER_OVERRIDE=zink, LD_LIBRARY_PATH) into Termux runs so user programs can access GPU drivers automatically.
5. Terminal Persistence, GitHub Sync & Hardware Permissions
Integrated Terminal & Background Listening: Uses Termux RUN_COMMAND (PendingIntent callbacks) paired with a dataSync foreground service (BuildPersistenceService) so builds and long-running commands continue delivering stdout, stderr, and clickable error diagnostics even when minimized.
Git & GitHub CLI Sync: Executes git init, git status, git diff, git add -A && git commit, git clone, git pull, and git push through Termux, with GitHub author & Personal Access Token credential setup.
Hardware & Storage Permissions Manager: Declares and provides on-demand runtime grants for Termux RUN_COMMAND, Termux SAF $HOME, Full External Storage, Internet, Camera, Microphone/Audio, Bluetooth (CONNECT/SCAN), Location, and Notifications.