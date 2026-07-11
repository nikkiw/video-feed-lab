package com.example.shared

/**
 * A simple shared API that returns a greeting.  The implementation lives in
 * [shared/src/commonMain] and is available to Android, desktop and web
 * platforms.
 */
class Greeting {
    fun greet(): String = "Hello from shared code!"
}
