package com.company.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.commons.io.IOUtils
import org.bonitasoft.engine.api.CommandAPI
import org.bonitasoft.engine.command.CommandUpdater
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        // To retrieve query parameters use the request.getParameter(..) method.
        // Be careful, parameter values are always returned as String values

        // Retrieve p parameter
        def p = request.getParameter "p"
        if (p == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter p is missing"}""")
        }
		p = Integer.parseInt(p);
		
        // Retrieve c parameter
        def c = request.getParameter "c"
        if (c == null) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter c is missing"}""")
        }
		c = Integer.parseInt(c);

		def o = request.getParameter "o"
		
        // Here is an example of how you can retrieve configuration parameters from a properties file
        // It is safe to remove this if no configuration is required
        Properties props = loadProperties("configuration.properties", context.resourceProvider)
        String paramValue = props["myParameterKey"]

        /* 
         * Execute business logic here
         * 
         * 
         * Your code goes here
         * 
         * 
         */
        // Prepare the result
		
		def CommandAPI api = context.apiClient.getCommandAPI();
		String commandName = "listLogs";
		String depsName = "listLogs.jar";

		def searchResult = api.searchCommands(new SearchOptionsBuilder(0,1).searchTerm(commandName).done());
		def byte[] jar = IOUtils.toByteArray(context.resourceProvider.getResourceAsStream("queriable-logs-1.0-SNAPSHOT.jar"));
		
		if(searchResult.getCount() != 0) {
			api.unregister(searchResult.getResult().get(0).getName());
		}
		if(searchResult.getCount() != 0) {
			api.removeDependency(depsName);
		}
		api.addDependency(depsName, jar);
		api.register(commandName, "List queriable logs", "org.bonitasoft.logs.LogsCommand");
		
		def SearchResult result = api.execute(commandName, ["p":p, "c":c, "o": o]);
		

        // Send the result as a JSON representation
        // You may use buildPagedResponse if your result is multiple
        return buildPagedResponse(responseBuilder, new JsonBuilder(result.getResult()).toString(), p, c, result.getCount())
    }

    /**
     * Build an HTTP response.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  httpStatus the status of the response
     * @param  body the response body
     * @return a RestAPIResponse
     */
    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }

    /**
     * Returns a paged result like Bonita BPM REST APIs.
     * Build a response with a content-range.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  body the response body
     * @param  p the page index
     * @param  c the number of result per page
     * @param  total the total number of results
     * @return a RestAPIResponse
     */
    RestApiResponse buildPagedResponse(RestApiResponseBuilder responseBuilder, Serializable body, int p, int c, long total) {
        return responseBuilder.with {
            withContentRange(p,c,total)
            withResponse(body)
            build()
        }
    }

    /**
     * Load a property file into a java.util.Properties
     */
    Properties loadProperties(String fileName, ResourceProvider resourceProvider) {
        Properties props = new Properties()
        resourceProvider.getResourceAsStream(fileName).withStream { InputStream s ->
            props.load s
        }
        props
    }

}
