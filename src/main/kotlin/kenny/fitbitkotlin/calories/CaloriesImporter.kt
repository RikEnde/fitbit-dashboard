package kenny.fitbitkotlin.calories

import kenny.fitbitkotlin.Importer

interface CaloriesImporter : Importer<Calories> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "calories-\\d{4}-\\d{2}-\\d{2}.json"
}
