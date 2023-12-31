pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
    id("com.highcapable.sweetproperty") version "1.0.5"
}
sweetProperty {
    global {
        sourcesCode { isEnable = false }
    }
    rootProject { all { isEnable = false } }
}
rootProject.name = "ProjectPromote"
include(":demo-app")
include(":projectpromote")