pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "Seeker-Store"
include(":app")

include(":utilities:notes")
include(":utilities:tasks")
include(":utilities:calculator")
include(":utilities:files")
include(":utilities:music")
include(":utilities:gallery")
include(":utilities:device")
include(":utilities:backup")
include(":utilities:vault")
