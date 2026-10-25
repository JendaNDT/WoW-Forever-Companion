package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class E12BuildIdentityTest {
    @Test fun packagedIdentityBindsProgramToCatalogPublisherAndSourceInputs(){
        val c=ApplicationProvider.getApplicationContext<Context>()
        val meta=Json.parseToJsonElement(c.assets.open("release-info.json").use{it.readBytes().decodeToString()}).jsonObject
        fun hash(path:String)=MessageDigest.getInstance("SHA-256").digest(c.assets.open(path).use{it.readBytes()}).joinToString(""){"%02x".format(it)}
        assertEquals(BuildConfig.VERSION_CODE,meta["versionCode"]!!.jsonPrimitive.int);assertEquals(BuildConfig.SOURCE_REVISION,meta["sourceRevision"]!!.jsonPrimitive.content)
        assertEquals(BuildConfig.BUILD_INPUTS_SHA256,meta["buildInputsSha256"]!!.jsonPrimitive.content)
        assertTrue(BuildConfig.SOURCE_REVISION.matches(Regex("[a-f0-9]{40}")))
        assertEquals(hash("content/starter.json"),meta["fallbackCatalogSha256"]!!.jsonPrimitive.content)
        assertEquals(hash("content/publisher.der"),meta["publisherKeySha256"]!!.jsonPrimitive.content)
        assertEquals("e422650dc10efbf7074ae880c0cbc0d7e6d09c8d3ba6a1b1e761f99f441c2f6b",meta["ownerCertificateSha256"]!!.jsonPrimitive.content)
    }
}
