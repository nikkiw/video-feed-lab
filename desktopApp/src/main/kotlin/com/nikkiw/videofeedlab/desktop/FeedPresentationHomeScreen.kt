package com.nikkiw.videofeedlab.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
                    .padding(horizontal = 40.dp, vertical = 32.dp),
        ) {
            Text(
                text = "Video Feed Lab",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a feed presentation to run.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
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
        modifier = Modifier.width(340.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            FeedPreview(presentation.id)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                PresentationStatusBadge(presentation.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = presentation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (presentation.isAvailable) {
                Button(onClick = onOpen) {
                    Text("Open")
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
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
                .height(180.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        val label =
            when (presentationId) {
                DesktopFeedPresentations.VerticalSnap -> "9:16\nVertical"
                DesktopFeedPresentations.HorizontalPageTurn -> "←  Page turn  →"
                else -> presentationId.value
            }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
