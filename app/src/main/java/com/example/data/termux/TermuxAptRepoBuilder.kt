package com.example.data.termux

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

/**
 * Builds a genuine Debian/Termux `.deb` archive (`termux-studio_1.0.0_all.deb`) and a complete
 * APT repository structure (`pool/`, `dists/stable/`) inside the user's shared folder so users can
 * either push `termux-repo/` to GitHub Pages for `pkg install termux-studio` or install locally.
 */
object TermuxAptRepoBuilder {

    const val PACKAGE_NAME = "termux-studio"
    const val PACKAGE_VERSION = "1.0.0"
    const val DEB_FILE_NAME = "termux-studio_1.0.0_all.deb"

    fun generateAptRepository(context: Context, repoRootDir: File): Result<File> {
        return try {
            repoRootDir.mkdirs()
            val termuxStudioScript = context.assets.open("termux_pkg/termux-studio").readBytes()
            val appCliScript = context.assets.open("libs/app/app_cli.sh").readBytes()

            val controlText = """
                Package: $PACKAGE_NAME
                Version: $PACKAGE_VERSION
                Architecture: all
                Maintainer: Termux studio <dev@termuxstudio.local>
                Section: devel
                Priority: optional
                Depends: bash, coreutils, curl, wget, git, grep, sed, tar
                Description: Termux studio companion CLI, compiler bridge, and IPC listener daemon
                 Provides termux-studio and app commands for live CDN resolution, pkg/curl/wget
                 library downloads, and multi-language C/C++/Zig/ASM/Rust builds inside Termux.
            """.trimIndent() + "\n"

            val controlTarGz = buildTarGz(
                listOf(
                    TarEntrySpec("./control", controlText.toByteArray(Charsets.UTF_8), mode = 0b110_100_100) // 0644
                )
            )
            val dataTarGz = buildTarGz(
                listOf(
                    TarEntrySpec("./data/data/com.termux/files/usr/bin/termux-studio", termuxStudioScript, mode = 0b111_101_101), // 0755
                    TarEntrySpec("./data/data/com.termux/files/usr/bin/app", appCliScript, mode = 0b111_101_101)
                )
            )

            val debBytes = buildArArchive(
                listOf(
                    "debian-binary" to "2.0\n".toByteArray(Charsets.US_ASCII),
                    "control.tar.gz" to controlTarGz,
                    "data.tar.gz" to dataTarGz
                )
            )

            val debRelPath = "pool/main/t/termux-studio/$DEB_FILE_NAME"
            val debFile = File(repoRootDir, debRelPath)
            debFile.parentFile?.mkdirs()
            debFile.writeBytes(debBytes)

            val debMd5 = digestHex(debBytes, "MD5")
            val debSha1 = digestHex(debBytes, "SHA-1")
            val debSha256 = digestHex(debBytes, "SHA-256")

            fun makePackagesIndex(arch: String): String = """
                Package: $PACKAGE_NAME
                Version: $PACKAGE_VERSION
                Architecture: $arch
                Maintainer: Termux studio <dev@termuxstudio.local>
                Depends: bash, coreutils, curl, wget, git, grep, sed, tar
                Filename: $debRelPath
                Size: ${debBytes.size}
                MD5sum: $debMd5
                SHA1: $debSha1
                SHA256: $debSha256
                Section: devel
                Priority: optional
                Description: Termux studio companion CLI, compiler bridge, and IPC listener daemon
            """.trimIndent() + "\n\n"

            val releaseEntries = mutableListOf<Triple<String, Int, ByteArray>>()
            listOf("all", "aarch64", "arm", "x86_64").forEach { arch ->
                val packagesText = makePackagesIndex(if (arch == "all") "all" else "all").toByteArray(Charsets.UTF_8)
                val packagesGz = gzipBytes(packagesText)

                val plainRel = "main/binary-$arch/Packages"
                val gzRel = "main/binary-$arch/Packages.gz"

                File(repoRootDir, "dists/stable/$plainRel").apply {
                    parentFile?.mkdirs()
                    writeBytes(packagesText)
                }
                File(repoRootDir, "dists/stable/$gzRel").apply {
                    parentFile?.mkdirs()
                    writeBytes(packagesGz)
                }

                releaseEntries.add(Triple(plainRel, packagesText.size, packagesText))
                releaseEntries.add(Triple(gzRel, packagesGz.size, packagesGz))
            }

            val releaseText = buildString {
                appendLine("Origin: TermuxStudio")
                appendLine("Label: TermuxStudio")
                appendLine("Suite: stable")
                appendLine("Codename: stable")
                appendLine("Architectures: all aarch64 arm x86_64")
                appendLine("Components: main")
                appendLine("Description: Termux studio official companion APT repository")
                appendLine("MD5Sum:")
                releaseEntries.forEach { (rel, size, bytes) ->
                    appendLine(" ${digestHex(bytes, "MD5")} ${size.toString().padStart(16)} $rel")
                }
                appendLine("SHA256:")
                releaseEntries.forEach { (rel, size, bytes) ->
                    appendLine(" ${digestHex(bytes, "SHA-256")} ${size.toString().padStart(16)} $rel")
                }
            }
            File(repoRootDir, "dists/stable/Release").writeText(releaseText)

            val setupScript = """
                #!/data/data/com.termux/files/usr/bin/bash
                set -e
                REPO_URL="${'$'}{1:-https://YOUR_GITHUB_USERNAME.github.io/termux-studio-repo}"
                echo "deb [trusted=yes] ${'$'}{REPO_URL} stable main" > "${'$'}PREFIX/etc/apt/sources.list.d/termux-studio.list"
                pkg update -y
                pkg install -y termux-studio
                termux-studio link-storage
                termux-studio probe
            """.trimIndent() + "\n"
            File(repoRootDir, "install-termux-studio.sh").apply {
                writeText(setupScript)
                setExecutable(true)
            }

            val readme = """
                # termux-studio APT Repository

                Push the contents of this `termux-repo/` folder to a GitHub repository (with GitHub Pages enabled on the root), then install directly inside Termux:

                ```bash
                echo "deb [trusted=yes] https://<YOUR_USER>.github.io/<YOUR_REPO> stable main" > ${'$'}PREFIX/etc/apt/sources.list.d/termux-studio.list
                pkg update -y
                pkg install -y termux-studio
                ```

                Or install locally right away from your shared folder:

                ```bash
                pkg install -y ~/TermuxStudio/termux-repo/$debRelPath
                termux-studio link-storage && termux-studio probe
                ```
            """.trimIndent() + "\n"
            File(repoRootDir, "README.md").writeText(readme)

            Result.success(debFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class TarEntrySpec(
        val path: String,
        val content: ByteArray,
        val mode: Int
    )

    private fun buildTarGz(entries: List<TarEntrySpec>): ByteArray {
        val rawTar = ByteArrayOutputStream()
        val mtimeSeconds = System.currentTimeMillis() / 1000L

        entries.forEach { entry ->
            val header = ByteArray(512)
            writeAscii(header, 0, 100, entry.path)
            writeOctal(header, 100, 8, entry.mode.toLong())
            writeOctal(header, 108, 8, 0L)
            writeOctal(header, 116, 8, 0L)
            writeOctal(header, 124, 12, entry.content.size.toLong())
            writeOctal(header, 136, 12, mtimeSeconds)
            for (i in 148 until 156) header[i] = ' '.code.toByte()
            header[156] = '0'.code.toByte()
            writeAscii(header, 257, 6, "ustar\u0000")
            writeAscii(header, 263, 2, "00")
            writeAscii(header, 265, 32, "root")
            writeAscii(header, 297, 32, "root")

            var chksum = 0
            for (b in header) chksum += (b.toInt() and 0xFF)
            val chkStr = chksum.toString(8).padStart(6, '0')
            writeAscii(header, 148, 6, chkStr)
            header[154] = 0
            header[155] = ' '.code.toByte()

            rawTar.write(header)
            rawTar.write(entry.content)
            val pad = (512 - (entry.content.size % 512)) % 512
            if (pad > 0) rawTar.write(ByteArray(pad))
        }
        rawTar.write(ByteArray(1024))
        return gzipBytes(rawTar.toByteArray())
    }

    private fun buildArArchive(members: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("!<arch>\n".toByteArray(Charsets.US_ASCII))
        val mtime = (System.currentTimeMillis() / 1000L).toString()

        members.forEach { (name, bytes) ->
            val header = buildString {
                append(name.take(16).padEnd(16, ' '))
                append(mtime.take(12).padEnd(12, ' '))
                append("0".padEnd(6, ' '))
                append("0".padEnd(6, ' '))
                append("100644".padEnd(8, ' '))
                append(bytes.size.toString().padEnd(10, ' '))
                append("`\n")
            }
            out.write(header.toByteArray(Charsets.US_ASCII))
            out.write(bytes)
            if (bytes.size % 2 != 0) {
                out.write('\n'.code)
            }
        }
        return out.toByteArray()
    }

    private fun gzipBytes(bytes: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(bytes) }
        return baos.toByteArray()
    }

    private fun digestHex(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm).digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun writeAscii(buf: ByteArray, offset: Int, maxLen: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val count = minOf(bytes.size, maxLen)
        System.arraycopy(bytes, 0, buf, offset, count)
    }

    private fun writeOctal(buf: ByteArray, offset: Int, len: Int, value: Long) {
        val oct = value.toString(8).padStart(len - 1, '0')
        writeAscii(buf, offset, len - 1, oct)
        buf[offset + len - 1] = 0
    }
}
