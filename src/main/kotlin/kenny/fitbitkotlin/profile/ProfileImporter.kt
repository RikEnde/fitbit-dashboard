package kenny.fitbitkotlin.profile

import kenny.fitbitkotlin.Importer

interface AccountImporter : Importer<Profile> {
    override fun directory(): String = "Personal & Account"
    override fun filePattern(): String = "Profile.csv"
    fun avatarpath(): String = "Personal & Account/Media/Avatar Photo.jpg"
}
