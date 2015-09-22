package eu.smartsantander.androidExperimentation.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * @author Dimitrios Amaxilatis.
 */
@Controller
public class PluginController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(PluginController.class);
    @Value("${plugins.dir}")
    String pluginsDir;

    @RequestMapping(value = "/dynamixRepository/{pluginName}.jar", method = RequestMethod.GET)
    public String downloadPlugin(@PathVariable("pluginName") final String pluginName, final HttpServletResponse httpServletResponse) {
        try {
            final InputStream is = new BufferedInputStream(new FileInputStream(new File(pluginsDir + pluginName + ".jar")));
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + pluginName + ".jar\"");
            httpServletResponse.setContentType("data:text/plaincharset=utf-8");
            FileCopyUtils.copy(is, httpServletResponse.getOutputStream());
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
        return null;


    }
}
