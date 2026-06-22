val requiredJdk = JavaVersion.VERSION_21
check(JavaVersion.current() == requiredJdk) {
    "lolive requires JDK 21. Current JDK: ${System.getProperty("java.version")} (${JavaVersion.current()})"
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lolive"
include(":app")
