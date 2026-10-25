package cz.jenda.wowforever.packages

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AtomicFile
import androidx.room.Room
import cz.jenda.wowforever.BuildConfig
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.data.CatalogInstaller
import cz.jenda.wowforever.data.db.ContentDatabase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import java.io.*
import java.net.URI
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection

/** Whole compatible catalog/map generations, immutable once committed. Personal data is never opened here. */
class PackageManager(private val context:Context,
    private val publicKey:ByteArray=context.assets.open("content/publisher.der").use{it.readBytes()},
    private val freeBytes:()->Long={context.filesDir.usableSpace},
    private val fault:(String)->Unit={},
    private val connectionFactory:(URI)->HttpsURLConnection={it.toURL().openConnection() as HttpsURLConnection},
    private val bundledAsset:String?=null,
    private val bundledOpener:(String)->InputStream={openBundledAsset(context,it)}) {
    val root=File(context.filesDir,"content-generations").apply{mkdirs()}
    private val pointer=AtomicFile(File(root,"active.json"))
    private val bundledReceipt=AtomicFile(File(root,"bundled-receipt"))
    private val mutex=Mutex()
    var state=ActivePackages();private set
    var recoveryMessage:String?=null;private set
    private val seed by lazy{context.assets.open("content/starter.json").use{decodePackage(it.readBytes())}}
    private val bundled by lazy {
        bundledAsset?.let { asset ->
            var manifest:ByteArray?=null;var signature:ByteArray?=null
            ZipInputStream(bundledOpener(asset).buffered()).use { zip ->
                // The bundled archive is produced by build_package with these two records first.
                repeat(2) {
                    val entry=requireNotNull(zip.nextEntry){"Chybí popis přibaleného obsahu."}
                    when(entry.name) {
                        "manifest.json"->manifest=zip.readBytesLimited(MAX_MANIFEST_BYTES)
                        "manifest.sig"->signature=zip.readBytesLimited(1024)
                        else->error("Neplatný přibalený obsah.")
                    }
                    zip.closeEntry()
                }
            }
            val bytes=requireNotNull(manifest)
            val m=ManifestValidator.decode(bytes,requireNotNull(signature),publicKey,BuildConfig.VERSION_CODE)
            Pair(m,"g${m.generation}_${sha256(bytes)}")
        }
    }
    private fun directory(id:String):File {
        require(id.matches(Regex("g[1-9][0-9]*_[a-f0-9]{64}"))){"Neplatná generace."}
        return File(root,id)
    }
    private fun room(file:File)=Room.databaseBuilder(context,ContentDatabase::class.java,file.absolutePath).build()
    private fun readManifest(dir:File)=ManifestValidator.decode(File(dir,"manifest.json").readBytes(),File(dir,"manifest.sig").readBytes(),publicKey,BuildConfig.VERSION_CODE)
    private fun space(required:Long){require(freeBytes()>=required+20L*1024*1024){"Není dost volného místa. Dosavadní obsah zůstal zachovaný."}}
    private fun writeState(value:ActivePackages) {
        val out=pointer.startWrite()
        try {out.write(ContentJson.encodeToString(value).toByteArray());pointer.finishWrite(out)}
        catch(t:Throwable){pointer.failWrite(out);throw t}
        state=value
    }
    suspend fun initialize()=mutex.withLock {
        val stored=try {pointer.openRead().use{ContentJson.decodeFromString<ActivePackages>(it.readBytes().toString(Charsets.UTF_8))}}
        catch(_:FileNotFoundException){ActivePackages()}
        catch(_:Exception){recoveryMessage="Záznam aktualizace byl poškozený. Používám přibalený obsah.";ActivePackages()}
        fun valid(id:String):Boolean= id==BUILTIN || runCatching{verifyDirectory(directory(id),true);true}.getOrDefault(false)
        state=when {
            valid(stored.active)->stored.copy(previous=stored.previous?.takeIf{valid(it)})
            stored.previous!=null && valid(stored.previous)->{recoveryMessage="Poslední balíček byl neúplný. Obnovil jsem předchozí funkční obsah.";ActivePackages(stored.previous)}
            else->{recoveryMessage="Stažený obsah byl neúplný. Používám přibalené mapy a katalog.";ActivePackages()}
        }
        if(state!=stored || recoveryMessage!=null)writeState(state)
        cleanup()
        if(bundledAsset!=null) {
            try { applyBundledLocked(recoveryMessage!=null) }
            catch(cancel:CancellationException){throw cancel}
            catch(_:Exception){recoveryMessage="Přibalené rozšíření se nepodařilo připravit. Dosavadní obsah a postup zůstaly zachované; zkus aplikaci znovu otevřít nebo obnovit přibalený obsah v Balíčky a mapy."}
        }
    }
    private fun acknowledgeBundle(id:String) {
        val out=bundledReceipt.startWrite()
        try{out.write(id.toByteArray());bundledReceipt.finishWrite(out)}
        catch(t:Throwable){bundledReceipt.failWrite(out);throw t}
    }
    private suspend fun stageBundledLocked(allowOlder:Boolean=false):PackageCandidate {
        val archive=File(root,"incoming-${UUID.randomUUID()}.wfp")
        try {
            bundledOpener(requireNotNull(bundledAsset)).use{copyLimited(it,archive,MAX_ARCHIVE_BYTES){}}
            return stageArchive(archive,allowOlder)
        }finally{archive.delete()}
    }
    // Do not retain the full candidate catalog while checking/activating the installed directory.
    private suspend fun prepareBundledId(allowOlder:Boolean=false)=stageBundledLocked(allowOlder).id
    private suspend fun applyBundledLocked(recovering:Boolean=false) {
        val (manifest,id)=requireNotNull(bundled)
        val acknowledged=try{bundledReceipt.openRead().use{it.readBytesLimited(160).decodeToString()}==id}catch(_:Exception){false}
        if(acknowledged && !recovering)return
        // An imported identical or newer generation stays selected, including future signed updates.
        if(activeGeneration()<manifest.generation)activateGenerationLocked(prepareBundledId())
        // A rollback is a user choice. Remember this offer so another launch does not undo that choice.
        // If this auxiliary write fails after activation, retry it on the next launch without reverting data.
        try{acknowledgeBundle(id)}catch(_:IOException){}
    }
    private fun cleanup(keep:String?=null) {
        val retained=setOfNotNull(state.active,state.previous,keep)
        root.listFiles().orEmpty().filter{it.isDirectory && (it.name.startsWith("g") || it.name.startsWith("stage-")) && it.name !in retained}.forEach{it.deleteRecursively()}
        root.listFiles().orEmpty().filter{it.isFile && it.name.startsWith("incoming-")}.forEach{it.delete()}
    }
    fun databaseFile():File?=state.active.takeUnless{it==BUILTIN}?.let{File(directory(it),"catalog.db")}
    fun tileOpener():(String)->InputStream {
        val dir=state.active.takeUnless{it==BUILTIN}?.let(::directory)
        return {path-> if(dir==null)context.assets.open(path).buffered() else File(dir,path).inputStream().buffered()}
    }
    fun activeCatalog():ContentPackage=if(state.active==BUILTIN)seed else File(directory(state.active),"catalog.json").inputStream().use { decodePackage(it) }
    private fun activeGeneration()=if(state.active==BUILTIN)seed.generation else readManifest(directory(state.active)).generation
    private fun activeMaps():List<MapRecord> {
        if(state.active==BUILTIN)return seed.maps
        val db=android.database.sqlite.SQLiteDatabase.openDatabase(File(directory(state.active),"catalog.db").absolutePath,null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
        return try {
            db.rawQuery("SELECT recordJson FROM maps ORDER BY uiMapId",null).use { cursor ->
                buildList { while(cursor.moveToNext())add(ContentJson.decodeFromString<MapRecord>(cursor.getString(0))) }
            }
        } finally { db.close() }
    }
    fun status():PackageStatus {
        val m=if(state.active==BUILTIN)null else readManifest(directory(state.active))
        val mapSizes=activeMaps().map{map->
            val prefix="maps/${map.uiMapId}/"
            map.name to (m?.files?.filter{it.path.startsWith(prefix)}?.sumOf{it.size} ?: builtinFiles().filter{it.path.startsWith(prefix)}.sumOf{it.size})
        }
        val previous=state.previous?.let{if(it==BUILTIN)seed.generation else readManifest(directory(it)).generation}
        return PackageStatus(m?.generation ?: seed.generation,state.active!=BUILTIN && state.active!=runCatching{bundled?.second}.getOrNull(),previous,m?.files?.sumOf{it.size} ?: 0,mapSizes,recoveryMessage)
    }
    private val seedFiles by lazy {
        val manifest=ContentJson.decodeFromString<List<PackFile>>(context.assets.open("content/builtin-files.json").use{it.readBytes().toString(Charsets.UTF_8)})
        manifest
    }
    private fun builtinFiles()=seedFiles
    private fun compatibleMaps(p:ContentPackage) {
        val known=ContentJson.decodeFromString<List<MapRecord>>(context.assets.open("content/supported-maps.json").use{it.readBytes().toString(Charsets.UTF_8)}).associateBy{it.key}
        require(p.maps.all { m -> known[m.key]?.let { old ->
            m.uiMapId==old.uiMapId && m.floorId==old.floorId && m.width==old.width && m.height==old.height && m.levels==old.levels &&
                m.coordinateVersion==old.coordinateVersion && m.transform==old.transform
        }==true }){"Jiná souřadnicová soustava nebo nepodporované mapy vyžadují novější verzi aplikace. Poznámky nebyly přesunuté."}
    }
    private suspend fun copyLimited(input:InputStream,target:File,limit:Long,progress:(Long)->Unit) {
        FileOutputStream(target).use{out->
            val buffer=ByteArray(65536);var total=0L
            while(true){currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break;total+=n
                require(total<=limit){"Balíček překračuje povolenou velikost."};out.write(buffer,0,n);progress(total)}
            out.fd.sync()
        }
    }
    suspend fun stage(input:InputStream,progress:(Long)->Unit={}):PackageCandidate=input.use{mutex.withLock {
        space(2L*1024*1024)
        val archive=File(root,"incoming-${UUID.randomUUID()}.wfp")
        try {copyLimited(input,archive,MAX_ARCHIVE_BYTES,progress);stageArchive(archive)}finally{archive.delete()}
    }}
    private fun connection(url:String,wifiOnly:Boolean):HttpsURLConnection {
        // Network supplies bytes only. A pinned publisher signature is mandatory before any activation.
        val uri=URI(url.trim())
        require(uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null && uri.fragment==null && (uri.port==-1 || uri.port==443)) {"Použij úplný HTTPS odkaz na podepsaný balíček."}
        require(uri.host!="localhost" && !uri.host.contains(':') && !uri.host.matches(Regex("[0-9.]+"))){"Použij veřejný název HTTPS serveru."}
        if(wifiOnly){
            val connectivity=context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            require(connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)==true){"Připoj Wi-Fi, nebo výslovně povol mobilní data."}
        }
        return connectionFactory(uri).apply {
            instanceFollowRedirects=false;connectTimeout=10_000;readTimeout=10_000
            setRequestProperty("Accept","application/zip, application/octet-stream")
        }
    }
    suspend fun inspectDownload(url:String,wifiOnly:Boolean):DownloadOffer=mutex.withLock {
        val c=connection(url,wifiOnly)
        try {
            c.requestMethod="HEAD";currentCoroutineContext().ensureActive()
            require(c.responseCode==HttpURLConnection.HTTP_OK && c.contentLengthLong in 1..MAX_ARCHIVE_BYTES){"Server neposkytl velikost balíčku. Vyber stažený soubor ručně."}
            space(c.contentLengthLong)
            DownloadOffer(url.trim(),c.contentLengthLong)
        }finally{c.disconnect()}
    }
    suspend fun download(url:String,wifiOnly:Boolean=true,expectedSize:Long?=null,progress:(Long)->Unit={}):PackageCandidate=mutex.withLock {
        space(expectedSize ?: MAX_ARCHIVE_BYTES)
        val archive=File(root,"incoming-${UUID.randomUUID()}.wfp")
        var connection:HttpsURLConnection?=null
        try {
            connection=connection(url,wifiOnly)
            currentCoroutineContext().ensureActive()
            require(connection.responseCode==HttpURLConnection.HTTP_OK){"Server balíček neposkytl. Použij přímý odkaz bez přesměrování."}
            require(connection.contentLengthLong<=MAX_ARCHIVE_BYTES){"Balíček je příliš velký."}
            require(expectedSize==null || connection.contentLengthLong==expectedSize){"Soubor na serveru se změnil. Znovu zjisti jeho velikost."}
            connection.inputStream.use{copyLimited(it,archive,MAX_ARCHIVE_BYTES,progress)}
            require(connection.contentLengthLong<0 || archive.length()==connection.contentLengthLong){"Stahování nebylo úplné."}
            stageArchive(archive)
        }finally{connection?.disconnect();archive.delete()}
    }
    private suspend fun stageArchive(archive:File,allowOlder:Boolean=false):PackageCandidate {
        val stage=File(root,"stage-${UUID.randomUUID()}").apply{check(mkdir())}
        try {
            lateinit var m:PackageManifest;lateinit var manifestBytes:ByteArray
            ZipFile(archive).use{zip->
                val entries=mutableListOf<java.util.zip.ZipEntry>();val enumeration=zip.entries()
                while(enumeration.hasMoreElements()){require(entries.size<8194){"Archiv obsahuje příliš mnoho souborů."};entries.add(enumeration.nextElement())}
                require(entries.size in 4..8194 && entries.map{it.name}.distinct().size==entries.size && entries.none{it.isDirectory}){"Neplatná struktura archivu."}
                fun small(name:String,max:Int):ByteArray {
                    val entry=requireNotNull(zip.getEntry(name)){"Chybí popis nebo podpis."}
                    require(entry.size in 1..max.toLong()){"Příliš velký popis balíčku."}
                    return zip.getInputStream(entry).use{it.readBytesLimited(max)}
                }
                manifestBytes=small("manifest.json",MAX_MANIFEST_BYTES)
                val signature=small("manifest.sig",1024)
                m=ManifestValidator.decode(manifestBytes,signature,publicKey,BuildConfig.VERSION_CODE)
                require(entries.map{it.name}.toSet()==m.files.map{it.path}.toSet()+setOf("manifest.json","manifest.sig")){"Archiv obsahuje neočekávané soubory."}
                space(m.files.sumOf{it.size}+4L*MAX_PACKAGE_BYTES)
                File(stage,"manifest.json").writeBytes(manifestBytes);File(stage,"manifest.sig").writeBytes(signature)
                for(f in m.files){
                    currentCoroutineContext().ensureActive()
                    val e=zip.getEntry(f.path);require(e.size==f.size){"Neshodná velikost souboru."}
                    val target=File(stage,f.path)
                    require(target.canonicalPath.startsWith(stage.canonicalPath+File.separator)){"Neplatná cesta v balíčku."}
                    target.parentFile!!.mkdirs()
                    zip.getInputStream(e).use{copyLimited(it,target,f.size){}}
                    require(target.length()==f.size && fileHash(target)==f.sha256){"Nesouhlasí kontrolní součet balíčku."}
                }
            }
            val catalog=File(stage,"catalog.json").inputStream().use { decodePackage(it) };ManifestValidator.catalog(m,catalog);compatibleMaps(catalog)
            checkTiles(stage,m)
            val current=activeGeneration()
            require(allowOlder || m.generation>=current){"Starší generaci použij přes tlačítko Návrat."}
            if(m.generation==current){
                val activeFiles=if(state.active==BUILTIN)builtinFiles() else readManifest(directory(state.active)).files
                require(m.files.toSet()==activeFiles.toSet()){"Stejná generace má jiný obsah."}
                return PackageCandidate(state.active,m,catalog,archive.length(),true)
            }
            val id="g${m.generation}_${sha256(manifestBytes)}"
            state.previous?.takeUnless{it==BUILTIN}?.let { previous ->
                val old=readManifest(directory(previous))
                require(old.generation!=m.generation || previous==id){"Dříve uložená generace má jiný obsah."}
            }
            val db=room(File(stage,"catalog.db"))
            try{CatalogInstaller(db).installDecoded(catalog,m.files.single { it.path=="catalog.json" }.sha256)
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use{it.moveToFirst()}
            }finally{db.close()}
            File(stage,"catalog.db.sha256").writeText(fileHash(File(stage,"catalog.db")))
            fault("database-ready")
            val target=directory(id)
            if(target.exists())verifyDirectory(target,true) else {
                Files.move(stage.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE)
            }
            fault("generation-ready")
            return PackageCandidate(id,m,catalog,archive.length())
        }finally{stage.deleteRecursively()}
    }
    private fun checkTiles(dir:File,m:PackageManifest) {
        m.files.filter{it.path.endsWith(".png")}.forEach { f ->
            val file=File(dir,f.path);val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeFile(file.path,bounds)
            require(bounds.outWidth==256 && bounds.outHeight==256 && bounds.outMimeType=="image/png"){"Neplatná mapová dlaždice."}
            val bitmap=requireNotNull(BitmapFactory.decodeFile(file.path)){"Poškozená mapa."}
            try{require(bitmap.width==256 && bitmap.height==256)}finally{bitmap.recycle()}
        }
    }
    private fun verifyDirectory(dir:File,database:Boolean):PackageManifest {
        val m=readManifest(dir)
        require(dir.name=="g${m.generation}_${sha256(File(dir,"manifest.json").readBytes())}")
        m.files.forEach{f->val file=File(dir,f.path);require(file.isFile && file.length()==f.size && fileHash(file)==f.sha256){"Neúplná generace obsahu."}}
        val catalog=File(dir,"catalog.json").inputStream().use { decodePackage(it) };ManifestValidator.catalog(m,catalog);compatibleMaps(catalog)
        if(database){
            require(File(dir,"catalog.db.sha256").readText()==fileHash(File(dir,"catalog.db"))){"Poškozená databáze balíčku."}
            val db=android.database.sqlite.SQLiteDatabase.openDatabase(File(dir,"catalog.db").absolutePath,null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
            try{
                db.rawQuery("PRAGMA quick_check",null).use{require(it.moveToFirst() && it.getString(0)=="ok")}
                db.rawQuery("SELECT generation,sha256 FROM package_metadata WHERE slot=1",null).use{require(it.moveToFirst() && it.getInt(0)==m.generation && it.getString(1)==m.files.single{f->f.path=="catalog.json"}.sha256)}
            }finally{db.close()}
        }
        return m
    }
    private suspend fun activateLocked(candidate:PackageCandidate) {
        if(candidate.unchanged)return
        activateGenerationLocked(candidate.id)
    }
    private suspend fun activateGenerationLocked(id:String) {
        currentCoroutineContext().ensureActive();val manifest=verifyDirectory(directory(id),true);space(65536)
        require(manifest.generation>activeGeneration()) {"Balíček už není novější než aktivní obsah."}
        fault("before-pointer")
        // Once this synchronous atomic write starts, cancellation cannot expose a half-generation.
        writeState(ActivePackages(id,state.active));recoveryMessage=null
    }
    suspend fun activate(candidate:PackageCandidate)=mutex.withLock {activateLocked(candidate)}
    suspend fun rollback()=mutex.withLock {
        val previous=requireNotNull(state.previous){"Předchozí generace není uložená."}
        if(previous!=BUILTIN)verifyDirectory(directory(previous),true)
        fault("before-pointer");writeState(ActivePackages(previous,state.active));recoveryMessage=null
    }
    suspend fun reset()=mutex.withLock {
        val bundle=bundled
        if(bundle==null) {fault("before-pointer");writeState(ActivePackages())}
        else {
            // A verified retained generation already is the exact bundled content.
            // Reusing it also avoids copying the 121 MB archive again after a rollback.
            val id=if(directory(bundle.second).isDirectory)bundle.second else prepareBundledId(allowOlder=true)
            verifyDirectory(directory(id),true)
            fault("before-pointer");writeState(ActivePackages(id))
            try{acknowledgeBundle(bundle.second)}catch(_:IOException){}
        }
        recoveryMessage=null
    }
    suspend fun restoreSelection(original:ActivePackages)=mutex.withLock {
        if(original.active!=BUILTIN)verifyDirectory(directory(original.active),true)
        writeState(original)
    }
    suspend fun discard()=mutex.withLock {cleanup()}
    suspend fun prune()=mutex.withLock {fault("before-prune");cleanup()}
}

private fun InputStream.readBytesLimited(max:Int):ByteArray {
    val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true){val n=read(buffer);if(n<0)break;require(out.size()+n<=max);out.write(buffer,0,n)}
    return out.toByteArray()
}
