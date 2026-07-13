package com.nikkiw.videofeedlab.feature.videofeed.api

sealed interface CatalogLoadState {
    data object Loading : CatalogLoadState

    data object Content : CatalogLoadState

    data object Empty : CatalogLoadState

    data class Error(val message: String) : CatalogLoadState
}
