package com.therapytrack.android

import com.therapytrack.android.offline.DurableQueue
import com.therapytrack.android.offline.DurableQueueStore
import com.therapytrack.android.offline.FileQueueStorage
import com.therapytrack.android.offline.JsonQueueCodec
import com.therapytrack.android.offline.OwnedWork
import com.therapytrack.android.offline.ownedBy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * The property under test is narrow and absolute: work already queued is
 * never destroyed by work queued later — not by a locked device, not by a
 * file that will not decode, not by a disk that will not take a write.
 */
class DurableQueueTest {
    @Serializable
    data class Note(val id: String = UUID.randomUUID().toString(), val text: String, val attempts: Int = 0)

    private val root = File(System.getProperty("java.io.tmpdir"), "dq-${UUID.randomUUID()}").apply { mkdirs() }
    private val codec = JsonQueueCodec(Note.serializer())

    private fun store(file: File) = DurableQueueStore(FileQueueStorage(file), codec, { it.id })
    private fun queue(file: File) = DurableQueue(store(file), { it.id })
    private fun fresh(name: String = UUID.randomUUID().toString()) = File(root, "$name.json")
    private fun onDisk(file: File) = Json.decodeFromString(ListSerializer(Note.serializer()), file.readText())

    @Test fun `an appended item survives a restart`() = runBlocking {
        val file = fresh()
        val note = Note(text = "first")
        queue(file).append(note)
        assertEquals(listOf(note), queue(file).all())
    }

    @Test fun `absence and unreadability are different answers`() {
        assertTrue(store(fresh()).load() is DurableQueueStore.Load.Items)
        val file = fresh().apply { writeText("[]"); setReadable(false) }
        try { assertTrue(store(file).load() is DurableQueueStore.Load.Unreadable) } finally { file.setReadable(true) }
    }

    @Test fun `a later write must not erase a queue that could not be read`() = runBlocking {
        val file = fresh()
        val queued = listOf(Note(text = "a"), Note(text = "b"), Note(text = "c"))
        file.writeText(Json.encodeToString(ListSerializer(Note.serializer()), queued))

        file.setReadable(false)                                   // the device is locked
        val q = queue(file)
        assertFalse(q.isReadable())
        assertEquals(0, q.count())
        val today = Note(text = "written while locked")
        q.append(today)
        file.setReadable(true)
        assertEquals("nothing was written over the queue it could not read", 3, onDisk(file).size)

        assertTrue(q.isReadable())                                // unlocked: both sets survive, once each
        val all = q.all()
        assertTrue(queued.all { n -> all.any { it.id == n.id } })
        assertTrue(all.any { it.id == today.id })
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals("the merge reached disk", 4, onDisk(file).size)
    }

    @Test fun `a queue that will not decode is kept aside, not dropped`() = runBlocking {
        val file = fresh("corrupt").apply { writeText("""[{"id":1,"text":null}]""") }
        assertEquals(0, queue(file).count())
        assertEquals(1, root.list()!!.count { it.startsWith("corrupt.json.unreadable-") })
        assertFalse(file.exists())
    }

    @Test fun `an undecodable file that cannot be moved aside is never overwritten`() = runBlocking {
        val dir = File(root, "stuck").apply { mkdirs() }
        val file = File(dir, "q.json")
        val original = """[{"id":1,"text":null}]"""
        file.writeText(original)
        dir.setWritable(false)                                    // no rename, no create
        try {
            val q = queue(file)
            assertFalse(q.isReadable())
            assertFalse("the new item is reported as not stored", q.append(Note(text = "new")))
        } finally { dir.setWritable(true) }
        assertEquals(original, file.readText())
    }

    @Test fun `merging keeps both sides exactly once`() {
        val shared = Note(text = "in both")
        val merged = store(fresh()).merge(stored = listOf(Note(text = "stored"), shared), memory = listOf(shared, Note(text = "memory")))
        assertEquals(3, merged.size)
        assertEquals(3, merged.map { it.id }.toSet().size)
    }

    @Test fun `ordinary operations`() = runBlocking {
        val file = fresh()
        val q = queue(file)
        val one = Note(text = "one"); val two = Note(text = "two")
        q.append(one); q.append(two)
        q.update(one.id) { it.copy(attempts = it.attempts + 1) }
        assertEquals(1, q.all().first { it.id == one.id }.attempts)
        assertEquals(0, q.all().first { it.id == two.id }.attempts)
        q.remove(one.id)
        assertEquals(1, queue(file).count())
        q.remove(two.id)
        assertEquals(0, queue(file).count())
        assertTrue("draining leaves it empty, not absent", file.exists())
    }

    @Test fun `append says whether it reached disk`() = runBlocking {
        val dir = File(root, "readonly").apply { mkdirs(); setWritable(false) }
        try {
            val q = queue(File(dir, "q.json"))
            assertFalse(q.append(Note(text = "into the void")))
            assertEquals("still held in memory for a live send", 1, q.count())
        } finally { dir.setWritable(true) }
        assertTrue(queue(fresh()).append(Note(text = "fine")))
    }

    @Test fun `work is scoped to the account that wrote it`() {
        data class Owned(override val ownerUserId: Int, val text: String) : OwnedWork
        val items = listOf(Owned(1, "Alice, offline"), Owned(2, "Bob"), Owned(1, "Alice again"))
        assertEquals(listOf("Bob"), items.ownedBy(2).map { it.text })
        assertEquals(2, items.ownedBy(1).size)
        assertTrue("nobody signed in means nothing is anyone's", items.ownedBy(null).isEmpty())
    }
}
