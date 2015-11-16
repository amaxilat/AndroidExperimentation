package eu.smartsantander.androidExperimentation.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.smartsantander.androidExperimentation.controller.BaseController;
import eu.smartsantander.androidExperimentation.model.Smartphone;
import eu.smartsantander.androidExperimentation.service.ModelManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/api/v1")
public class SmartphoneController extends BaseController {

    /**
     * a log4j logger to print messages.
     */
    private static final Logger LOGGER = Logger.getLogger(SmartphoneController.class);

    @Autowired
    ModelManager modelManager;

    /**
     * Registers a {@see Smartphone} to the service.
     *
     * @return the phoneId generated or -1 if there was a error.
     */
    @ResponseBody
    @RequestMapping(value = "/smartphone", method = RequestMethod.POST)
    public Integer registerSmartphone(@RequestBody String smartphoneString) {
        try {
            LOGGER.info(smartphoneString);
            Smartphone smartphone = new ObjectMapper().readValue(smartphoneString, Smartphone.class);
            smartphone = modelManager.registerSmartphone(smartphone);
            LOGGER.info("register Smartphone: Device:" + smartphone.getId());
            LOGGER.info("register Smartphone: Device Sensor Rules:" + smartphone.getSensorsRules());
            LOGGER.info("register Smartphone: Device Type:" + smartphone.getDeviceType());
            LOGGER.info("----------------------.-------------");
            return smartphone.getId();
        } catch (Exception e) {
            LOGGER.error(e, e);
            LOGGER.debug(e.getMessage());
        }
        return -1;
    }
}
