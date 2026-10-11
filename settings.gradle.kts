pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral(); maven("https://maven.mozilla.org/maven2/") { content { includeGroup("org.mozilla.geckoview") } } } }
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

include(":utilities:appmanager", ":utilities:scanner")
include(":utilities:weather")
include(":utilities:camera")
include(":utilities:fitness")
include(":utilities:messaging")
include(":utilities:translation")
include(":utilities:maps")
include(":utilities:browser")
