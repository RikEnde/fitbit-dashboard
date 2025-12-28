package kenny.fitbitkotlin.importer.distance

import kenny.fitbitkotlin.distance.Distance
import kenny.fitbitkotlin.importer.Importer

interface DistanceImporter : Importer<Distance> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "distance-\\d{4}-\\d{2}-\\d{2}.json"
}
