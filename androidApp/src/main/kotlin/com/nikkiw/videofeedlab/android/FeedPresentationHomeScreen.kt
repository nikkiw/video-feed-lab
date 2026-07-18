package com.nikkiw.videofeedlab.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedPresentationId

@Composable
internal fun FeedPresentationHomeScreen(
    presentations: List<FeedPresentationDescriptor>,
    onPresentationSelected: (FeedPresentationId) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Video Feed Lab",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a feed presentation to run.",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                presentations.forEach { presentation ->
                    FeedPresentationCard(
                        presentation = presentation,
                        onOpen = { onPresentationSelected(presentation.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPresentationCard(
    presentation: FeedPresentationDescriptor,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            FeedPreview(presentation.id)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Medium,
                )
                PresentationStatusBadge(presentation.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = presentation.description,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (presentation.isAvailable) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open")
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Planned")
                }
            }
        }
    }
}

@Composable
private fun FeedPreview(presentationId: FeedPresentationId) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        val label =
            when (presentationId) {
                AndroidFeedPresentations.VerticalSnap -> "9:16 Vertical"
                AndroidFeedPresentations.HorizontalPageTurn -> "←  Page turn  →"
                else -> presentationId.value
            }

        Text(
            text = label,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun PresentationStatusBadge(status: FeedPresentationStatus) {
    val label =
        when (status) {
            FeedPresentationStatus.Stable -> "Stable"
            FeedPresentationStatus.Experimental -> "Experimental"
            FeedPresentationStatus.Planned -> "Planned"
        }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.primary,
        )
    }
}
