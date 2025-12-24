package kenny.fitbitkotlin.distance

import kenny.fitbitkotlin.Importer

interface DistanceImporter : Importer<Distance> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "distance-\\d{4}-\\d{2}-\\d{2}.json"
}
