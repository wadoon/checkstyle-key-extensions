import com.puppycrawl.tools.checkstyle.*
import org.junit.jupiter.api.Test
import org.key_project.checkstyle.Helper
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlin.test.assertEquals

/**
 *
 * @author Alexander Weigl
 * @version 1 (7/21/25)
 */
class Test {
    @Test
    fun test() {
        System.setProperty("home.dir", Paths.get(".").absolute().toString())

        val root = Paths.get("share")

        root.resolve("Test.java").writeText(
            """
            public class Test {
                // A tooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo long line, that should be forbidden, but previously exists.
                // This line is also so long......................................................., but fresh. Hence, it appears in git diff. 
            }
        """.trimIndent()
        )

        val files = root.walk().map { it.toFile() }.toList()

        val sos = ByteArrayOutputStream()
        val listener = DefaultLogger(sos, AbstractAutomaticBean.OutputStreamOptions.NONE)

        val config = Paths.get("src/test/resources/config.xml").toAbsolutePath()
        //val inputSource = InputSource(config.bufferedReader())
        val configuration = ConfigurationLoader.loadConfiguration(
            config.toString(),
            PropertiesExpander(System.getProperties())
        )

        val checker = Checker()
        checker.setModuleClassLoader(Checker::class.java.classLoader)
        checker.configure(configuration)
        checker.addListener(listener)

        val errors = checker.process(files)
        println("""Found $errors check style errors.""")
        println(sos)
        assertEquals(
            1, errors,
            "There should be exactly one checkstyle error!"
        )

        checker.destroy()
    }
}