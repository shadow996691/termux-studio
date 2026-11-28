package com.example.data.workspace

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

object TarGzExtractor {

    /**
     * Extracts a .tar.gz stream into [destDir], optionally stripping the first path segment
     * (e.g., "raylib-6.2_linux_arm64/") and materializing symlinks as file copies so they work
     * cleanly on Android shared storage (/sdcard) where POSIX symlinks are disallowed.
     */
    fun extractTarGz(
        inputStream: InputStream,
        destDir: File,
        stripFirstComponent: Boolean = true
    ): Result<Int> {
        return try {
            destDir.mkdirs()
            val pendingSymlinks = mutableListOf<Pair<File, String>>()
            var extractedCount = 0

            GZIPInputStream(BufferedInputStream(inputStream)).use { tarIn ->
                val header = ByteArray(512)
                while (true) {
                    val read = readFully(tarIn, header)
                    if (read < 512 || isAllZero(header)) break

                    val rawName = readString(header, 0, 100)
                    val prefix = readString(header, 345, 155)
                    val fullName = if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName
                    val typeFlag = header[156].toInt().toChar()
                    val size = parseOctal(header, 124, 12)
                    val linkName = readString(header, 157, 100)

                    val relPath = normalizeRelPath(fullName, stripFirstComponent)
                    val outFile = if (relPath.isNotEmpty()) File(destDir, relPath) else null

                    when (typeFlag) {
                        '5' -> {
                            outFile?.mkdirs()
                        }
                        '2', '1' -> {
                            // Symlink or hardlink: defer copy until regular files exist
                            if (outFile != null && linkName.isNotBlank()) {
                                pendingSymlinks.add(outFile to linkName)
                            }
                        }
                        '0', '\u0000' -> {
                            if (outFile != null && !relPath.contains("..")) {
                                outFile.parentFile?.mkdirs()
                                outFile.outputStream().use { out ->
                                    copyExact(tarIn, out, size)
                                }
                                if ((parseOctal(header, 100, 8) and 0b001_000_000L) != 0L) {
                                    outFile.setExecutable(true)
                                }
                                extractedCount++
                            } else {
                                skipExact(tarIn, size)
                            }
                        }
                        else -> {
                            skipExact(tarIn, size)
                        }
                    }

                    // Skip 512-byte block padding
                    if (typeFlag != '5' && size > 0L) {
                        val rem = (512L - (size % 512L)) % 512L
                        if (rem > 0L) skipExact(tarIn, rem)
                    }
                }
            }

            // Materialize symlinks safely on /sdcard
            repeat(2) {
                pendingSymlinks.forEach { (linkFile, linkTarget) ->
                    val targetFile = File(linkFile.parentFile ?: destDir, linkTarget)
                    if (targetFile.exists() && (!linkFile.exists() || linkFile.length() != targetFile.length())) {
                        linkFile.parentFile?.mkdirs()
                        targetFile.copyTo(linkFile, overwrite = true)
                        extractedCount++
                    }
                }
            }

            Result.success(extractedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeRelPath(raw: String, stripFirst: Boolean): String {
        val cleaned = raw.trimStart('.', '/').trimEnd('/')
        if (cleaned.isEmpty()) return ""
        val parts = cleaned.split('/').filter { it.isNotEmpty() && it != "." }
        val sliced = if (stripFirst && parts.size > 1) parts.drop(1) else if (stripFirst && parts.size == 1) emptyList() else parts
        return sliced.joinToString("/")
    }

    private fun readFully(stream: InputStream, buf: ByteArray): Int {
        var offset = 0
        while (offset < buf.size) {
            val n = stream.read(buf, offset, buf.size - offset)
            if (n <= 0) break
            offset += n
        }
        return offset
    }

    private fun copyExact(input: InputStream, out: java.io.OutputStream, total: Long) {
        var remaining = total
        val buffer = ByteArray(8192)
        while (remaining > 0) {
            val toRead = minOf(remaining, buffer.size.toLong()).toInt()
            val n = input.read(buffer, 0, toRead)
            if (n <= 0) break
            out.write(buffer, 0, n)
            remaining -= n
        }
    }

    private fun skipExact(input: InputStream, total: Long) {
        var remaining = total
        val buffer = ByteArray(4096)
        while (remaining > 0) {
            val n = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (n <= 0) break
            remaining -= n
        }
    }

    private fun isAllZero(buf: ByteArray): Boolean = buf.all { it == 0.toByte() }

    private fun readString(buf: ByteArray, offset: Int, length: Int): String {
        val end = (offset until minOf(offset + length, buf.size)).firstOrNull { buf[it] == 0.toByte() }
            ?: minOf(offset + length, buf.size)
        return String(buf, offset, end - offset, Charsets.UTF_8).trim()
    }

    private fun parseOctal(buf: ByteArray, offset: Int, length: Int): Long {
        val str = readString(buf, offset, length)
        if (str.isEmpty()) return 0L
        return str.toLongOrNull(8) ?: 0L
    }
}
