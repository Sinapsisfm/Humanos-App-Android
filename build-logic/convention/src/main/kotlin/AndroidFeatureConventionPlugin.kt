import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("humanos.android.library")
                apply("humanos.compose")
                apply("humanos.hilt")
            }

            dependencies {
                add("implementation", project(":core:core-ui"))
                add("implementation", project(":core:core-model"))
            }
        }
    }
}
