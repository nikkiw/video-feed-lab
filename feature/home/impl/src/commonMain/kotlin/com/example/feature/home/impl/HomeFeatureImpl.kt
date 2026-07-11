package com.example.feature.home.impl

import com.example.feature.home.api.HomeFeatureApi

/**
 * Default implementation of [HomeFeatureApi].  Note that this class can
 * optionally depend on other shared libraries or platform‑specific code.  It
 * resides in the multiplatform module so that the same implementation
 * applies on all supported platforms.
 */
class HomeFeatureImpl : HomeFeatureApi {
    override fun getHomeGreeting(): String = "Welcome to the Home feature!"
}
