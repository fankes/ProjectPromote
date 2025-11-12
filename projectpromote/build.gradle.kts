plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = gropify.project.projectpromote.groupName
    compileSdk = gropify.project.android.compileSdk

    defaultConfig {
        minSdk = gropify.project.android.minSdk
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
}

dependencies {
    implementation(libs.markwon.core)
    implementation(libs.markwon.image)
    implementation(libs.markwon.html)
    implementation(libs.okhttp)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
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
    coordinates(gropify.project.projectpromote.groupName, gropify.project.projectpromote.moduleName, gropify.project.projectpromote.version)
    pom {
        name = gropify.project.name
        description = gropify.project.description
        url = gropify.project.url
        licenses {
            license {
                name = gropify.project.licence.name
                url = gropify.project.licence.url
                distribution = gropify.project.licence.url
            }
        }
        developers {
            developer {
                id = gropify.project.developer.id
                name = gropify.project.developer.name
                email = gropify.project.developer.email
            }
        }
    }
}