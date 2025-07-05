package com.bromano.drive

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.DocsScopes
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.FileNotFoundException

enum class AuthMethod {
    // Application Default Credentials
    ADC,

    // OAuth flow, which requires `credentials.json` and Oauth project to be registered
    OAUTH,
}

/**
 * Utilities to create a Google Drive Client
 *
 * Kotlin conversion of https://developers.google.com/workspace/drive/api/quickstart/java
 */
object GoogleServices {
    private const val APPLICATION_NAME = "Google Docs Markdown Converter"
    private val CREDENTIALS_FILE_PATH = "${System.getProperty("user.home")}/.gdocs-md-cli/credentials.json"
    private val TOKENS_DIRECTORY_PATH = "${System.getProperty("user.home")}/.gdocs-md-cli/tokens"

    val SCOPES = listOf(DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA_READONLY, DocsScopes.DOCUMENTS_READONLY)

    private val JSON_FACTORY = GsonFactory.getDefaultInstance()

    /**
     * Perform OAuth flow and cache credentials in Token directory
     */
    private fun getOAuthCredentials(transport: NetHttpTransport): Credential {
        val credentialsFile = java.io.File(CREDENTIALS_FILE_PATH)
        if (!credentialsFile.exists()) {
            throw FileNotFoundException("Credentials file not found at: $CREDENTIALS_FILE_PATH")
        }

        val clientSecrets =
            GoogleClientSecrets.load(
                JSON_FACTORY,
                credentialsFile.reader(),
            )

        val flow =
            GoogleAuthorizationCodeFlow.Builder(
                transport,
                JSON_FACTORY,
                clientSecrets,
                SCOPES,
            )
                .setDataStoreFactory(FileDataStoreFactory(java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build()

        val receiver =
            LocalServerReceiver.Builder()
                .setPort(8888)
                .build()

        return AuthorizationCodeInstalledApp(flow, receiver)
            .authorize("user")
    }

    private fun getCredentials(
        authMethod: AuthMethod,
        transport: NetHttpTransport,
    ): HttpRequestInitializer {
        return when (authMethod) {
            AuthMethod.ADC -> HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault().createScoped(SCOPES))
            AuthMethod.OAUTH -> getOAuthCredentials(transport)
        }
    }

    fun createDriveClient(authMethod: AuthMethod): Drive {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        return Drive.Builder(transport, JSON_FACTORY, getCredentials(authMethod, transport))
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun createDocsClient(authMethod: AuthMethod): Docs {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        return Docs.Builder(transport, JSON_FACTORY, getCredentials(authMethod, transport))
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    fun extractFileId(input: String): String {
        // If it's a URL, extract the file ID
        val urlPattern = "^https://docs\\.google\\.com/document/d/([a-zA-Z0-9-_]+).*".toRegex()
        return urlPattern.matchEntire(input)?.groupValues?.get(1) ?: input
    }
}
