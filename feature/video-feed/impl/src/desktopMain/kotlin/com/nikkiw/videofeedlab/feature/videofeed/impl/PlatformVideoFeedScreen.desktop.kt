package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.nikkiw.videofeedlab.feature.videofeed.api.CatalogLoadState
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent

@Composable
internal actual fun PlatformVideoFeedScreen(
    component: VideoFeedComponent,
    onBack: (() -> Unit)?,
) {
    val model by component.models.subscribeAsState()
    when (val loadState = model.catalogLoadState) {
        CatalogLoadState.Content -> DesktopVideoFeedContent(component, model, onBack)
        CatalogLoadState.Loading -> CatalogStatusScreen(message = "Loading video catalog…", showProgress = true)
        CatalogLoadState.Empty -> CatalogStatusScreen(message = "No videos found")
        is CatalogLoadState.Error -> CatalogStatusScreen(message = loadState.message, onRetry = component::onRetryLoad)
    }
}

@Composable
private fun CatalogStatusScreen(
    message: String,
    showProgress: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showProgress) CircularProgressIndicator()
            Text(message)
            onRetry?.let { retry ->
                Button(onClick = retry) {
                    Text("Retry")
                }
            }
        }
    }
}
