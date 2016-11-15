package org.cloudfoundry.community.servicebroker.brooklyn.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.http.HttpToolResponse;
import org.apache.brooklyn.util.stream.Streams;
import org.apache.brooklyn.util.text.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.util.Maps;

@RestController
public class BrooklynController {

    private BrooklynRestAdmin admin;
    private BrooklynServiceInstanceRepository instanceRepository;
    private HttpServletRequest context;
    private BrooklynConfig config;
    private HttpClient httpClient;

    @Autowired
    public BrooklynController(BrooklynRestAdmin admin, BrooklynServiceInstanceRepository instanceRepository, HttpServletRequest context, BrooklynConfig config, HttpClient httpClient) {
        this.admin = admin;
        this.instanceRepository = instanceRepository;
        this.context = context;
        this.config = config;
        this.httpClient = httpClient;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public void create(InputStream uploadedInputStream) {
        admin.postBlueprint(Streams.readFullyString(uploadedInputStream));
        // TODO create a response
    }

    @RequestMapping(value = "/delete/{name}/{version:.+}/", method = RequestMethod.DELETE)
    public void delete(@PathVariable("name") String name, @PathVariable("version") String version) {
        try {
            admin.deleteCatalogEntry(name, version);
        } catch (Exception e) {
            // TODO create a response
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/invoke/{application}/{entity}/{effector}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Object invoke(
            @PathVariable("application") String application,
            @PathVariable("entity") String entity,
            @PathVariable("effector") String effector,
            @RequestBody Map<String, Object> params) {

        BrooklynServiceInstance instance = instanceRepository.findOne(application);
        if (instance != null) {
            String appId = instance.getServiceDefinitionId();
            return admin.invokeEffector(appId, entity, effector, params);
        }

        return new Object();
    }

    @RequestMapping(value = "/effectors/{application}", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> effectors(@PathVariable("application") String application) {
        BrooklynServiceInstance instance = instanceRepository.findOne(application);
        if (instance != null) {
            String appId = instance.getServiceDefinitionId();
            Future<Map<String, Object>> applicationEffectorsFuture = admin.getApplicationEffectors(appId);
            return ServiceUtil.getFutureValueLoggingError(applicationEffectorsFuture);
        }
        return Collections.emptyMap();
    }

    @RequestMapping(value = "/sensors/{application}", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> sensors(@PathVariable("application") String application) {
        BrooklynServiceInstance instance = instanceRepository.findOne(application);
        if (instance != null) {
            String appId = instance.getServiceDefinitionId();
            Future<Map<String, Object>> applicationSensorsFuture = admin.getApplicationSensors(appId);
            return ServiceUtil.getFutureValueLoggingError(applicationSensorsFuture);
        }
        return Collections.emptyMap();
    }

    @RequestMapping(value = "/is-running/{application}")
    public @ResponseBody Boolean isRunning(@PathVariable("application") String application) {
        BrooklynServiceInstance instance = instanceRepository.findOne(application);
        if (instance != null) {
            String appId = instance.getServiceDefinitionId();
            Future<Boolean> applicationRunningFuture = admin.isApplicationRunning(appId);
            return ServiceUtil.getFutureValueLoggingError(applicationRunningFuture);
        }
        return false;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.DELETE, RequestMethod.POST}, value = "/brooklyn/**", produces = "application/json")
    public @ResponseBody Object proxy(HttpServletRequest request) throws URISyntaxException, IOException {
        String[] split = request.getServletPath().split("/");
        String[] vars = Arrays.copyOfRange(split, 2, split.length);
        StringBuilder builder = new StringBuilder()
                .append(config.toFullUrl(vars));
        if (!Strings.isBlank(request.getQueryString())) {
            builder.append("?" + request.getQueryString());
        }
        String url = builder.toString();
        HttpToolResponse response = null;
        switch(request.getMethod()) {
            case "GET":
                response = HttpTool.httpGet(httpClient, new URI(url), null);
                break;
            case "DELETE":
                response = HttpTool.httpDelete(httpClient, new URI(url), null);
                break;
            case "POST":
                Map<String, String[]> parameterMap = request.getParameterMap();
                if (parameterMap.isEmpty()) {
                    byte[] bytes = IOUtils.toByteArray(request.getInputStream());
                    response = HttpTool.httpPost(httpClient, new URI(url), null, bytes);
                } else {
                    Map<String, String> params = Maps.newLinkedHashMap();
                    for (String key : parameterMap.keySet()) {
                        params.put(key, request.getParameter(key));
                    }
                    response = HttpTool.httpPost(httpClient, new URI(url), null, params);
                }
                break;
        }
        if (!HttpTool.isStatusCodeHealthy(response.getResponseCode()))
            throw new RuntimeException("Error forwarding query to Brooklyn: " + url);
        return response.getContentAsString();
    }

}
