package cz.jenda.wowforever.data

import android.content.res.AssetManager
import cz.jenda.wowforever.model.*
import org.json.JSONObject

class AtlasRepository(private val assets: AssetManager) {
    fun load(): Atlas {
        val root = JSONObject(assets.open("atlas.json").bufferedReader().use { it.readText() })
        val maps = root.getJSONArray("maps").let { array -> (0 until array.length()).map {
            val m = array.getJSONObject(it)
            MapDescriptor(m.getInt("uiMapId"), m.getString("name"), m.getInt("width"),
                m.getInt("height"), m.getInt("levels"), m.getString("build"), m.getString("sourceUrl"))
        } }
        val points = root.getJSONArray("points").let { array -> (0 until array.length()).map {
            val p = array.getJSONObject(it)
            MapPoint(p.getString("id"), p.getString("name"),
                MapPosition(p.getInt("uiMapId"), p.getDouble("xPercent"), p.getDouble("yPercent")),
                p.getString("sourceUrl"))
        } }
        return Atlas(maps, points)
    }
}
