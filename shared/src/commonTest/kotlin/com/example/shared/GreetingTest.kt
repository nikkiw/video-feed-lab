package com.example.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun greetReturnsTemplateMessage() {
        assertEquals("Hello from shared code!", Greeting().greet())
    }
}
