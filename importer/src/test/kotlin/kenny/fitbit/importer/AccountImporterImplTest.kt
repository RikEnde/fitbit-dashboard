package kenny.fitbit.importer

import kenny.fitbit.importer.profile.AccountImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class AccountImporterImplTest {

    @Autowired
    private lateinit var accountImporter: AccountImporterImpl

    @Test
    fun `file pattern excludes imported files`() {
        val pattern = accountImporter.filePattern().toRegex()
        assertTrue(pattern.matches("Profile.csv"))
        assertFalse(pattern.matches("Profile.csv.imported"))
        assertFalse(pattern.matches("OtherFile.csv"))
    }

    @Test
    fun `CSV file parses correctly`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val csvFile = File(testDataDir, "Personal & Account/Profile.csv")
        val lines = csvFile.readLines()

        val header = lines[0].split(",")
        val data = lines[1].split(",")
        val rowData = header.zip(data).toMap()

        assertEquals("TEST123", rowData["id"])
        assertEquals("Test User", rowData["full_name"])
        assertEquals("test@example.com", rowData["email_address"])
        assertEquals("175.0", rowData["height"])
        assertEquals("70.0", rowData["weight"])
    }
}
