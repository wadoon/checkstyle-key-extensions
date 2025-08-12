import com.puppycrawl.tools.checkstyle.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.walk
import kotlin.test.DefaultAsserter.assertTrue

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
        println(sos.toString())
        assertTrue("$errors check style errors found. $sos", errors == 0)
        checker.destroy()
    }
}