pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
plugins {
    id("com.highcapable.gropify") version "1.0.0"
}
gropify {
    global {
        android { isEnabled = false }
    }
    rootProject { common { isEnabled = false } }
}
rootProject.name = "ProjectPromote"
include(":demo-app")
include(":projectpromote")