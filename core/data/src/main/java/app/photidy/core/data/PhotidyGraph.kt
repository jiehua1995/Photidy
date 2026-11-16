package app.photidy.core.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Minimal manual dependency graph. A DI framework would add build complexity without
 * value at this stage; feature ViewModels receive their dependencies from here.
 */
object PhotidyGraph {

    @Volatile
    private var initialized = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appContext: Context
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var photoRepository: PhotoRepository
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            settingsRepository = SettingsRepository(appContext)
            photoRepository = PhotoRepository(
                store = LibraryStore(appContext),
                mediaLibrary = MediaLibrary(appContext),
                mediaEditor = MediaEditor(appContext),
                scope = scope,
            )
            initialized = true
        }
    }
}
