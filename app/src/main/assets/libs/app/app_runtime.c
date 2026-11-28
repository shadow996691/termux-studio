#include "app.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static AppConfig g_active_config;
static int g_initialized = 0;

AppConfig app_default_config(const char *name) {
    AppConfig cfg;
    cfg.app_name = name ? name : "TermuxStudioApp";
    cfg.package_id = "com.termux.studio.app";
    cfg.version = "1.0.0";
    cfg.width = 1280;
    cfg.height = 720;
    cfg.target_fps = 60;
    cfg.enable_gpu_drivers = true;
    cfg.enable_raylib = false;
    return cfg;
}

void app_enable_gpu_drivers(void) {
    setenv("TERMUX_STUDIO_GPU", "1", 1);
    if (!getenv("GALLIUM_DRIVER")) {
        setenv("GALLIUM_DRIVER", "zink", 0);
    }
    if (!getenv("MESA_LOADER_DRIVER_OVERRIDE")) {
        setenv("MESA_LOADER_DRIVER_OVERRIDE", "zink", 0);
    }
}

AppPlatform app_get_platform(void) {
#if defined(__ANDROID__)
    return APP_PLATFORM_ANDROID;
#elif defined(_WIN32) || defined(_WIN64)
    return APP_PLATFORM_WINDOWS;
#elif defined(__APPLE__)
    return APP_PLATFORM_MACOS;
#else
    return APP_PLATFORM_LINUX;
#endif
}

const char *app_get_platform_name(void) {
    switch (app_get_platform()) {
        case APP_PLATFORM_ANDROID: return "Android";
        case APP_PLATFORM_WINDOWS: return "Window";
        case APP_PLATFORM_MACOS:   return "macOS";
        default:                   return "Linux";
    }
}

const char *app_get_gpu_backend(void) {
    const char *driver = getenv("GALLIUM_DRIVER");
    if (driver && driver[0] != '\0') return driver;
#if defined(__ANDROID__)
    return "OpenGL ES 3.0 / Vulkan";
#elif defined(_WIN32)
    return "Direct3D / OpenGL";
#elif defined(__APPLE__)
    return "Metal";
#else
    return "Mesa OpenGL / Vulkan";
#endif
}

void app_log(const char *tag, const char *message) {
    printf("[%s][%s] %s\n", app_get_platform_name(), tag ? tag : "App", message ? message : "");
}

int app_init(const AppConfig *config) {
    if (config) {
        g_active_config = *config;
    } else {
        g_active_config = app_default_config("TermuxStudioApp");
    }
    if (g_active_config.enable_gpu_drivers) {
        app_enable_gpu_drivers();
    }
    g_initialized = 1;
    app_log("AppInit", g_active_config.app_name);
    app_log("GPU", app_get_gpu_backend());
    return 0;
}

int app_run(void (*on_frame)(float dt)) {
    if (!g_initialized) {
        AppConfig def = app_default_config("TermuxStudioApp");
        app_init(&def);
    }
    if (on_frame) {
        on_frame(1.0f / (g_active_config.target_fps > 0 ? g_active_config.target_fps : 60));
    }
    return 0;
}

void app_shutdown(void) {
    if (g_initialized) {
        app_log("AppShutdown", "Exited cleanly");
        g_initialized = 0;
    }
}
