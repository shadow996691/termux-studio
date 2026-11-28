#ifndef TERMUX_STUDIO_APP_H
#define TERMUX_STUDIO_APP_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum AppPlatform {
    APP_PLATFORM_ANDROID = 0,
    APP_PLATFORM_LINUX = 1,
    APP_PLATFORM_WINDOWS = 2,
    APP_PLATFORM_MACOS = 3
} AppPlatform;

typedef struct AppConfig {
    const char *app_name;
    const char *package_id;
    const char *version;
    int width;
    int height;
    int target_fps;
    bool enable_gpu_drivers;
    bool enable_raylib;
} AppConfig;

AppConfig app_default_config(const char *name);
int app_init(const AppConfig *config);
void app_enable_gpu_drivers(void);
AppPlatform app_get_platform(void);
const char *app_get_platform_name(void);
const char *app_get_gpu_backend(void);
void app_log(const char *tag, const char *message);
int app_run(void (*on_frame)(float dt));
void app_shutdown(void);

#ifdef __cplusplus
}
#endif

#endif // TERMUX_STUDIO_APP_H
