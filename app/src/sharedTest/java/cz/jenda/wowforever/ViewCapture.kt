package cz.jenda.wowforever

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/** Draw Android views to a real raster Canvas; works in host tests without PixelCopy. Main thread. */
fun captureView(view: View): Bitmap {
    require(view.width > 0 && view.height > 0)
    return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
        view.draw(Canvas(it))
    }
}
