package com.therapytrack.android.offline

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileNotFoundException

/**
 * Queue bytes at rest under a hardware-backed key. A queued check-in or
 * journal entry is health data sitting on a phone; the file itself is
 * unreadable to anything that gets hold of it without the keystore.
 */
class EncryptedQueueStorage(private val context: Context, name: String) : QueueStorage {
    private val file = File(context.filesDir, name)
    private val masterKey by lazy { MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build() }

    private fun encrypted(target: File) = EncryptedFile.Builder(
        context, target, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()

    override fun read(): ByteArray? {
        if (!file.exists()) return null
        if (!file.canRead()) throw FileNotFoundException("${file.name} is not readable")
        return encrypted(file).openFileInput().use { it.readBytes() }
    }

    override fun write(bytes: ByteArray) {
        // EncryptedFile refuses to overwrite, so write beside and rename over.
        // The scratch copy keeps the *same file name* in a scratch directory:
        // EncryptedFile binds the ciphertext to the file's name (it is the
        // associated data), so a "queue.json.tmp" renamed to "queue.json" would
        // be unreadable on the next launch — which is exactly what happened
        // the first time this ran on an emulator.
        val scratchDir = File(context.filesDir, "queue-scratch").apply { mkdirs() }
        val scratch = File(scratchDir, file.name)
        if (scratch.exists()) scratch.delete()
        encrypted(scratch).openFileOutput().use { it.write(bytes) }
        if (!scratch.renameTo(file)) { scratch.delete(); throw java.io.IOException("could not replace ${file.name}") }
    }

    override fun quarantine(suffix: String) {
        val dest = File(file.parentFile, "${file.name}.unreadable-$suffix")
        if (!file.renameTo(dest)) throw java.io.IOException("could not move ${file.name} aside")
    }
}
