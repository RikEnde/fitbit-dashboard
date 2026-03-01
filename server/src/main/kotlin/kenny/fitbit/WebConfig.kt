package kenny.fitbit

import jakarta.servlet.Filter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

@Configuration
class WebConfig : WebMvcConfigurer {

    /** Forward bare "/" to "/index.html" so SvelteKit boots with window.location == "/" */
    @Bean
    fun spaRootFilter(): Filter = Filter { request, response, chain ->
        val req = request as HttpServletRequest
        if (req.requestURI == "/" && req.method.equals("GET", ignoreCase = true)) {
            req.getRequestDispatcher("/index.html").forward(request, response)
        } else {
            chain.doFilter(request, response)
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? =
                    super.getResource(resourcePath, location)
                        ?: super.getResource("index.html", location)
            })
    }
}
