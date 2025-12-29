package kenny.fitbit.profile

import jakarta.persistence.*

@Entity
@Table(
    name = "profile"
)
data class Profile(
    @Id
    val id: String,

    @Column(nullable = false)
    val fullName: String,

    @Column(nullable = false)
    val firstName: String,

    @Column(nullable = false)
    val lastName: String,

    @Column(nullable = false)
    val displayNameSetting: String,

    @Column(nullable = false)
    val displayName: String,

    @Column(nullable = true)
    val username: String?,

    @Column(nullable = false)
    val emailAddress: String,

    @Column(nullable = false)
    val dateOfBirth: String,

    @Column(nullable = false)
    val child: Boolean,

    @Column(nullable = false)
    val country: String,

    @Column(nullable = true)
    val state: String?,

    @Column(nullable = true)
    val city: String?,

    @Column(nullable = false)
    val timezone: String,

    @Column(nullable = false)
    val locale: String,

    @Column(nullable = false)
    val memberSince: String,

    @Column(nullable = true)
    val aboutMe: String?,

    @Column(nullable = false)
    val startOfWeek: String,

    @Column(nullable = false)
    val sleepTracking: String,

    @Column(nullable = false)
    val timeDisplayFormat: String,

    @Column(nullable = false)
    val gender: String,

    @Column(nullable = false)
    val height: Double,

    @Column(nullable = false)
    val weight: Double,

    @Column(nullable = false)
    val strideLengthWalking: Double,

    @Column(nullable = false)
    val strideLengthRunning: Double,

    @Column(nullable = false)
    val weightUnit: String,

    @Column(nullable = false)
    val distanceUnit: String,

    @Column(nullable = false)
    val heightUnit: String,

    @Column(nullable = false)
    val waterUnit: String,

    @Column(nullable = false)
    val glucoseUnit: String,

    @Column(nullable = false)
    val swimUnit: String,

    @Lob
    @Column(nullable = true)
    val avatar: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Profile

        if (id != other.id) return false
        if (fullName != other.fullName) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (displayNameSetting != other.displayNameSetting) return false
        if (displayName != other.displayName) return false
        if (username != other.username) return false
        if (emailAddress != other.emailAddress) return false
        if (dateOfBirth != other.dateOfBirth) return false
        if (child != other.child) return false
        if (country != other.country) return false
        if (state != other.state) return false
        if (city != other.city) return false
        if (timezone != other.timezone) return false
        if (locale != other.locale) return false
        if (memberSince != other.memberSince) return false
        if (aboutMe != other.aboutMe) return false
        if (startOfWeek != other.startOfWeek) return false
        if (sleepTracking != other.sleepTracking) return false
        if (timeDisplayFormat != other.timeDisplayFormat) return false
        if (gender != other.gender) return false
        if (height != other.height) return false
        if (weight != other.weight) return false
        if (strideLengthWalking != other.strideLengthWalking) return false
        if (strideLengthRunning != other.strideLengthRunning) return false
        if (weightUnit != other.weightUnit) return false
        if (distanceUnit != other.distanceUnit) return false
        if (heightUnit != other.heightUnit) return false
        if (waterUnit != other.waterUnit) return false
        if (glucoseUnit != other.glucoseUnit) return false
        if (swimUnit != other.swimUnit) return false
        if (avatar != null) {
            if (other.avatar == null) return false
            if (!avatar.contentEquals(other.avatar)) return false
        } else if (other.avatar != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + firstName.hashCode()
        result = 31 * result + lastName.hashCode()
        result = 31 * result + displayNameSetting.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + emailAddress.hashCode()
        result = 31 * result + dateOfBirth.hashCode()
        result = 31 * result + child.hashCode()
        result = 31 * result + country.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        result = 31 * result + (city?.hashCode() ?: 0)
        result = 31 * result + timezone.hashCode()
        result = 31 * result + locale.hashCode()
        result = 31 * result + memberSince.hashCode()
        result = 31 * result + (aboutMe?.hashCode() ?: 0)
        result = 31 * result + startOfWeek.hashCode()
        result = 31 * result + sleepTracking.hashCode()
        result = 31 * result + timeDisplayFormat.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + strideLengthWalking.hashCode()
        result = 31 * result + strideLengthRunning.hashCode()
        result = 31 * result + weightUnit.hashCode()
        result = 31 * result + distanceUnit.hashCode()
        result = 31 * result + heightUnit.hashCode()
        result = 31 * result + waterUnit.hashCode()
        result = 31 * result + glucoseUnit.hashCode()
        result = 31 * result + swimUnit.hashCode()
        result = 31 * result + (avatar?.contentHashCode() ?: 0)
        return result
    }
}
