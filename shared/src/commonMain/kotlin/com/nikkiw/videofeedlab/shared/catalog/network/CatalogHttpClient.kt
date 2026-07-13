package com.nikkiw.videofeedlab.shared.catalog.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createCatalogJson(): Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

fun createCatalogHttpClient(json: Json): HttpClient =
    HttpClient {
        configureCatalogHttpClient(json)
    }

fun HttpClientConfig<*>.configureCatalogHttpClient(json: Json) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
    }
    defaultRequest {
        accept(ContentType.Application.Json)
    }
}

private const val CONNECT_TIMEOUT_MS = 5_000L
private const val REQUEST_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 15_000L
