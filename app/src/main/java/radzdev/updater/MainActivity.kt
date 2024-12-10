package radzdev.updater

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.radzdev.radzupdater.updater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updater(this,
            "https://raw.githubusercontent.com/Radzdevteam/test/refs/heads/main/updatertest")
            .checkForUpdates()
    }
}
