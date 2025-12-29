package kenny.fitbit.importer.profile

import kenny.fitbit.importer.Importer
import kenny.fitbit.profile.Profile

interface AccountImporter : Importer<Profile> {
    override fun directory(): String = "Personal & Account"
    override fun filePattern(): String = "Profile.csv"
    fun avatarpath(): String = "Personal & Account/Media/Avatar Photo.jpg"
}
