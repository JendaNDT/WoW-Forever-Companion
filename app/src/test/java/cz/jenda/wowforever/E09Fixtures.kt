package cz.jenda.wowforever

import android.content.Context
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.packages.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Ephemeral keys and synthetic catalog revisions exist in tests only. */
object E09Fixture {
    val pair by lazy {KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()}
    fun seed(c:Context)=c.assets.open("content/starter.json").use{decodePackage(it.readBytes())}
    fun zip(c:Context,p:ContentPackage=seed(c).copy(generation=3),
        raw:ByteArray=ContentJson.encodeToString(p).toByteArray(),
        fileChange:(MutableMap<String,ByteArray>)->Unit={},
        manifestChange:(PackageManifest)->PackageManifest={it},
        archiveChange:(MutableMap<String,ByteArray>)->Unit={}):ByteArray {
        val files=linkedMapOf("catalog.json" to raw)
        val baseline=ContentJson.decodeFromString<List<PackFile>>(c.assets.open("content/builtin-files.json").use{it.readBytes().decodeToString()})
        baseline.filter{it.path!="catalog.json"}.forEach{f->files[f.path]=c.assets.open(f.path).use{it.readBytes()}}
        fileChange(files)
        val manifest=manifestChange(PackageManifest(1,p.packageId,p.generation,p.gameVariant,1,9,p.checkedOn,emptyList(),
            p.maps.map{PackFrame(it.key,it.floorId,it.coordinateVersion,it.build)},files.map{(name,data)->PackFile(name,data.size.toLong(),sha256(data))}))
        val encoded=ContentJson.encodeToString(manifest).toByteArray()
        val signature=Signature.getInstance("SHA256withRSA").apply{initSign(pair.private);update(encoded)}.sign()
        files["manifest.json"]=encoded;files["manifest.sig"]=signature;archiveChange(files)
        val out=ByteArrayOutputStream()
        ZipOutputStream(out).use{z->files.forEach{(name,data)->z.putNextEntry(ZipEntry(name));z.write(data);z.closeEntry()}}
        return out.toByteArray()
    }
}
