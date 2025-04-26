package kenny.fitbitkotlin.steps

import kenny.fitbitkotlin.Importer

interface StepsImporter : Importer<Steps> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "steps-\\d{4}-\\d{2}-\\d{2}.json"
}
