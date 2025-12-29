package kenny.fitbit.distance

import kenny.fitbit.Importer

interface DistanceImporter : Importer<Distance> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "distance-\\d{4}-\\d{2}-\\d{2}.json"
}
