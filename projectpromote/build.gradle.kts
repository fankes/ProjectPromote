plugins {
    autowire(libs.plugins.android.library)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.maven.publish)
}

android {
    namespace = property.project.projectpromote.groupName
    compileSdk = property.project.android.compileSdk

    defaultConfig {
        minSdk = property.project.android.minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
}

dependencies {
    implementation(io.noties.markwon.core)
    implementation(io.noties.markwon.image)
    implementation(io.noties.markwon.html)
    implementation(com.squareup.okhttp3.okhttp)
    implementation(androidx.lifecycle.lifecycle.runtime.ktx)
    implementation(androidx.core.core.ktx)
    implementation(androidx.appcompat.appcompat)
    implementation(com.google.android.material.material)
    testImplementation(junit.junit)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.espresso.espresso.core)
}

publishing {
    repositories {
        val repositoryDir = gradle.gradleUserHomeDir
            .resolve("fankes-maven-repository")
            .resolve("repository")
        maven {
            name = "FankesMavenReleases"
            url = repositoryDir.resolve("releases").toURI()
        }
        maven {
            name = "FankesMavenSnapShots"
            url = repositoryDir.resolve("snapshots").toURI()
        }
    }
}

mavenPublishing {
    coordinates(property.project.projectpromote.groupName, property.project.projectpromote.moduleName, property.project.projectpromote.version)
    pom {
        name = property.project.name
        description = property.project.description
        url = property.project.url
        licenses {
            license {
                name = property.project.licence.name
                url = property.project.licence.url
                distribution = property.project.licence.url
            }
        }
        developers {
            developer {
                id = property.project.developer.id
                name = property.project.developer.name
                email = property.project.developer.email
            }
        }
    }
}