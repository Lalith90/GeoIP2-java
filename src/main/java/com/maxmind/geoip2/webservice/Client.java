package com.maxmind.geoip2.webservice;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.maxmind.geoip2.exception.GeoIP2Exception;
import com.maxmind.geoip2.exception.HttpException;
import com.maxmind.geoip2.exception.WebServiceException;
import com.maxmind.geoip2.model.CityIspOrgLookup;
import com.maxmind.geoip2.model.CityLookup;
import com.maxmind.geoip2.model.CountryLookup;
import com.maxmind.geoip2.model.OmniLookup;

/**
 * This class provides a client API for all the GeoIP2 web service's end points.
 * The end points are Country, City, City/ISP/Org, and Omni. Each end point
 * returns a different set of data about an IP address, with Country returning
 * the least data and Omni the most.
 * 
 * Each web service end point is represented by a different model class, and
 * these model classes in turn contain multiple Record classes. The record
 * classes have attributes which contain data about the IP address.
 * 
 * If the web service does not return a particular piece of data for an IP
 * address, the associated attribute is not populated.
 * 
 * The web service may not return any information for an entire record, in which
 * case all of the attributes for that record class will be empty.
 * 
 * **Usage**
 * 
 * The basic API for this class is the same for all of the web service end
 * points. First you create a web service object with your MaxMind
 * {@code userId} and {@code licenseKey}, then you call the method corresponding
 * to a specific end point, passing it the IP address you want to look up.
 * 
 * If the request succeeds, the method call will return a model class for the
 * end point you called. This model in turn contains multiple record classes,
 * each of which represents part of the data returned by the web service.
 * 
 * If the request fails, the client class throws an exception.
 * 
 * **Exceptions**
 * 
 * For details on the possible errors returned by the web service itself, see
 * {@link http://dev.maxmind.com/geoip2/geoip/web-services the GeoIP2 web
 * service documentation}.
 * 
 * If the web service returns an explicit error document, this is thrown as a
 * {@link WebServiceException}. If some other sort of transport error occurs,
 * this is thrown as a {@link HttpException}. The difference is that the web
 * service error includes an error message and error code delivered by the web
 * service. The latter is thrown when some sort of unanticipated error occurs,
 * such as the web service returning a 500 or an invalid error document.
 * 
 * If the web service returns any status code besides 200, 4xx, or 5xx, this
 * also becomes a {@link HttpException}.
 * 
 * Finally, if the web service returns a 200 but the body is invalid, the client
 * throws a {@link GenericException}.
 */
public class Client {
    private final HttpTransport transport;
    private final int userId;
    private final String licenseKey;
    private final List<String> languages;
    private String host = "geoip.maxmind.com";

    /**
     * @param userId
     *            Your MaxMind user ID.
     * @param licenseKey
     *            Your MaxMind license key.
     */
    public Client(int userId, String licenseKey) {
        this(userId, licenseKey, null, null, null);
    }

    /**
     * @param userId
     *            Your MaxMind user ID.
     * @param licenseKey
     *            Your MaxMind license key.
     * @param languages
     *            List of language codes to use in name property from most
     *            preferred to least preferred.
     */
    public Client(int userId, String licenseKey, List<String> languages) {
        this(userId, licenseKey, languages, null, null);
    }

    /* package-private as this is just for unit testing */
    Client(int userId, String licenseKey, HttpTransport transport) {
        this(userId, licenseKey, null, null, transport);
    }

    /* package-private as this is just for unit testing */
    Client(int userId, String licenseKey, List<String> languages,
            HttpTransport transport) {
        this(userId, licenseKey, languages, null, transport);
    }

    /**
     * @param userId
     *            Your MaxMind user ID.
     * @param licenseKey
     *            Your MaxMind license key.
     * @param host
     *            The host to use.
     */
    public Client(int userId, String licenseKey, String host) {
        this(userId, licenseKey, null, host, null);
    }

    /**
     * @param userId
     *            Your MaxMind user ID.
     * @param licenseKey
     *            Your MaxMind license key.
     * @param languages
     *            List of language codes to use in name property from most
     *            preferred to least preferred.
     * @param host
     *            The host to use.
     */
    public Client(int userId, String licenseKey, List<String> languages,
            String host) {
        this(userId, licenseKey, languages, host, null);
    }

    /* package-private as we only need to specify a transport for unit testing */
    Client(int userId, String licenseKey, List<String> languages, String host,
            HttpTransport transport) {
        this.userId = userId;
        this.licenseKey = licenseKey;
        if (host != null) {
            this.host = host;
        }
        if (languages == null) {
            this.languages = Arrays.asList("en");
        } else {
            this.languages = languages;
        }
        if (transport == null) {
            this.transport = new NetHttpTransport();
        } else {
            this.transport = transport;
        }
    }

