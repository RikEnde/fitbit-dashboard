package kenny.fitbit.importer.calories

import kenny.fitbit.calories.Calories
import kenny.fitbit.importer.Importer

/**
 * Interface for Calories JSON importer.
 * Implementations should extend JsonImporter<Calories> and implement this interface.
 */
interface CaloriesImporter : Importer<Calories> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "calories-\\d{4}-\\d{2}-\\d{2}.json"
}
