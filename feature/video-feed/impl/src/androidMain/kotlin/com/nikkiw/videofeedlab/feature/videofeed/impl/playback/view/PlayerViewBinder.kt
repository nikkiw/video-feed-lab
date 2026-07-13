package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.view

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

internal interface PlayerViewBinder {
    fun bind(
        index: Int,
        view: PlayerView,
        player: Player?,
    )

    fun unbind(
        index: Int,
        view: PlayerView,
    )

    fun release()
}

@OptIn(UnstableApi::class)
internal class Media3PlayerViewBinder : PlayerViewBinder {
    private val views = mutableMapOf<Int, PlayerView>()
    private var released = false

    override fun bind(
        index: Int,
        view: PlayerView,
        player: Player?,
    ) {
        if (released) return
        views[index]?.takeIf { it !== view }?.let(::detach)
        views[index] = view
        if (view.player !== player) switchPlayer(index, view, player)
    }

    private fun switchPlayer(
        index: Int,
        view: PlayerView,
        player: Player?,
    ) {
        if (player == null) {
            detach(view)
        } else {
            val previousView =
                views.entries
                    .firstOrNull { (otherIndex, candidate) ->
                        otherIndex != index && candidate.player === player
                    }?.value
            PlayerView.switchTargetView(player, previousView, view)
        }
    }

    private fun detach(view: PlayerView) {
        view.player?.let { PlayerView.switchTargetView(it, view, null) }
    }

    override fun unbind(
        index: Int,
        view: PlayerView,
    ) {
        if (released || views[index] !== view) return
        view.player?.let { PlayerView.switchTargetView(it, view, null) }
        views.remove(index)
    }

    override fun release() {
        if (released) return
        released = true
        views.values.forEach { view ->
            view.player?.let { PlayerView.switchTargetView(it, view, null) }
        }
        views.clear()
    }
}
