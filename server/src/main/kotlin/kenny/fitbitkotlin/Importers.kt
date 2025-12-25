package kenny.fitbitkotlin

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import jakarta.persistence.EntityManager
import java.io.File
import java.time.format.DateTimeFormatter

interface Importer<T> {
    val dataDir: String
        get() = "data"

    val batchSize: Int
        get() = BatchConstants.DEFAULT_BATCH_SIZE

    val maxConcurrentFiles: Int
        get() = BatchConstants.MAX_CONCURRENT_FILES

    fun directory(): String
    fun filePattern(): String
    fun parseAndSaveEntity(jsonItem: com.fasterxml.jackson.databind.JsonNode)

    fun parseToEntity(jsonItem: com.fasterxml.jackson.databind.JsonNode): T? {
        parseAndSaveEntity(jsonItem)
        return null
    }

    fun getDateTimeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    fun import(): Int {
        val files = files()
        val objectMapper = ObjectMapper()
        val size = files.size
        val semaphore = Semaphore(maxConcurrentFiles)

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        importFile(index, size, file, objectMapper)
                        file.renameTo(File(file.absolutePath + ".imported"))
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    suspend fun importFile(
        index: Int,
        size: Int,
        file: File,
        objectMapper: ObjectMapper
    ) {
        println("Processing file ${index + 1} of $size (%.4f".format(100.0 * index / size))
        println("Parsing $file")
        try {
            val jsonNode = objectMapper.readTree(file)
            if (jsonNode.isArray) {
                for (item in jsonNode) {
                    parseAndSaveEntity(item)
                }
            }
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
        }
    }

    suspend fun importFileWithBatching(
        index: Int,
        size: Int,
        file: File,
        objectMapper: ObjectMapper,
        entityManager: EntityManager,
        saveAllBatch: (List<T>) -> Unit
    ) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val jsonNode = objectMapper.readTree(file)
            if (jsonNode.isArray) {
                val batch = mutableListOf<T>()
                var count = 0

                for (item in jsonNode) {
                    val entity = parseToEntity(item)
                    if (entity != null) {
                        batch.add(entity)
                        count++

                        if (batch.size >= batchSize) {
                            saveAllBatch(batch)
                            entityManager.flush()
                            entityManager.clear()
                            batch.clear()
                        }
                    }
                }

                if (batch.isNotEmpty()) {
                    saveAllBatch(batch)
                    entityManager.flush()
                    entityManager.clear()
                }

                println("Imported $count records from ${file.name}")
            }
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
        }
    }

    fun files(): List<File> {
        // List corresponding files that match the pattern
        val dir = File(dataDir, directory())
        val pattern = filePattern().toRegex()

        val files = dir.listFiles { file ->
            file.isFile && pattern.matches(file.name)
        } ?: emptyArray()

        println("Found ${files.size} files matching pattern ${filePattern()} in directory ${dir.absolutePath}")
        files.forEach { println("  - ${it.name}") }

        return files.toList().sorted()
    }
}