    /**
     * @return A Country lookup for the requesting IP address
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public CountryLookup country() throws GeoIP2Exception {
        return this.country(null);
    }

    /**
     * @param ipAddress
     *            IPv4 or IPv6 address to lookup.
     * @return A Country lookup for the requested IP address.
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public CountryLookup country(InetAddress ipAddress) throws GeoIP2Exception {
        return this.responseFor("country", ipAddress, CountryLookup.class);
    }

    /**
     * @return A City lookup for the requesting IP address
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public CityLookup city() throws GeoIP2Exception {
        return this.city(null);
    }

    /**
     * @param ipAddress
     *            IPv4 or IPv6 address to lookup.
     * @return A City lookup for the requested IP address.
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public CityLookup city(InetAddress ipAddress) throws GeoIP2Exception {
        return this.responseFor("city", ipAddress, CityLookup.class);
    }

    /**
     * @return A City/ISP/Org lookup for the requesting IP address
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public CityIspOrgLookup cityIspOrg() throws GeoIP2Exception {
        return this.cityIspOrg(null);
    }

    /**
     * @param ipAddress
     *            IPv4 or IPv6 address to lookup.
     * @return A City/ISP/Org lookup for the requested IP address.
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public CityIspOrgLookup cityIspOrg(InetAddress ipAddress)
            throws GeoIP2Exception {
        return this.responseFor("city_isp_org", ipAddress,
                CityIspOrgLookup.class);
    }

    /**
     * @return An Omni lookup for the requesting IP address
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public OmniLookup omni() throws GeoIP2Exception {
        return this.omni(null);
    }

    /**
     * @param ipAddress
     *            IPv4 or IPv6 address to lookup.
     * @return An Omni lookup for the requested IP address.
     * @throws GeoIP2Exception
     *             if there is an error making the request.
     */
    public OmniLookup omni(InetAddress ipAddress) throws GeoIP2Exception {
        return this.responseFor("omni", ipAddress, OmniLookup.class);
    }

    private <T extends CountryLookup> T responseFor(String path,
            InetAddress ipAddress, Class<T> cls) throws GeoIP2Exception {
        GenericUrl uri = this.createUri(path, ipAddress);
        HttpResponse response = this.getResponse(uri);

        Long content_length = response.getHeaders().getContentLength();

        if (content_length == null || content_length.intValue() <= 0) {
            throw new GeoIP2Exception("Received a 200 response for " + uri
                    + " but there was no message body.");
        }

        String body = Client.getSuccessBody(response, uri);

        InjectableValues inject = new InjectableValues.Std().addValue(
                "languages", this.languages);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        try {
            return mapper.reader(cls).with(inject).readValue(body);
        } catch (IOException e) {
            throw new GeoIP2Exception(
                    "Received a 200 response but not decode it as JSON: "
                            + body);
        }
    }

    private HttpResponse getResponse(GenericUrl uri) throws GeoIP2Exception {
        HttpRequestFactory requestFactory = this.transport
                .createRequestFactory();
        HttpRequest request;
        try {
            request = requestFactory.buildGetRequest(uri);
        } catch (IOException e) {
            throw new GeoIP2Exception("Error building request", e);
        }
        request.getHeaders().setAccept("application/json");
        request.getHeaders().setBasicAuthentication(
                String.valueOf(this.userId), this.licenseKey);

        try {
            return request.execute();
        } catch (HttpResponseException e) {
            throw Client.handleErrorStatus(e.getContent(), e.getStatusCode(),
                    uri);
        } catch (IOException e) {
            throw new GeoIP2Exception(
                    "Unknown error when connecting to web service: "
                            + e.getMessage(), e);
        }
    }

    private static String getSuccessBody(HttpResponse response, GenericUrl uri)
            throws GeoIP2Exception {
        String body;
        try {
            body = response.parseAsString();
        } catch (IOException e) {
            throw new GeoIP2Exception(
                    "Received a 200 response but not decode message body: "
                            + e.getMessage());
        }

        if (response.getContentType() == null
                || !response.getContentType().contains("json")) {
            throw new GeoIP2Exception("Received a 200 response for " + uri
                    + " but it does not appear to be JSON:\n" + body);
        }
        return body;
    }

    private static HttpException handleErrorStatus(String content, int status,
            GenericUrl uri) {
        if ((status >= 400) && (status < 500)) {
            return Client.handle4xxStatus(content, status, uri);
        } else if ((status >= 500) && (status < 600)) {
            return new HttpException("Received a server error (" + status
                    + ") for " + uri, status, uri.toURL());
        } else {
            return new HttpException("Received a very surprising HTTP status ("
                    + status + ") for " + uri, status, uri.toURL());
        }
    }

    private static HttpException handle4xxStatus(String body, int status,
            GenericUrl uri) {

        if (body == null) {
            return new HttpException("Received a " + status + " error for "
                    + uri + " with no body", status, uri.toURL());
        }

        Map<String, String> content;
        try {
            ObjectMapper mapper = new ObjectMapper();
            content = mapper.readValue(body,
                    new TypeReference<HashMap<String, String>>() {
                    });
        } catch (IOException e) {
            return new HttpException("Received a " + status + " error for "
                    + uri + " but it did not include the expected JSON body: "
                    + body, status, uri.toURL());
        }

        String error = content.get("error");
        String code = content.get("code");

        if (error == null || code == null) {
            return new HttpException(
                    "Response contains JSON but it does not specify code or error keys: "
                            + body, status, uri.toURL());
        }

        return new WebServiceException(content.get("error"),
                content.get("code"), status, uri.toURL());
    }

    private GenericUrl createUri(String path, InetAddress ipAddress) {
        return new GenericUrl("https://" + this.host + "/geoip/v2.0/" + path
                + "/" + (ipAddress == null ? "me" : ipAddress.getHostAddress()));

    }
}
