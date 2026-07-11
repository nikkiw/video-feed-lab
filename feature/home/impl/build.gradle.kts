plugins {
    id("com.example.feature-impl-convention")
}

// The implementation module automatically depends on the corresponding API
// module thanks to the convention plugin.  Additional dependencies specific to
// this implementation (e.g. Compose UI components) can be declared here.
