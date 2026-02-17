package kenny.fitbit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import kenny.fitbit.profile.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Base interface for file importers.
 * No dependencies on JSON/Jackson or JPA - those are in format-specific subclasses.
 */
interface Importer<T> {
    var dataDir: String

    var userDir: String?

    var profile: Profile?

    var maxDate: LocalDate?

    /**
     * Subdirectory within dataDir where files are located.
     */
    fun directory(): String

    /**
     * Regex pattern for matching files to import.
     */
    fun filePattern(): String

    /**
     * Extract the date from an entity for tracking the most recent data date.
     */
    fun entityDate(entity: T): LocalDate? = null

    /**
     * Main entry point - import all matching files.
     * Returns the number of files processed.
     */
    fun import(): Int

    /**
     * Find all files matching the pattern in the directory.
     */
    fun files(): List<File> {
        val baseDir = if (userDir != null) File(dataDir, userDir) else File(dataDir)
        val dir = File(baseDir, directory())
        val pattern = filePattern().toRegex()

        val files = dir.listFiles { file ->
            file.isFile && pattern.matches(file.name)
        } ?: emptyArray()

        println("Found ${files.size} files matching pattern ${filePattern()} in directory ${dir.absolutePath}")
        files.forEach { println("  - ${it.name}") }

        return files.toList().sorted()
    }
}

/**
 * Abstract base class for JSON file importers.
 * Handles concurrent file processing, JSON parsing, and batched persistence.
 */
abstract class JsonImporter<T>(
    protected val repository: JpaRepository<T, *>,
    protected val entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : Importer<T> {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override var dataDir: String = "../data"
    override var userDir: String? = null
    override var profile: Profile? = null
    override var maxDate: LocalDate? = null

    /**
     * Number of entities to batch before persisting.
     */
    open val batchSize: Int = 5000

    /**
     * Maximum number of files to process concurrently.
     */
    open val maxConcurrentFiles: Int = 10

    /**
     * Parse a JSON item into an entity.
     * Return null to skip the item.
     */
    abstract fun parseToEntity(jsonItem: JsonNode): T?

    /**
     * Date/time formatter for parsing timestamps in JSON.
     * Override if your data uses a different format.
     */
    open fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    /**
     * Hook called before importing files.
     * Override to perform setup like loading existing IDs for duplicate detection.
     */
    protected open fun beforeImport() {}

    protected open fun saveBatch(entities: List<T>) {
        transactionTemplate.execute {
            repository.saveAll(entities)
            entityManager.flush()
            entityManager.clear()
        }
    }

    private fun updateMaxDate(entity: T) {
        val date = entityDate(entity) ?: return
        val current = maxDate
        if (current == null || date > current) {
            maxDate = date
        }
    }

    override fun import(): Int {
        maxDate = null
        beforeImport()

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

    protected open suspend fun importFile(
        index: Int,
        size: Int,
        file: File,
        objectMapper: ObjectMapper
    ) {
        println("Processing file ${index + 1} of $size (%.1f%%)".format(100.0 * index / size))
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
                        updateMaxDate(entity)
                        count++

                        if (batch.size >= batchSize) {
                            saveBatch(batch.toList())
                            batch.clear()
                        }
                    }
                }

                if (batch.isNotEmpty()) {
                    saveBatch(batch.toList())
                }

                println("Imported $count records from ${file.name}")
            }
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
        }
    }
}

/**
 * Abstract base class for CSV file importers.
 * Handles concurrent file processing, CSV parsing, and batched persistence.
 */
abstract class CsvImporter<T>(
    protected val repository: JpaRepository<T, *>,
    protected val entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : Importer<T> {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override var dataDir: String = "../data"
    override var userDir: String? = null
    override var profile: Profile? = null
    override var maxDate: LocalDate? = null

    /**
     * Number of entities to batch before persisting.
     */
    open val batchSize: Int = 5000

    /**
     * Maximum number of files to process concurrently.
     */
    open val maxConcurrentFiles: Int = 10

    /**
     * Whether the CSV has a header row to skip.
     */
    open val hasHeader: Boolean = true

    /**
     * CSV delimiter character.
     */
    open val delimiter: String = ","

    /**
     * Parse a CSV row into an entity.
     * @param values List of column values
     * @param headers The header names (empty list if no header)
     * @return Entity or null to skip the row
     */
    abstract fun parseRow(values: List<String>, headers: List<String>): T?

    /**
     * Date/time formatter for parsing timestamps.
     * Override for format-specific patterns.
     */
    open fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    protected open fun saveBatch(entities: List<T>) {
        transactionTemplate.execute {
            repository.saveAll(entities)
            entityManager.flush()
            entityManager.clear()
        }
    }

    private fun updateMaxDate(entity: T) {
        val date = entityDate(entity) ?: return
        val current = maxDate
        if (current == null || date > current) {
            maxDate = date
        }
    }

    override fun import(): Int {
        maxDate = null
        val files = files()
        val size = files.size
        val semaphore = Semaphore(maxConcurrentFiles)

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        importFile(index, size, file)
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

    protected open suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<T>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                val headers = if (hasHeader) {
                    reader.readLine()?.split(delimiter) ?: emptyList()
                } else {
                    emptyList()
                }

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val values = line!!.split(delimiter)
                    val entity = parseRow(values, headers)
                    if (entity != null) {
                        batch.add(entity)
                        updateMaxDate(entity)
                        lineCount++

                        if (batch.size >= batchSize) {
                            saveBatch(batch.toList())
                            batch.clear()
                        }
                    }
                }
            }

            if (batch.isNotEmpty()) {
                saveBatch(batch.toList())
            }

            println("Imported $lineCount records from ${file.name}")
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
