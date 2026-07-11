package com.example.feature.home.impl

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeatureImplTest {
    @Test
    fun getHomeGreetingReturnsTemplateMessage() {
        assertEquals("Welcome to the Home feature!", HomeFeatureImpl().getHomeGreeting())
    }
}
