package com.therapytrack.android.offline

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileNotFoundException

/**
 * Work that belongs to one account.
 *
 * A queue is one file per device, and a device can be signed into more than
 * one account over its life. Left unscoped, a journal entry written offline as
 * one person would be sent under whoever signed in next, into their record.
 */
interface OwnedWork {
    val ownerUserId: Int
}

/** The items belonging to [userId]. Nobody signed in means nothing is anyone's. */
fun <T : OwnedWork> List<T>.ownedBy(userId: Int?): List<T> =
    if (userId == null) emptyList() else filter { it.ownerUserId == userId }

/** How a queue's items are turned into bytes and back. Injected so tests can run on plain files. */
interface QueueCodec<Item> {
    fun encode(items: List<Item>): ByteArray
    fun decode(bytes: ByteArray): List<Item>
}

/** Where the queue's bytes live. The real one is an EncryptedFile; tests use plain files. */
interface QueueStorage {
    /** Returns null when nothing has ever been written. Throws when present but unreadable. */
    fun read(): ByteArray?
    fun write(bytes: ByteArray)
    /** Move the current file aside under [suffix], keeping its bytes. */
    fun quarantine(suffix: String)
}

/** Plain-file storage: the shape every test drives, and the fallback if encryption is unavailable. */
class FileQueueStorage(val file: File) : QueueStorage {
    override fun read(): ByteArray? {
        if (!file.exists()) return null
        if (!file.canRead()) throw FileNotFoundException("${file.name} is not readable")
        return file.readBytes()
    }

    override fun write(bytes: ByteArray) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) { tmp.delete(); throw java.io.IOException("could not replace ${file.name}") }
    }

    override fun quarantine(suffix: String) {
        val dest = File(file.parentFile, "${file.name}.unreadable-$suffix")
        if (!file.renameTo(dest)) throw java.io.IOException("could not move ${file.name} aside")
    }
}

/**
 * Disk backing for the outboxes that hold clinical work until it reaches the
 * server. The one distinction it exists to make: **an empty queue and an
 * unreadable queue are not the same thing.** A read that fails for any reason
 * other than "nothing was ever written" is reported as [Load.Unreadable], and
 * the caller's contract is to leave the file alone until it can read it.
 */
class DurableQueueStore<Item : Any>(
    val storage: QueueStorage,
    private val codec: QueueCodec<Item>,
    private val id: (Item) -> Any,
    private val log: (String) -> Unit = {}
) {
    sealed class Load<out Item> {
        data class Items<Item>(val items: List<Item>) : Load<Item>()
        data class Unreadable(val cause: Throwable) : Load<Nothing>()
    }

    fun load(): Load<Item> {
        val bytes = try {
            storage.read() ?: return Load.Items(emptyList())   // never queued
        } catch (e: Exception) {
            return Load.Unreadable(e)                          // locked, or real I/O trouble
        }
        return try {
            Load.Items(codec.decode(bytes))
        } catch (decodeError: Exception) {
            // Keep the bytes: a queue that will not decode is usually a shape
            // change shipped in an update — exactly when it most likely holds work.
            try {
                storage.quarantine(System.currentTimeMillis().toString())
            } catch (moveError: Exception) {
                // Still at its path, so "empty" would let the next write replace it.
                log("queue did not decode and could not be moved aside; leaving untouched: $moveError")
                return Load.Unreadable(moveError)
            }
            log("queue did not decode; kept aside: $decodeError")
            Load.Items(emptyList())
        }
    }

    fun write(items: List<Item>) = storage.write(codec.encode(items))

    /** Both sides kept, an item present in both appearing once (memory wins). */
    fun merge(stored: List<Item>, memory: List<Item>): List<Item> {
        val held = memory.map(id).toSet()
        return stored.filter { id(it) !in held } + memory
    }
}

/**
 * The in-memory half of a durable queue and the rules about when it may be
 * written back. `loaded` is set only after a read that actually succeeded.
 */
class DurableQueue<Item : Any>(
    private val store: DurableQueueStore<Item>,
    private val id: (Item) -> Any,
    private val log: (String) -> Unit = {}
) {
    private val lock = Mutex()
    private var items: MutableList<Item> = mutableListOf()
    private var loaded = false

    suspend fun all(): List<Item> = lock.withLock { loadIfNeeded(); items.toList() }
    suspend fun count(): Int = all().size

    /** Add an item and write it down. Returns whether the write reached disk. */
    suspend fun append(item: Item): Boolean = lock.withLock {
        loadIfNeeded()
        items.add(item)
        persist()
    }

    suspend fun remove(itemId: Any) = lock.withLock {
        loadIfNeeded()
        items.removeAll { id(it) == itemId }
        persist()
    }

    suspend fun update(itemId: Any, transform: (Item) -> Item) = lock.withLock {
        loadIfNeeded()
        val index = items.indexOfFirst { id(it) == itemId }
        if (index >= 0) { items[index] = transform(items[index]); persist() }
    }

    /** False means the store exists and is currently unreadable; nothing is written over it. */
    suspend fun isReadable(): Boolean = lock.withLock { loadIfNeeded(); loaded }

    private fun loadIfNeeded() {
        if (loaded) return
        when (val load = store.load()) {
            is DurableQueueStore.Load.Items -> {
                val heldInMemory = items.toList()
                items = store.merge(load.items, heldInMemory).toMutableList()
                loaded = true
                // Items accepted during the unreadable window have never been on disk.
                if (heldInMemory.isNotEmpty()) persist()
            }
            is DurableQueueStore.Load.Unreadable -> log("queue unreadable, not overwriting: ${load.cause}")
        }
    }

    private fun persist(): Boolean {
        if (!loaded) return false
        return try { store.write(items); true } catch (e: Exception) { log("could not persist queue: $e"); false }
    }
}
