// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
}
// Needed to make the Suppress annotation work for the plugins block
true