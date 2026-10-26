import java.net.URI
import java.security.MessageDigest
import java.security.KeyStore
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}
val releaseCode=18
val releaseVersion="0.17.0-ui1"
fun digest(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
val inputPrefixes=listOf("app/src/main/","app/src/release/","gradle/wrapper/")
val inputNames=setOf("app/build.gradle.kts","build.gradle.kts","settings.gradle.kts","gradle.properties","gradlew","gradlew.bat","release/owner-certificate.sha256")
val buildInputs=rootProject.fileTree(rootProject.projectDir) {
    inputPrefixes.forEach { include(it+"**") };inputNames.forEach { include(it) }
}.files.sortedBy { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath }
val inputManifest=buildInputs.joinToString("") { it.relativeTo(rootProject.projectDir).invariantSeparatorsPath+"\u0000"+digest(it.readBytes())+"\n" }
val inputHash=digest(inputManifest.toByteArray(Charsets.UTF_8))
fun gitOutput(vararg args:String):String?=runCatching {
    val p=ProcessBuilder(listOf("git","-C",rootProject.projectDir.path)+args).redirectErrorStream(true).start()
    val value=p.inputStream.bufferedReader().readText().trim();if(p.waitFor()==0)value else null
}.getOrNull()
val ownGit=gitOutput("rev-parse","--show-toplevel")?.let { file(it).canonicalFile==rootProject.projectDir.canonicalFile }==true
val archiveState=runCatching { JsonSlurper().parse(rootProject.file("source-state.json")) as Map<*,*> }.getOrNull()
val sourceRevision=if(ownGit)gitOutput("rev-parse","HEAD").orEmpty() else archiveState?.get("sourceRevision")?.toString().orEmpty()
val dirtyInputs=if(ownGit)gitOutput("status","--porcelain","--untracked-files=all","--",*(inputPrefixes+inputNames).toTypedArray())?.isNotBlank()!=false
    else archiveState?.get("buildInputsSha256")!=inputHash
val identity=mapOf("schema" to 1,"applicationId" to "cz.jenda.wowforever.guide","versionCode" to releaseCode,"versionName" to releaseVersion,
    "channel" to "pilot","sourceRevision" to sourceRevision,"sourceDirty" to dirtyInputs,"buildInputsSha256" to inputHash,
    "fallbackCatalogSha256" to digest(file("src/main/assets/content/starter.json").readBytes()),
    "publisherKeySha256" to digest(file("src/main/assets/content/publisher.der").readBytes()),
    "ownerCertificateSha256" to rootProject.file("release/owner-certificate.sha256").readText().trim())
val identityDir=layout.buildDirectory.dir("generated/releaseIdentity")
val generateReleaseIdentity=tasks.register("generateReleaseIdentity") {
    inputs.files(buildInputs);inputs.property("sourceRevision",sourceRevision);inputs.property("sourceDirty",dirtyInputs)
    outputs.dir(identityDir)
    doLast { identityDir.get().asFile.apply { mkdirs() }.resolve("release-info.json").writeText(JsonOutput.toJson(identity)+"\n") }
}
val checkReleasePrerequisites=tasks.register("checkReleasePrerequisites") {
    doLast {
        val store=System.getenv("FOREVER_SIGNING_STORE").orEmpty()
        val password=System.getenv("FOREVER_SIGNING_PASSWORD").orEmpty()
        check(store.isNotBlank() && password.isNotBlank()) { "Release vyžaduje původní vlastnický keystore a heslo. Vývojový podpis nesmí nahradit podpis vydání." }
        val keyStore=runCatching { KeyStore.getInstance(file(store),password.toCharArray()) }.getOrNull()
        val certificate=keyStore?.getCertificate("forever")
        check(certificate!=null && keyStore?.isKeyEntry("forever")==true) { "Vlastnický podpis nebyl načtený. Ověř samostatně uložené původní klíče E09." }
        check(digest(certificate.encoded)==rootProject.file("release/owner-certificate.sha256").readText().trim()) { "Cizí certifikát. Release musí zachovat podpis E09 a novějších verzí." }
        check(sourceRevision.matches(Regex("[a-f0-9]{40}")) && !dirtyInputs) { "Release vyžaduje uložený čistý zdrojový kód nebo ověřený zdrojový archiv." }
    }
}
tasks.configureEach {
    if(name in setOf("packageRelease","assembleRelease","bundleRelease"))dependsOn(checkReleasePrerequisites)
    if(name=="preBuild")dependsOn(generateReleaseIdentity)
}

android {
    namespace = "cz.jenda.wowforever"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
    // WFP already contains compressed PNGs. Keep it mmap/stream friendly inside the APK.
    androidResources { noCompress += listOf("wfp","wfpart") }
    defaultConfig {
        applicationId = "cz.jenda.wowforever.guide"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseCode
        versionName = releaseVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String","SOURCE_REVISION",JsonOutput.toJson(sourceRevision))
        buildConfigField("String","BUILD_INPUTS_SHA256",JsonOutput.toJson(inputHash))
    }
    signingConfigs {
        if(System.getenv("FOREVER_SIGNING_STORE")!=null)create("owner") {
            storeFile=file(System.getenv("FOREVER_SIGNING_STORE"))
            storePassword=System.getenv("FOREVER_SIGNING_PASSWORD")
            keyAlias="forever";keyPassword=System.getenv("FOREVER_SIGNING_PASSWORD")
        }
    }
    buildTypes {
        debug { signingConfigs.findByName("owner")?.let{signingConfig=it} }
        release { isMinifyEnabled = false;signingConfigs.findByName("owner")?.let{signingConfig=it} }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets {
        getByName("main").assets.srcDir(identityDir)
        getByName("test").java.srcDir("src/sharedTest/java")
        getByName("test").resources.srcDir("src/main/assets/content")
        getByName("androidTest").java.srcDir("src/sharedTest/java")
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("ovh.plrapps:mapcompose:3.2.0")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("androidx.datastore:datastore:1.1.7")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

tasks.withType<Test>().configureEach {
    maxHeapSize=providers.gradleProperty("forever.testHeap").orElse("2g").get()
    if(providers.gradleProperty("forever.testHeap").isPresent)forkEvery=1
    maxParallelForks=1
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
    // Honor a developer/CI network proxy for Robolectric's separate test JVM, when present.
    providers.environmentVariable("HTTPS_PROXY").orNull?.let { value ->
        val proxy = URI(value)
        proxy.host?.let { host ->
            systemProperty("https.proxyHost", host)
            systemProperty("https.proxyPort", (if (proxy.port > 0) proxy.port else 80).toString())
        }
    }
}
