package kenny.fitbit.importer.steps

import kenny.fitbit.importer.Importer
import kenny.fitbit.steps.Steps

interface StepsImporter : Importer<Steps> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "steps-\\d{4}-\\d{2}-\\d{2}.json"
}
