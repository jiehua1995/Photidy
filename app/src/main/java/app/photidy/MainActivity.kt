package app.photidy

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.model.LibraryMode

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotidyApp()
        }
    }

    override fun onResume() {
        super.onResume()
        // Photos can be added or removed outside the app; re-read MediaStore on return.
        val repository = PhotidyGraph.photoRepository
        if (repository.libraryMode.value == LibraryMode.DEVICE) {
            repository.refreshDeviceLibrary()
        }
    }
}
