import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "com.liulkovich.florapoint.shared"
        compileSdk = 36
        minSdk = 24

        androidResources.enable = true

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }


    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ForestPointShared"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
    }


    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtimeSaveable)
            implementation(libs.navigation.compose)
            implementation(compose.components.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine.defaults)
            implementation(libs.kotlinx.serialization.json)
        }

        iosMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation("androidx.sqlite:sqlite-bundled:2.6.2")
        }

    }
}


val sharedPhotoDirectory =
    layout.buildDirectory.dir("generated/forestpointPhotos")


val prepareSharedPhotoResources =
    tasks.register<Sync>("prepareSharedPhotoResources") {

        // Сохраняем все существующие общие ресурсы:
        // переводы, иконки категорий и другие файлы.
        from(
            layout.projectDirectory.dir(
                "src/commonMain/composeResources"
            )
        )

        // Добавляем фотографии из оригинального Android-проекта.
        from(
            rootProject.layout.projectDirectory.dir(
                "app/src/main/res/drawable"
            )
        ) {
            include("*.webp")
            into("drawable")
        }

        into(sharedPhotoDirectory)
    }




compose.resources {
    packageOfResClass =
        "com.liulkovich.florapoint.shared.generated.resources"

    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = prepareSharedPhotoResources.map {
            sharedPhotoDirectory.get()
        }
    )
}
