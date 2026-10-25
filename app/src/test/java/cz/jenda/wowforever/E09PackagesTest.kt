package cz.jenda.wowforever

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.*
import cz.jenda.wowforever.data.db.*
import cz.jenda.wowforever.packages.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.net.URI
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class E09PackagesTest {
    private val c=ApplicationProvider.getApplicationContext<Context>()
    private fun manager(free:()->Long={Long.MAX_VALUE},fault:(String)->Unit={})=PackageManager(c,E09Fixture.pair.public.encoded,free,fault)
    @Before fun clean(){File(c.filesDir,"content-generations").deleteRecursively()}
    @After fun after(){File(c.filesDir,"content-generations").deleteRecursively()}
    private fun reject(action:suspend()->Unit)=runBlocking {var rejected=false;try{action()}catch(_:Exception){rejected=true};assertTrue("Invalid package was accepted",rejected)}
    @Test fun signedGenerationPersistsAndRollbackResetNeverAlterPersonalData()=runBlocking {
        val personal=Room.inMemoryDatabaseBuilder(c,PersonalDatabase::class.java).build()
        try {
            personal.personal().createProfile(ProfileEntity());val journal=PersonalRepository(personal)
            journal.setQuest(DEFAULT_PROFILE_ID,"wow-forever:quest:788","Cutting Teeth",QuestState.COMPLETED)
            journal.addStop(DEFAULT_PROFILE_ID,"wow-forever:npc:5765","Ruzan")
            journal.setTravel(DEFAULT_PROFILE_ID,"wow-forever:travel:historical","Objevený bod",true)
            val before=journal.profileBackup(DEFAULT_PROFILE_ID)
            val m=manager();m.initialize();val candidate=m.stage(E09Fixture.zip(c).inputStream())
            assertEquals(2,m.status().generation);m.activate(candidate)
            assertEquals(3,m.status().generation);assertNotNull(m.databaseFile())
            val installed=Room.databaseBuilder(c,ContentDatabase::class.java,m.databaseFile()!!.absolutePath).build()
            try{assertEquals(3,installed.catalog().metadata()!!.generation);assertEquals(53,installed.catalog().entries().size)}finally{installed.close()}
            var reopened=manager();reopened.initialize();assertEquals(3,reopened.status().generation)
            reopened.rollback();assertEquals(2,reopened.status().generation);assertEquals(3,reopened.status().previousGeneration)
            reopened.rollback();assertEquals(3,reopened.status().generation)
            reopened.reset();reopened.prune();assertEquals(2,reopened.status().generation)
            assertNull(reopened.status().previousGeneration);assertTrue(reopened.root.listFiles().orEmpty().none{it.isDirectory})
            assertEquals(before,journal.profileBackup(DEFAULT_PROFILE_ID))
        }finally{personal.close()}
    }
    @Test fun exactBuiltinPackageIsUnchangedButChangedSameGenerationAndDowngradeAreRejected()=runBlocking {
        val m=manager();m.initialize();val p=E09Fixture.seed(c)
        val unchanged=m.stage(E09Fixture.zip(c,p,c.assets.open("content/starter.json").use{it.readBytes()}).inputStream())
        assertTrue(unchanged.unchanged);m.activate(unchanged);assertNull(m.state.previous)
        reject{m.stage(E09Fixture.zip(c,p.copy(entries=p.entries.map{it.copy(descriptionCs=it.descriptionCs+" test")})).inputStream())}
        m.activate(m.stage(E09Fixture.zip(c).inputStream()))
        reject{m.stage(E09Fixture.zip(c,p).inputStream())};assertEquals(3,m.status().generation)
    }
    @Test fun damagedPayloadSignatureForeignPublisherMissingTileAndExtraPathsAreRejected()=runBlocking {
        val m=manager();m.initialize()
        val cases=listOf(
            E09Fixture.zip(c,archiveChange={it["catalog.json"]=byteArrayOf(1,2,3)}),
            E09Fixture.zip(c,archiveChange={it["manifest.sig"]=ByteArray(256)}),
            E09Fixture.zip(c,fileChange={it.remove("maps/1411/0/0/0.png")}),
            E09Fixture.zip(c,archiveChange={it["../escaped"]=byteArrayOf(1)}),
            E09Fixture.zip(c,fileChange={it["../signed-escape"]=byteArrayOf(1)}),
            E09Fixture.zip(c,archiveChange={it["unknown.json"]=byteArrayOf(1)})
        )
        cases.forEach{bytes->reject{m.stage(bytes.inputStream())};assertEquals(BUILTIN,m.state.active)}
        val other=java.security.KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
        reject{PackageManager(c,other.public.encoded,{Long.MAX_VALUE}).stage(E09Fixture.zip(c).inputStream())}
        assertFalse(File(c.filesDir,"escaped").exists())
    }
    @Test fun incompatibleSchemaAppVariantDependenciesAndCoordinateChangeNeverActivate()=runBlocking {
        val m=manager();m.initialize()
        val cases=listOf<(PackageManifest)->PackageManifest>(
            {it.copy(formatVersion=2)},{it.copy(contentSchema=2)},{it.copy(minimumAppCode=BuildConfig.VERSION_CODE+1)},
            {it.copy(gameVariant="classic")},{it.copy(dependencies=listOf("missing-pack"))},
            {it.copy(frames=it.frames.drop(1))},{it.copy(files=it.files.map{f->f.copy(size=f.size+1)})}
        )
        for(change in cases)reject{m.stage(E09Fixture.zip(c,manifestChange=change).inputStream())}
        val p=E09Fixture.seed(c).copy(generation=3);val map=p.maps.first()
        val changed=p.copy(maps=p.maps.map{if(it.key==map.key)it.copy(coordinateVersion="different") else it},locations=p.locations.map{if(it.mapKey==map.key)it.copy(coordinateVersion="different") else it})
        reject{m.stage(E09Fixture.zip(c,changed).inputStream())};assertEquals(2,m.status().generation)
    }
    @Test fun signedInvalidPngAndCatalogReferencesAreStillRejected()=runBlocking {
        val m=manager();m.initialize()
        reject{m.stage(E09Fixture.zip(c,fileChange={it["maps/1411/0/0/0.png"]=byteArrayOf(1,2,3)}).inputStream())}
        val p=E09Fixture.seed(c).copy(generation=3)
        reject{m.stage(E09Fixture.zip(c,p.copy(locations=p.locations.map{it.copy(entryKey="wow-forever:npc:99999999")})).inputStream())}
        assertEquals(BUILTIN,m.state.active)
    }
    @Test fun failedTransferAndCancellationCloseStreamsAndPreserveTheLastActiveGeneration()=runBlocking {
        val m=manager();m.initialize();m.activate(m.stage(E09Fixture.zip(c).inputStream()));val active=m.state
        var closed=false
        val broken=object:InputStream(){override fun read():Int=throw IOException("Interrupted network");override fun close(){closed=true}}
        reject{m.stage(broken)};assertTrue(closed);assertEquals(active,m.state)
        val input=object:ByteArrayInputStream(E09Fixture.zip(c,E09Fixture.seed(c).copy(generation=4))){override fun close(){closed=true;super.close()}}
        closed=false
        val worker=launch {val job=coroutineContext[Job]!!;m.stage(input){job.cancel()}}
        worker.join();assertTrue(worker.isCancelled);assertTrue(closed);assertEquals(active,m.state)
        assertTrue(m.root.listFiles().orEmpty().none{it.name.startsWith("incoming-") || it.name.startsWith("stage-")})
        val reopened=manager();reopened.initialize();assertEquals(3,reopened.status().generation)
    }
    @Test fun noSpaceBeforeTransferOrUnpackingLeavesThePreviousContentAndClosesInput()=runBlocking {
        var closed=false;val bytes=E09Fixture.zip(c)
        val input=object:ByteArrayInputStream(bytes){override fun close(){closed=true;super.close()}}
        val full=manager({0});full.initialize();reject{full.stage(input)};assertTrue(closed);assertEquals(BUILTIN,full.state.active)
        var calls=0;val shrinking=manager({if(++calls==1)Long.MAX_VALUE else 0});shrinking.initialize()
        reject{shrinking.stage(bytes.inputStream())};assertEquals(BUILTIN,shrinking.state.active)
        assertTrue(shrinking.root.listFiles().orEmpty().none{it.isDirectory})
    }
    @Test fun interruptionsBeforeDatabaseRenameAndPointerRetainTheOldGenerationAfterRestart()=runBlocking {
        for(point in listOf("database-ready","generation-ready","before-pointer")){
            clean();var crash:String?=null;val m=manager(fault={if(it==crash)throw IOException("Simulated interruption")});m.initialize()
            m.activate(m.stage(E09Fixture.zip(c).inputStream()));val old=m.state
            crash=point
            if(point=="before-pointer"){
                val next=m.stage(E09Fixture.zip(c,E09Fixture.seed(c).copy(generation=4)).inputStream());reject{m.activate(next)}
            }else reject{m.stage(E09Fixture.zip(c,E09Fixture.seed(c).copy(generation=4)).inputStream())}
            assertEquals(old,m.state)
            val reopened=manager();reopened.initialize();assertEquals(old,reopened.state)
            assertEquals(1,reopened.root.listFiles().orEmpty().count{it.isDirectory})
        }
    }
    @Test fun damagedActiveFilesOrDatabaseFallBackToThePreviousGenerationThenBuiltin()=runBlocking {
        val m=manager();m.initialize();m.activate(m.stage(E09Fixture.zip(c).inputStream()))
        m.activate(m.stage(E09Fixture.zip(c,E09Fixture.seed(c).copy(generation=4)).inputStream()))
        File(m.root,"${m.state.active}/maps/1411/0/0/0.png").writeBytes(byteArrayOf(0))
        val reopened=manager();reopened.initialize();assertEquals(3,reopened.status().generation);assertNotNull(reopened.recoveryMessage)
        reopened.databaseFile()!!.appendBytes(byteArrayOf(0))
        val fallback=manager();fallback.initialize();assertEquals(2,fallback.status().generation)
    }
    @Test fun pruningKeepsOnlyActiveAndPreviousAndRemovesAbandonedStaging()=runBlocking {
        val m=manager();m.initialize()
        for(g in 3..5){m.activate(m.stage(E09Fixture.zip(c,E09Fixture.seed(c).copy(generation=g)).inputStream()));m.prune()}
        assertEquals(5,m.status().generation);assertEquals(4,m.status().previousGeneration)
        assertEquals(2,m.root.listFiles().orEmpty().count{it.isDirectory})
        File(m.root,"stage-abandoned").mkdir();File(m.root,"incoming-incomplete.wfp").writeBytes(byteArrayOf(0))
        val reopened=manager();reopened.initialize();assertEquals(m.state,reopened.state)
        assertEquals(2,reopened.root.listFiles().orEmpty().count{it.isDirectory})
    }
    @Test fun downloadOfferChecksSizeAndHttpFailuresRedirectsAndTruncatedBodiesNeverSwitch()=runBlocking {
        val bytes=E09Fixture.zip(c);var code=200;var body=bytes;var reported=bytes.size.toLong();var last:FakeHttps?=null
        val m=PackageManager(c,E09Fixture.pair.public.encoded,{Long.MAX_VALUE},connectionFactory={uri->FakeHttps(uri,code,body,reported).also{last=it}})
        m.initialize();val offer=m.inspectDownload("https://updates.example.org/package.wfp",false);assertEquals(bytes.size.toLong(),offer.bytes)
        assertTrue(last!!.disconnected);assertEquals("HEAD",last!!.requestMethod);assertFalse(last!!.instanceFollowRedirects)
        code=302;reject{m.download(offer.url,false,offer.bytes)};assertTrue(last!!.disconnected)
        code=503;reject{m.download(offer.url,false,offer.bytes)}
        code=200;body=bytes.copyOf(bytes.size/2);reject{m.download(offer.url,false,offer.bytes)}
        body=bytes;reported=bytes.size+1L;reject{m.download(offer.url,false,offer.bytes)}
        assertEquals(BUILTIN,m.state.active)
        reported=bytes.size.toLong();val candidate=m.download(offer.url,false,offer.bytes);m.activate(candidate);assertEquals(3,m.status().generation)
    }
    @Test fun insecureUrlsAndDefaultWifiWithoutWifiDoNotOpenAConnection()=runBlocking {
        var opened=false
        val m=PackageManager(c,E09Fixture.pair.public.encoded,{Long.MAX_VALUE},connectionFactory={opened=true;error("Unexpected network")});m.initialize()
        for(url in listOf("http://example.org/a","https://user:pass@example.org/a","https://127.0.0.1/a","https://localhost/a","https://example.org:8443/a"))reject{m.inspectDownload(url,false)}
        reject{m.inspectDownload("https://updates.example.org/a",true)}
        assertFalse(opened);assertEquals(BUILTIN,m.state.active)
    }
}

private class FakeHttps(uri:URI,private val code:Int,private val body:ByteArray,private val size:Long):HttpsURLConnection(uri.toURL()) {
    var disconnected=false
    override fun getResponseCode()=code
    override fun getContentLengthLong()=size
    override fun getInputStream():InputStream=body.inputStream()
    override fun disconnect(){disconnected=true}
    override fun usingProxy()=false
    override fun connect(){}
    override fun getCipherSuite()="FAKE_TEST"
    override fun getLocalCertificates():Array<Certificate>?=null
    override fun getServerCertificates():Array<Certificate> = emptyArray()
}
