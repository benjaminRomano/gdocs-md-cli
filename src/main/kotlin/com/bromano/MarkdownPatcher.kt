package com.bromano

import java.nio.file.Path
import kotlin.text.Regex

/**
 * Patch markdown to properly conform to `.md` format
 *
 * 1. Replace the embedded base64 images with either local or remote image references
 * 2. Strip out invalid header links {#...}
 * 3. Update Table of Contents with proper links format
 *
 * @param markdown The original markdown content
 * @param contentUris List of image URIs from the Google Doc
 * @param outputFile The output markdown file path (used to determine relative image paths)
 * @param imagesDir The directory to save downloaded images. If empty, content URIs will be patched in.
 * @return Processed markdown content
 */
fun patchMarkdown(
    markdown: String,
    contentUris: List<String>,
    outputFile: Path,
    imagesDir: Path? = null,
): String {
    var updatedMarkdown = markdown.substringBefore("[image1]: <data:image/png;base64,")

    contentUris.mapIndexed { index, uri ->
        val imageRef =
            if (imagesDir == null) {
                uri
            } else {
                val localImage = downloadImage(uri, imagesDir, "image${index + 1}")
                outputFile.parent
                    .relativize(imagesDir)
                    .resolve(localImage.name)
                    .toString()
                    .replace("\\", "/")
            }

        // Replace the image reference in the markdown
        updatedMarkdown =
            updatedMarkdown
                .replace("![][image${index + 1}]", "<img src=\"$imageRef\" />")
    }

    updatedMarkdown =
        processHeaders(updatedMarkdown).let {
            updateTableOfContents(it.first, it.second)
        }

    return updatedMarkdown
}

private fun processHeaders(content: String): Pair<String, Map<String, String>> {
    val anchorMap = mutableMapOf<String, String>()
    val headerPattern = "^(#+)\\s*(.*?)(?:\\s*\\{\\s*#([^}]+)\\s*})?\\s*$".toRegex(RegexOption.MULTILINE)

    val processedContent =
        headerPattern.replace(content) { match ->
            val (hashes, headerText, explicitAnchor) = match.destructured
            val anchor =
                explicitAnchor.ifEmpty {
                    // Generate anchor from header text if no explicit anchor is provided
                    headerText.trim()
                        .lowercase()
                        .replace("[^a-z0-9\\s-]".toRegex(), "") // Remove special chars
                        .replace("\\s+".toRegex(), "-") // Replace spaces with hyphens
                        .replace("-" + Regex("-" + "+") + "$".toRegex(), "") // Remove trailing hyphens
                }

            // Store the mapping for TOC updates
            anchorMap[headerText.trim()] = anchor

            // Return the header without the {#anchor} part
            "$hashes $headerText"
        }

    return Pair(processedContent, anchorMap)
}

/**
 * Updates table of contents links to use the new anchor format.
 */
private fun updateTableOfContents(
    content: String,
    anchorMap: Map<String, String>,
): String {
    val tocPattern = "\\[([^]]+)]\\(#([^)]+)\\)".toRegex()

    return content.lines().joinToString("\n") { line ->
        if (line.trimStart().startsWith("[")) {
            // This line might be a TOC entry
            tocPattern.replace(line) { match ->
                val (linkText, oldAnchor) = match.destructured
                val newAnchor =
                    anchorMap.entries.find {
                        it.key.equals(linkText, ignoreCase = true) ||
                            it.key.lowercase().replace("[^a-z0-9]+".toRegex(), "-") == oldAnchor
                    }?.value ?: oldAnchor

                "[$linkText](#$newAnchor)"
            }
        } else {
            line
        }
    }
}
