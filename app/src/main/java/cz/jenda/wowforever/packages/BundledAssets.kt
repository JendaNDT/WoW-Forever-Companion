package cz.jenda.wowforever.packages

import android.content.Context
import android.content.res.AssetManager
import cz.jenda.wowforever.content.ContentJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Enumeration

@Serializable private data class BundleIndex(val parts:List<String>,val bytes:Long,val sha256:String)
/** Opens a stored part at a time; the signed manifest verifies the complete archive. */
internal fun sequenceParts(parts:List<String>,opener:(String)->InputStream):InputStream {
    var index=0
    return SequenceInputStream(object:Enumeration<InputStream> {
        override fun hasMoreElements()=index<parts.size
        override fun nextElement()=opener(parts[index++])
    })
}
internal fun openBundledAsset(context:Context,name:String):InputStream {
    fun stored(path:String):InputStream {
        val descriptor=try { context.assets.openFd(path) }
            catch(_:IOException) { return context.assets.open(path,AssetManager.ACCESS_STREAMING) }
        return try { descriptor.createInputStream() } catch(t:Throwable) { descriptor.close();throw t }
    }
    val indexInput=try { context.assets.open("$name.index.json") } catch(_:IOException) { return stored(name) }
    val raw=indexInput.use { it.readBytes().also { bytes->require(bytes.size<=16384) } }
    val index=ContentJson.decodeFromString<BundleIndex>(raw.toString(Charsets.UTF_8))
    require(index.parts.size in 1..128 && index.parts.distinct().size==index.parts.size && index.bytes>0)
    val basename=name.substringAfterLast('/')
    require(index.parts.all { it.matches(Regex(Regex.escape(basename)+"\\.part[0-9]{3}\\.wfpart")) })
    val folder=name.substringBeforeLast('/',"")
    return sequenceParts(index.parts.map { if(folder.isEmpty())it else "$folder/$it" },::stored)
}
