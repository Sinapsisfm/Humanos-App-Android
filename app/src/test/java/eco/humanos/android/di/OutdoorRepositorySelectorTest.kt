package eco.humanos.android.di

import eco.humanos.android.core.outdoor.repository.InMemoryOutdoorRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

/** Composición pura de selección de repositorio (JVM, sin Hilt/Room/Android). */
class OutdoorRepositorySelectorTest {

    @Test
    fun `room off devuelve in-memory y NO construye room`() {
        var roomBuilt = false
        val inMem = InMemoryOutdoorRepository()
        val result = selectOutdoorRepository(
            roomEnabled = false,
            room = { roomBuilt = true; InMemoryOutdoorRepository() },
            inMemory = { inMem },
        )
        assertSame(inMem, result)
        assertFalse("Room no debe construirse cuando el flag está OFF", roomBuilt)
    }

    @Test
    fun `room on devuelve room y NO construye in-memory`() {
        var inMemBuilt = false
        val roomRepo = InMemoryOutdoorRepository() // sentinela (stand-in del repo Room)
        val result = selectOutdoorRepository(
            roomEnabled = true,
            room = { roomRepo },
            inMemory = { inMemBuilt = true; InMemoryOutdoorRepository() },
        )
        assertSame(roomRepo, result)
        assertFalse("In-memory no debe construirse cuando Room está ON", inMemBuilt)
    }
}
