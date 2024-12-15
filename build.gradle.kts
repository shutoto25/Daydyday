// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin) apply false
    // TODO tureにするとタスクが通るがfalseとの違いがわからない https://blog.mrhaki.com/2016/09/gradle-goodness-add-but-do-not-apply.html
    alias(libs.plugins.dokka) apply true
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.dagger.hilt) apply false
}
// Needed to make the Suppress annotation work for the plugins block
true