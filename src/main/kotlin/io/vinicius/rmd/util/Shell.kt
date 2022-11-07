package io.vinicius.rmd.util

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Shell(private val directory: File) {
    init {
        if (!directory.exists()) directory.mkdirs()
    }

    fun downloadImage(url: String, output: String): Boolean {
        val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:106.0) Gecko/20100101 Firefox/106.0"
        val dest = File(directory, output)
        return runCommand("wget -U \"$userAgent\" $url -O ${dest.absoluteFile}").isSuccess
    }

    fun downloadVideo(url: String, output: String): Boolean {
        val dest = File(directory, output)
        return runCommand("youtube-dl $url -o ${dest.absoluteFile}").isSuccess
    }

    fun calculateHash(fileName: String): String? {
        val file = File(directory, fileName)

        return if (file.exists()) {
            runCommand("sha256sum ${file.absoluteFile}").getOrNull()?.take(64)
        } else {
            null
        }
    }

    private fun runCommand(
        command: String,
        workingDir: File = File("."),
        timeout: Duration = 60.minutes
    ): Result<String> = try {
        val parts = command.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)

        if (proc.exitValue() == 0) {
            Result.success(proc.inputStream.bufferedReader().readText())
        } else {
            Result.failure(IOException(proc.inputStream.bufferedReader().readText()))
        }
    } catch (e: IOException) {
        Result.failure(e)
    }
}