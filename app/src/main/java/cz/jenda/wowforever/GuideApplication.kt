package cz.jenda.wowforever

import android.app.Application
import cz.jenda.wowforever.data.GuideStore

const val BUNDLED_CONTENT_ASSET="content/bundled-data-8.wfp"

open class GuideApplication : Application() {
    open val store by lazy { GuideStore(this,bundledAsset=BUNDLED_CONTENT_ASSET) }
}
