package kenny.fitbitkotlin.importer.calories

import kenny.fitbitkotlin.importer.Importer
import kenny.fitbitkotlin.calories.Calories

interface CaloriesImporter : Importer<Calories> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "calories-\\d{4}-\\d{2}-\\d{2}.json"
}
