package com.example

import com.example.data.editor.CppLanguageEngine
import com.example.data.model.CodeFileType
import com.example.data.model.SuggestionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CppLanguageEngineTest {

    @Test
    fun syntaxHighlighting_preservesExactCharacterLengthAcrossLanguages() {
        val cppSample = """
            #include <iostream>
            #include "libs/raylib/raylib.h"
            // single line comment
            /* block comment */
            int main() {
                std::string s = "hello";
                return 0;
            }
        """.trimIndent()
        val javaSample = """
            import java.util.*;
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Termux studio");
                }
            }
        """.trimIndent()
        val configSample = """
            # build.config
            TARGET_PLATFORM=Android
            PACKAGE_FORMAT=apk
        """.trimIndent()

        assertEquals(cppSample.length, CppLanguageEngine.highlightCode(cppSample, CodeFileType.CPP_SOURCE).text.length)
        assertEquals(javaSample.length, CppLanguageEngine.highlightCode(javaSample, CodeFileType.JAVA).text.length)
        assertEquals(configSample.length, CppLanguageEngine.highlightCode(configSample, CodeFileType.BUILD_CONFIG).text.length)
    }

    @Test
    fun autocomplete_suggestsRaylibAndAppHeaders() {
        val angleRay = "#include <ray"
        val stdSuggestions = CppLanguageEngine.computeSuggestions(
            code = angleRay,
            cursorOffset = angleRay.length,
            localHeaders = emptyList(),
            localSymbols = emptyList()
        )
        assertTrue(stdSuggestions.any { it.displayLabel == "<raylib.h>" && it.kind == SuggestionKind.STD_HEADER })

        val quoteApp = "#include \"libs/app"
        val localSuggestions = CppLanguageEngine.computeSuggestions(
            code = quoteApp,
            cursorOffset = quoteApp.length,
            localHeaders = listOf("include/vector_math.h"),
            localSymbols = emptyList()
        )
        assertTrue(localSuggestions.any { it.displayLabel == "\"libs/app/app.h\"" && it.kind == SuggestionKind.LOCAL_HEADER })
    }

    @Test
    fun fileTypes_detectCompiledAndMiddleLevelLanguages() {
        assertEquals(CodeFileType.JAVA, CodeFileType.fromFileName("Main.java", false))
        assertEquals(CodeFileType.RUST, CodeFileType.fromFileName("main.rs", false))
        assertEquals(CodeFileType.GO, CodeFileType.fromFileName("server.go", false))
        assertEquals(CodeFileType.ZIG, CodeFileType.fromFileName("kernel.zig", false))
        assertEquals(CodeFileType.ASSEMBLY, CodeFileType.fromFileName("boot.s", false))
        assertEquals(CodeFileType.BUILD_CONFIG, CodeFileType.fromFileName("build.config", false))
        assertEquals(CodeFileType.PACKAGE_ARTIFACT, CodeFileType.fromFileName("MyApp.apk", false))
    }

    @Test
    fun diagnosticParser_extractsGccAndJavacErrors() {
        val output = "main.cpp:14:9: error: expected ';' before 'return'\nMain.java:4: error: ';' expected"
        val diags = CppLanguageEngine.parseCompilerDiagnostics(output)
        assertEquals(2, diags.size)
        assertEquals("main.cpp", diags[0].fileName)
        assertEquals(14, diags[0].line)
        assertEquals("Main.java", diags[1].fileName)
        assertEquals(4, diags[1].line)
    }
}
