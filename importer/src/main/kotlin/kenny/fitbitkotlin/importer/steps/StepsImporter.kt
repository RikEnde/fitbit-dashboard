package kenny.fitbitkotlin.importer.steps

import kenny.fitbitkotlin.importer.Importer
import kenny.fitbitkotlin.steps.Steps

interface StepsImporter : Importer<Steps> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "steps-\\d{4}-\\d{2}-\\d{2}.json"
}
