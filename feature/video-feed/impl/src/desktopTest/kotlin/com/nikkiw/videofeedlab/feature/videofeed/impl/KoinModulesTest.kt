package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.nikkiw.videofeedlab.shared.catalog.catalogModule
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinModulesTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifiesApplicationDependencyGraph() {
        module {
            includes(catalogModule, videoFeedModule)
        }.verify(extraTypes = listOf(HttpClientEngine::class))
    }
}
