package cz.jenda.wowforever

import android.content.Context
import cz.jenda.wowforever.content.*
import cz.jenda.wowforever.packages.*
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun e10Package():ContentPackage=decodePackage(E10DataTest::class.java.getResourceAsStream("/e10-v4.json")!!.use{it.readBytes()})
fun e10ManifestBytes()=File("../content/e10-v4.manifest.json").readBytes()
fun e10Signature()=File("../content/e10-v4.manifest.sig").readBytes()
fun distributionTile(context:Context,path:String):ByteArray=if(path.startsWith("maps/1413/"))File("../content/e10-maps/$path").readBytes() else context.assets.open(path).use{it.readBytes()}

/** Real publisher-signed distribution assembled using only public source material; no private key in tests. */
fun e10Distribution(context:Context,legacy3:Boolean=false,change:(MutableMap<String,ByteArray>)->Unit={}):ByteArray {
    val resource=E10DataTest::class.java
    val manifestBytes=if(legacy3)resource.getResourceAsStream("/legacy-e09-gen3.manifest.json")!!.use{it.readBytes()} else e10ManifestBytes()
    val signature=if(legacy3)resource.getResourceAsStream("/legacy-e09-gen3.manifest.sig")!!.use{it.readBytes()} else e10Signature()
    val catalog=resource.getResourceAsStream(if(legacy3)"/legacy-e09-gen3.json" else "/e10-v4.json")!!.use{it.readBytes()}
    val manifest=ContentJson.decodeFromString<PackageManifest>(manifestBytes.decodeToString())
    val files=linkedMapOf("manifest.json" to manifestBytes,"manifest.sig" to signature)
    manifest.files.forEach { file ->files[file.path]=if(file.path=="catalog.json")catalog else distributionTile(context,file.path)}
    change(files)
    val out=ByteArrayOutputStream()
    ZipOutputStream(out).use{zip->files.forEach{(path,bytes)->zip.putNextEntry(ZipEntry(path));zip.write(bytes);zip.closeEntry()}}
    return out.toByteArray()
}
