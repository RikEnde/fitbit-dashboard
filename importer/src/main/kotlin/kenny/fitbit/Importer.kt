package kenny.fitbit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import kenny.fitbit.profile.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

/**
 * Base interface for file importers.
 * No dependencies on JSON/Jackson or JPA - those are in format-specific subclasses.
 */
interface Importer<T> {
    var dataDir: String

    var userDir: String?

    var profile: Profile?

    var maxDate: LocalDate?

    var onProgress: (String) -> Unit

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
            file.isFile && !java.nio.file.Files.isSymbolicLink(file.toPath()) && pattern.matches(file.name)
        } ?: emptyArray()

        onProgress("Found ${files.size} files matching pattern ${filePattern()} in directory ${dir.absolutePath}")
        files.forEach { onProgress("  - ${it.name}") }

        return files.toList().sorted()
    }
}

/**
 * Abstract base class for file importers that persist to JPA.
 * Handles concurrent file processing, batched persistence, max date tracking, and file renaming.
 */
abstract class FileImporter<T>(
    protected val repository: JpaRepository<T, *>,
    protected val entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : Importer<T> {

    protected val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    override var dataDir: String = "../data"
    override var userDir: String? = null
    override var profile: Profile? = null
    private val _maxDate = AtomicReference<LocalDate?>(null)
    override var maxDate: LocalDate?
        get() = _maxDate.get()
        set(value) { _maxDate.set(value) }
    override var onProgress: (String) -> Unit = ::println

    open val batchSize: Int = 5000
    open val maxConcurrentFiles: Int = 10

    /**
     * Hook called before importing files.
     * Override to perform setup like loading existing IDs for duplicate detection.
     */
    open fun beforeImport() {}

    /**
     * Process a single file. Called concurrently from import().
     */
    protected abstract suspend fun importFile(index: Int, size: Int, file: File)

    protected open fun saveBatch(entities: List<T>) {
        transactionTemplate.execute {
            repository.saveAll(entities)
            entityManager.flush()
            entityManager.clear()
        }
    }

    protected fun updateMaxDate(entity: T) {
        val date = entityDate(entity) ?: return
        while (true) {
            val current = _maxDate.get()
            if (current != null && date <= current) break
            if (_maxDate.compareAndSet(current, date)) break
        }
    }

    override fun import(): Int {
        maxDate = null
        beforeImport()

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
                    } catch (e: Exception) {
                        onProgress("Failed to import ${file.name}, file will not be renamed: ${e.message}")
                        log.error("Failed to import ${file.name}", e)
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.joinAll()
        }

        onProgress("Completed processing ${files.size} files")
        return size
    }
}

/**
 * Abstract base class for JSON file importers.
 */
abstract class JsonImporter<T>(
    repository: JpaRepository<T, *>,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : FileImporter<T>(repository, entityManager, transactionManager) {

    private val objectMapper = ObjectMapper()

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

    override suspend fun importFile(index: Int, size: Int, file: File) {
        onProgress("Processing file ${index + 1} of $size (%.1f%%) - ${file.name}".format(100.0 * index / size))

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

            onProgress("Imported $count records from ${file.name}")
        }
    }
}

/**
 * Abstract base class for CSV file importers.
 */
abstract class CsvImporter<T>(
    repository: JpaRepository<T, *>,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : FileImporter<T>(repository, entityManager, transactionManager) {

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

    override suspend fun importFile(index: Int, size: Int, file: File) {
        onProgress("Processing file ${index + 1} of $size (%.1f%%) - ${file.name}".format(100.0 * index / size))

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

        onProgress("Imported $lineCount records from ${file.name}")
    }
}
