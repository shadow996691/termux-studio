package com.example.data.gpu

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Build
import com.example.data.model.GpuDriverInfo

object GpuDriverDetector {

    fun probeGpuDrivers(context: Context): GpuDriverInfo {
        val pm = context.packageManager
        val hasVulkan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        } else {
            false
        }
        val vulkanVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.systemAvailableFeatures
                .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?.version?.let { ver ->
                    val major = (ver shr 22) and 0x3FF
                    val minor = (ver shr 12) and 0x3FF
                    val patch = ver and 0xFFF
                    "$major.$minor.$patch"
                } ?: if (hasVulkan) "1.1+" else "Unavailable"
        } else "Unavailable"

        var vendor = "Hardware GPU"
        var renderer = "OpenGL ES 3.0+"
        var glesVersion = "OpenGL ES 3.0"

        try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display != EGL14.EGL_NO_DISPLAY) {
                val vers = IntArray(2)
                if (EGL14.eglInitialize(display, vers, 0, vers, 1)) {
                    val attribs = intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                        EGL14.EGL_NONE
                    )
                    val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
                    val numConfig = IntArray(1)
                    if (EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfig, 0) && numConfig[0] > 0) {
                        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                        val eglCtx = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
                        val pbufAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                        val surface = EGL14.eglCreatePbufferSurface(display, configs[0], pbufAttribs, 0)
                        if (eglCtx != EGL14.EGL_NO_CONTEXT && surface != EGL14.EGL_NO_SURFACE) {
                            if (EGL14.eglMakeCurrent(display, surface, surface, eglCtx)) {
                                vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: vendor
                                renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: renderer
                                glesVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: glesVersion
                                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                            }
                            EGL14.eglDestroySurface(display, surface)
                        }
                        if (eglCtx != EGL14.EGL_NO_CONTEXT) {
                            EGL14.eglDestroyContext(display, eglCtx)
                        }
                    }
                    EGL14.eglTerminate(display)
                }
            }
        } catch (_: Exception) {
        }

        val recommendedDriver = when {
            vendor.contains("Qualcomm", ignoreCase = true) || renderer.contains("Adreno", ignoreCase = true) ->
                "Turnip Vulkan + Mesa Zink"
            vendor.contains("ARM", ignoreCase = true) || renderer.contains("Mali", ignoreCase = true) ->
                "Mali Vulkan + ANGLE / VirGL"
            else -> "Mesa Zink / Android EGL"
        }

        return GpuDriverInfo(
            vendor = vendor,
            renderer = renderer,
            glesVersion = glesVersion,
            hasVulkan = hasVulkan,
            vulkanVersion = vulkanVersion,
            recommendedTermuxDriver = recommendedDriver
        )
    }

    fun buildTermuxGpuExports(): String {
        return """
            export TERMUX_STUDIO_GPU=1
            export DISPLAY="${'$'}{DISPLAY:-:0}"
            export GALLIUM_DRIVER="${'$'}{GALLIUM_DRIVER:-zink}"
            export MESA_LOADER_DRIVER_OVERRIDE="${'$'}{MESA_LOADER_DRIVER_OVERRIDE:-zink}"
            export MESA_GL_VERSION_OVERRIDE="${'$'}{MESA_GL_VERSION_OVERRIDE:-4.6}"
            export LD_LIBRARY_PATH="${'$'}PREFIX/lib:/vendor/lib64:/system/lib64:${'$'}{LD_LIBRARY_PATH:-}"
        """.trimIndent().replace("\n", "; ")
    }
}
