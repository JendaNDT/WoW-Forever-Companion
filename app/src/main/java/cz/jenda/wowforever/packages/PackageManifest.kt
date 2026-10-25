package cz.jenda.wowforever.packages

import cz.jenda.wowforever.content.*
import kotlinx.serialization.Serializable
import java.io.File
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate

const val MAX_ARCHIVE_BYTES = 128L * 1024 * 1024
const val MAX_EXPANDED_BYTES = 256L * 1024 * 1024
const val MAX_MANIFEST_BYTES = 512 * 1024
const val BUILTIN = "builtin"

@Serializable data class PackFile(val path:String,val size:Long,val sha256:String)
@Serializable data class PackFrame(val mapKey:String,val floorId:Int,val coordinateVersion:String,val build:String)
@Serializable data class PackageManifest(val formatVersion:Int,val packageId:String,val generation:Int,
    val gameVariant:String,val contentSchema:Int,val minimumAppCode:Int,val checkedOn:String,
    val dependencies:List<String>,val frames:List<PackFrame>,val files:List<PackFile>)
@Serializable data class ActivePackages(val active:String=BUILTIN,val previous:String?=null)
data class PackageCandidate(val id:String,val manifest:PackageManifest,val catalog:ContentPackage,val bytes:Long,val unchanged:Boolean=false)
data class PackageStatus(val generation:Int,val downloaded:Boolean,val previousGeneration:Int?,val downloadBytes:Long,
    val maps:List<Pair<String,Long>>,val recoveryMessage:String?=null)
data class DownloadOffer(val url:String,val bytes:Long)

fun sha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
fun fileHash(file:File):String {
    val digest=MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input -> val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;digest.update(b,0,n)} }
    return digest.digest().joinToString(""){"%02x".format(it)}
}

object ManifestValidator {
    fun decode(bytes:ByteArray,signature:ByteArray,publicKey:ByteArray,appCode:Int):PackageManifest {
        require(bytes.size in 1..MAX_MANIFEST_BYTES && signature.size in 256..1024){"Neplatný popis nebo podpis balíčku."}
        val verifier=Signature.getInstance("SHA256withRSA")
        verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKey)))
        verifier.update(bytes)
        require(verifier.verify(signature)){"Podpis balíčku neodpovídá vydavateli průvodce."}
        val m=ContentJson.decodeFromString<PackageManifest>(bytes.toString(Charsets.UTF_8))
        require(m.formatVersion==1 && m.contentSchema==1 && m.minimumAppCode in 9..appCode){"Balíček potřebuje jinou verzi aplikace."}
        require(m.packageId==CONTENT_PACKAGE_ID && m.gameVariant=="wow-forever" && m.generation>0){"Balíček patří jiné hře nebo oblasti."}
        LocalDate.parse(m.checkedOn)
        require(m.dependencies.isEmpty()){ "Balíček vyžaduje nepodporovanou závislost." }
        require(m.files.size in 2..8192 && m.files.map{it.path}.distinct().size==m.files.size){"Neplatný seznam souborů."}
        require(m.files.all { f ->
            (f.path=="catalog.json" || f.path.matches(Regex("maps/[1-9][0-9]{0,8}/(?:floor_[0-9]{1,3}/)?[0-7]/[0-9]{1,3}/[0-9]{1,3}\\.png"))) &&
                f.size in 1..(if(f.path=="catalog.json")MAX_PACKAGE_BYTES.toLong() else 4L*1024*1024) && f.sha256.matches(Regex("[0-9a-f]{64}"))
        }){"Neplatná cesta, velikost nebo součet souboru."}
        require(m.files.sumOf{it.size}<=MAX_EXPANDED_BYTES && m.files.count{it.path=="catalog.json"}==1){"Příliš velký nebo neúplný balíček."}
        require(m.frames.isNotEmpty() && m.frames.map{"${it.mapKey}:${it.floorId}"}.distinct().size==m.frames.size){"Neplatné mapové rámce."}
        return m
    }
    fun catalog(m:PackageManifest,p:ContentPackage) {
        ContentValidator.validate(p)
        val entries=p.entries.associateBy{it.key}
        val minimumCode=if(p.generation>=7 || p.entries.any { it.referencePosition || it.kind==EntryKind.OBJECT } || p.maps.any { it.uiMapId !in setOf(947,1414,1411,1454,1413) })15
            else if(p.locations.any{entries[it.entryKey]?.verification==Verification.CLASSIC})14
            else if(p.maps.any{it.uiMapId==1413 || it.scopeCs!=null})10 else 9
        require(m.minimumAppCode >= minimumCode){"Balíček uvádí nepravdivou kompatibilitu aplikace."}
        require(m.generation==p.generation && m.packageId==p.packageId && m.gameVariant==p.gameVariant && m.contentSchema==p.schemaVersion && m.checkedOn==p.checkedOn){"Popis balíčku neodpovídá katalogu."}
        require(m.frames.toSet()==p.maps.map{PackFrame(it.key,it.floorId,it.coordinateVersion,it.build)}.toSet()){"Neshodné mapy nebo souřadnicové verze."}
        val expected=mutableSetOf("catalog.json")
        p.maps.forEach { map ->
            val root=if(map.floorId==0)"maps/${map.uiMapId}" else "maps/${map.uiMapId}/floor_${map.floorId}"
            for(z in 0 until map.levels){
                val divisor=1 shl(map.levels-1-z)
                val columns=(map.width+256*divisor-1)/(256*divisor);val rows=(map.height+256*divisor-1)/(256*divisor)
                for(row in 0 until rows)for(col in 0 until columns)expected.add("$root/$z/$row/$col.png")
            }
        }
        require(expected==m.files.map{it.path}.toSet()){"Chybí dlaždice nebo balíček obsahuje cizí soubory."}
    }
}
