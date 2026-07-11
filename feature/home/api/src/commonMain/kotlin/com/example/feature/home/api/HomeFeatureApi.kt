package com.example.feature.home.api

/**
 * Contract for the Home feature.  By placing only interfaces and data types in
 * this module, we ensure that other modules can depend on the Home feature
 * without pulling in its implementation or transitive dependencies.  A simple
 * facade method is provided as an example.
 */
interface HomeFeatureApi {
    /**
     * Returns a greeting specific to the Home feature.
     */
    fun getHomeGreeting(): String
}
