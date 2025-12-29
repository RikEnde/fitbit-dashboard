package kenny.fitbit

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class GraphiQlController {
    @GetMapping("/graphiql", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun graphiql(): String {
        return ClassPathResource("graphiql/index.html").inputStream.bufferedReader().readText()
    }
}
