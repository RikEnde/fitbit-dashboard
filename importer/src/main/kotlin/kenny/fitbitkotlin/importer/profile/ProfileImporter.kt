package kenny.fitbitkotlin.importer.profile

import kenny.fitbitkotlin.importer.Importer
import kenny.fitbitkotlin.profile.Profile

interface AccountImporter : Importer<Profile> {
    override fun directory(): String = "Personal & Account"
    override fun filePattern(): String = "Profile.csv"
    fun avatarpath(): String = "Personal & Account/Media/Avatar Photo.jpg"
}
