package com.terkula.uaxctf.google

import com.fasterxml.jackson.core.JsonFactory
import com.google.api.client.auth.oauth2.Credential
import com.google.api.services.sheets.v4.SheetsScopes
import java.util.*
import java.util.Collections.singletonList
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import java.io.InputStreamReader
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import java.io.FileNotFoundException
import java.io.IOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import java.io.File
import com.google.api.services.sheets.v4.Sheets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import org.springframework.stereotype.Component

@Component
class GoogleSheetsClient {

    private val SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY)
    private val CREDENTIALS_FILE_PATH = "/credentials.json"
    private val APPLICATION_NAME = "Google Sheets API Java Quickstart"
    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
    private val TOKENS_DIRECTORY_PATH = "tokens"


    @Throws(IOException::class)
    private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
        // Load client secrets.
        val inputStream = GoogleSheetsClient::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
                ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")

        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

        // Build flow and trigger user authorization request.

        val flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build()

       val receiver = LocalServerReceiver.Builder()
               .setPort(8888)
               .build()

        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun readSheet(sheetId: String, sheetName: String): List<List<Any>> {

        // Build a new authorized API client service.
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

        val range = "$sheetName!A2:E"

        val service = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build()


        val response = service.spreadsheets().values()
                .get(sheetId, range)
                .execute()

        return response.getValues()
    }

}