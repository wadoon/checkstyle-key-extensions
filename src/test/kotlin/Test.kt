import com.puppycrawl.tools.checkstyle.Main
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolute

/**
 *
 * @author Alexander Weigl
 * @version 1 (7/21/25)
 */
class Test {
    @Test
    fun test() {
        System.setProperty("home.dir", Paths.get(".").absolute().toString())
        Main.main("-c", "src/test/resources/config.xml", "share/")
    }
}