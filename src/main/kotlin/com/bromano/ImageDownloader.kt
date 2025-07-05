package com.bromano

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * Downloads an image from a URL and saves it to the specified directory.
 *
 * @param imageUrl The URL of the image to download
 * @param outputDir The directory where the image should be saved
 * @param imageName The name to give the downloaded image (without extension)
 * @return The name of the downloaded file, or null if download failed
 */
fun downloadImage(
    imageUrl: String,
    outputDir: Path,
    imageName: String,
): File {
    // Determine file extension from URL or content type
    val extension =
        imageUrl
            .substringAfterLast('.', "")
            .takeIf { it.length < 5 }
            ?: "png"

    val outputFile = outputDir.resolve("$imageName.$extension").toFile()

    val client = OkHttpClient()
    val request = Request.Builder().url(imageUrl).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Failed to download image: ${response.code} - ${response.message}")
        }

        response.body?.byteStream()?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Response body is null")
    }

    return outputFile
}
