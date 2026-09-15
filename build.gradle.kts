plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.com.github.aerathstuff.zygoteloader) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
