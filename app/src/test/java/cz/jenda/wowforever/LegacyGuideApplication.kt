package cz.jenda.wowforever

import cz.jenda.wowforever.data.GuideStore

/** Preserve the pre-E13 startup state for historical upgrade/rollback regression scenarios. */
class LegacyGuideApplication:GuideApplication() {
    override val store by lazy { GuideStore(this) }
}
