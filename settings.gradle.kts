pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.3"
    id("com.highcapable.sweetproperty") version "1.0.4"
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