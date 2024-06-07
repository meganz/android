package mega.privacy.android.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.androidx
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("androidx")

val Project.testlib
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("testlib")

val Project.lib
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("lib")

val Project.google
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("google")