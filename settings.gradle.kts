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

rootProject.name = "cia-android"

include(":app")
include(":cia-kit")
include(":core")
include(":designsystem")
include(":feature:home")
include(":feature:paymentselector")
include(":feature:link")
include(":feature:movements")
include(":feature:detail")
include(":feature:qr")
include(":feature:cpf")
include(":feature:congrats")
include(":feature:inputamount")
include(":feature:validatelimit")
