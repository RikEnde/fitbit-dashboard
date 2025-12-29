package kenny.fitbit.profile

import kenny.fitbit.Importer

interface AccountImporter : Importer<Profile> {
    override fun directory(): String = "Personal & Account"
    override fun filePattern(): String = "Profile.csv"
    fun avatarpath(): String = "Personal & Account/Media/Avatar Photo.jpg"
}
