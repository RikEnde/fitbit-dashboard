package kenny.fitbit.importer.distance

import kenny.fitbit.distance.Distance
import kenny.fitbit.importer.Importer

interface DistanceImporter : Importer<Distance> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "distance-\\d{4}-\\d{2}-\\d{2}.json"
}
