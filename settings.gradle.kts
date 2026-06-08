pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "humanos-android"

// ── App ──────────────────────────────────────────────────────────────────────
include(":app")

// ── Core ─────────────────────────────────────────────────────────────────────
include(":core:core-model")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-network")
include(":core:core-security")
include(":core:core-ui")
include(":core:core-observability")
include(":core:core-update")

// ── Data ─────────────────────────────────────────────────────────────────────
include(":data:data-auth")
include(":data:data-capture")
include(":data:data-tasks")

// ── Feature ──────────────────────────────────────────────────────────────────
include(":feature:feature-dashboard")
include(":feature:feature-capture")
include(":feature:feature-settings")
include(":feature:feature-tasks")

// ── Integrations ─────────────────────────────────────────────────────────────
include(":integrations:integration-humanos")
include(":integrations:integration-quebot")

// ── Testing ──────────────────────────────────────────────────────────────────
include(":testing:testing-common")
