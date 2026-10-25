package cz.jenda.wowforever

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import cz.jenda.wowforever.ui.GuideScreen
import cz.jenda.wowforever.ui.GuideTheme

class MainActivity : ComponentActivity() {
    private val model: GuideViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ReportDrawnWhen { model.ready }; GuideTheme { GuideScreen(model) } }
    }
    override fun onStop() { model.checkpoint(); super.onStop() }
    override fun onTrimMemory(level:Int) {
        super.onTrimMemory(level)
        if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)model.trimMapMemory()
    }
}
