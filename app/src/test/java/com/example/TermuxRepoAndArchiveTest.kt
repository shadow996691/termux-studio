package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.local.CdnLibraryEntity
import com.example.data.termux.TermuxAptRepoBuilder
import com.example.data.termux.TermuxBridge
import com.example.data.workspace.TarGzExtractor
import com.example.ui.applyAutoIndentationIfNewline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TermuxRepoAndArchiveTest {

    @Test
    fun raylibArchiveExtractsCleanlyWithHeadersAndLibraries() {
        val tarFile = File("src/main/assets/libs/raylib-6.2_linux_arm64.tar.gz")
        assertTrue("Expected bundled raylib-6.2_linux_arm64.tar.gz asset", tarFile.exists())

        val tempDest = java.nio.file.Files.createTempDirectory("raylib_test").toFile()
        try {
            val result = tarFile.inputStream().use { stream ->
                TarGzExtractor.extractTarGz(stream, tempDest, stripFirstComponent = true)
            }
            assertTrue("Extraction should succeed", result.isSuccess)
            assertTrue("raylib.h should exist", File(tempDest, "include/raylib.h").exists())
            assertTrue("rlgl.h should exist", File(tempDest, "include/rlgl.h").exists())
            assertTrue("raymath.h should exist", File(tempDest, "include/raymath.h").exists())
            assertTrue("libraylib.a should exist", File(tempDest, "lib/libraylib.a").exists())
            assertTrue("libraylib.so should be materialized", File(tempDest, "lib/libraylib.so").exists())
        } finally {
            tempDest.deleteRecursively()
        }
    }

    @Test
    fun termuxStudioPackageIsUpdatedTo100WithLiveCdnAndLibFetch() {
        assertEquals("1.0.0", TermuxAptRepoBuilder.PACKAGE_VERSION)
        assertEquals("termux-studio_1.0.0_all.deb", TermuxAptRepoBuilder.DEB_FILE_NAME)

        val script = File("src/main/assets/termux_pkg/termux-studio").readText()
        assertTrue(script.contains("VERSION=\"1.0.0\""))
        assertTrue(script.contains("cmd_cdn_resolve()"))
        assertTrue(script.contains("cmd_lib_fetch()"))
        assertTrue(script.contains("wget)"))
        assertTrue(script.contains("curl|*)"))
        assertTrue(script.contains("pkg)"))
    }

    @Test
    fun appCliLinksZigAsmRustCAndCppViaObjectFiles() {
        val appCli = File("src/main/assets/libs/app/app_cli.sh").readText()
        assertTrue(appCli.contains("compile_to_object()"))
        assertTrue(appCli.contains("zig build-obj"))
        assertTrue(appCli.contains("rustc --emit=obj"))
        assertTrue(appCli.contains("nasm -f elf64"))
        assertTrue(appCli.contains("clang -c"))
        assertTrue(appCli.contains("app_runtime.o"))
    }

    @Test
    fun termuxBridgeBuildsLiveCdnResolveAndLibFetchCommands() {
        val cdn = CdnLibraryEntity(
            name = "json_live",
            url = "https://example.org/json.hpp",
            installRelPath = "online-live",
            compilerFlags = "",
            isEnabled = true
        )
        val cdnCmd = TermuxBridge.buildCdnResolvePrefix("/storage/emulated/0/TermuxStudio", listOf(cdn))
        assertTrue(cdnCmd.contains("termux-studio cdn-resolve"))
        assertTrue(cdnCmd.contains("json_live::https://example.org/json.hpp"))

        val wgetCmd = TermuxBridge.buildTermuxLibFetchCommand(
            "/storage/emulated/0/TermuxStudio",
            "wget",
            "stb",
            "https://example.org/stb_image.h"
        )
        assertEquals(
            "termux-studio lib-fetch \"/storage/emulated/0/TermuxStudio\" \"wget\" \"stb\" \"https://example.org/stb_image.h\"",
            wgetCmd
        )
    }

    @Test
    fun editorAutoIndentPreservesAndExpandsBraceIndentation() {
        val prev = TextFieldValue("    if (ready) {", TextRange(16))
        val typedNewline = TextFieldValue("    if (ready) {\n", TextRange(17))
        val indented = applyAutoIndentationIfNewline(prev, typedNewline, "    ")
        assertEquals("    if (ready) {\n        ", indented.text)
        assertEquals(25, indented.selection.start)
    }
}
