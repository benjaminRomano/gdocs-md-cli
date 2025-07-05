package com.bromano

import com.bromano.drive.AuthMethod
import com.bromano.drive.GoogleServices
import com.bromano.drive.GoogleServices.extractFileId
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import patchMarkdown

fun main(args: Array<String>) = GoogleDocsToMarkdown().main(args)

/**
 * Convert a Google doc into Markdown
 */
class GoogleDocsToMarkdown : CliktCommand(
    name = "gdocs-md",
) {
    private val input by option("--file-id", "-f", help = "Google Docs file ID or URL (e.g. 'https://docs.google.com/document/d/...')")
        .required()

    private val output by option("--output", "-o", help = "Output file path")
        .path(mustExist = false, canBeDir = false)
        .required()

    private val authMethod by option(
        "--auth",
        "-a",
        help = "Authentication method",
    ).choice("adc" to AuthMethod.ADC, "oauth" to AuthMethod.OAUTH)
        .default(AuthMethod.ADC)

    override fun run() {
        val fileId = extractFileId(input)

        echo("Processing document with ID: $fileId")

        val drive = GoogleServices.createDriveClient(authMethod)
        val docs = GoogleServices.createDocsClient(authMethod)

        val markdown =
            try {
                drive.files().export(fileId, "text/markdown")
                    .executeMediaAsInputStream()
                    .use { stream ->
                        val content = stream.readAllBytes().toString(Charsets.UTF_8)
                        // Remove all the base64 encoded strings
                        content.substringBefore("[image1]: <data:image/png;base64,")
                    }
            } catch (e: Exception) {
                throw Exception("Failed to export document: ${e.message}", e)
            }

        val doc =
            try {
                docs.documents().get(fileId).execute()
            } catch (e: Exception) {
                throw Exception("Failed to fetch document: ${e.message}", e)
            }

        val contentUris =
            doc.body.content.flatMap { structuralElement ->
                structuralElement.paragraph?.elements
                    ?.mapNotNull { pe ->
                        pe.inlineObjectElement
                            ?.inlineObjectId
                            ?.let { id ->
                                doc.inlineObjects[id]?.inlineObjectProperties?.embeddedObject?.imageProperties?.contentUri
                            }
                    } ?: emptyList()
            }

        output.toFile().writeText(patchMarkdown(markdown, contentUris))

        echo("Successfully converted document to markdown")
    }
}
