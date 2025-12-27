package kenny.fitbitkotlin.importer

object BatchConstants {
    const val DEFAULT_BATCH_SIZE = 5000
    const val SMALL_BATCH_SIZE = 1000    // For cascade entities
    const val LARGE_BATCH_SIZE = 10000   // For CSV
    const val MAX_CONCURRENT_FILES = 10
}
