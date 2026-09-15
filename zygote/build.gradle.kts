import com.v7878.zygisk.gradle.ZygoteLoader

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.com.github.aerathstuff.zygoteloader)
}

android {
    namespace = "io.github.rem01gaming.netswitch.zygote"
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

zygisk {
    // system_server：装 ZygoteHook，剔除 INET_GID
    packages(ZygoteLoader.PACKAGE_SYSTEM_SERVER)
    // 全部 App 进程：装 InetExceptionPatchHook，改写 DNS 异常
    packages(ZygoteLoader.ALL_PACKAGES)

    id = "net_switch_zygisk"
    name = "Net Switch Zygisk"
    author = "Rem01Gaming, Antonio-Riccio + INET_GID patch"
    description = "INET_GID strip backend for net-switch (isolated.json driven)"
    entrypoint = "io.github.rem01gaming.netswitch.zygote.ZygoteEntry"
    archiveName = "net-switch-ZYGISK-${android.defaultConfig.versionName}"
    isAddVariantToArchiveName = true
}

dependencies {
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.io.github.vova7878.r8annotations)

    implementation(androidvmtools.panama.core)
    implementation(androidvmtools.panama.unsafe)
    implementation(androidvmtools.panama.llvm)
    implementation(androidvmtools.sun.cleaner)
}
