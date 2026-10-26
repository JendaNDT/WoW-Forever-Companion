package cz.jenda.wowforever

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabletLayoutDeviceTest:TabletLayoutChecks() {
    override fun shot(name:String) {
        // Capture the hardware-rendered window; View.draw(Canvas) cannot draw GPU map tiles.
        val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Device screenshot unavailable" }
        outputDirectory().resolve("$name.png").apply {
            parentFile!!.mkdirs()
            outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        }
        bitmap.recycle()
    }
}
