plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.lagradost.cloudstream3.gradle")
}

cloudstream {
    setDependencies(listOf("android-base"))
    language = "en"
    description = "Proveedor para MyBoyfriendTV"
    authors = listOf("Fade")
    status = 1
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=myboyfriend.tv&sz=%size%"
}

dependencies {
    implementation(kotlin("stdlib"))
}