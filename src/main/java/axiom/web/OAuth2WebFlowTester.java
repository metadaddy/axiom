package axiom.web;

import axiom.oauth2.OauthContext;
import com.opensymphony.xwork2.ActionContext;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan Brainard
 * @since 2011-08-22
 */
public class OAuth2WebFlowTester extends OAuthSupport {

    private OauthContext oauthContext;

    public OAuth2WebFlowTester() {
//        final String host = "login.salesforce.com";
//
//        String requestURL = getServletRequest()
//                                .getRequestURL()
//                                .toString()
//                                .replaceFirst(".jsp", ".action");
//
//        if (!requestURL.contains("localhost")) {
//            requestURL = requestURL.replaceFirst("http", "https");
//        }

//        this.oauthContext = new OauthContext(host, requestURL);

        this.oauthContext = new OauthContext("login.salesforce.com", "xxx");
    }

    @Override
    public Breadcrumbable getParentPage() {
        return new OAuthHome();
    }

    public String redirectForAuthorization() throws UnsupportedEncodingException {
        if (oauthContext.getAuthRequestUrl() == null) {
            addActionError("Must provide an Authorization URL");
            return INPUT; //TODO: should be error?
        }

        return SUCCESS;
    }

    public String handleAuthorizationCode() throws URISyntaxException {
        oauthContext.setFieldsFrom(sanitizeParameterArrays(ActionContext.getContext().getParameters()));

        if (oauthContext.getError() != null) {
            addActionError(oauthContext.getError() + ": " + oauthContext.getError_description());
            return ERROR;
        }

        if (oauthContext.getCode() == null || "".equals(oauthContext.getCode())) {
            addActionError(oauthContext.getError() + ": " + oauthContext.getError_description());
            return ERROR;
        }

        return SUCCESS;
    }

    public String requestAccessToken() {
        final HttpClient client = new HttpClient();
        final PostMethod tokenRequest = new PostMethod(oauthContext.getTokenRequestUrl());
        try {
            int statusCode = client.executeMethod(tokenRequest);

            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception(tokenRequest.getStatusLine() + "\n" + new String(tokenRequest.getResponseBody()));
            }

            //noinspection unchecked
            oauthContext.setFieldsFrom((JSONObject) JSONSerializer.toJSON(new String(tokenRequest.getResponseBody())));

            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            addActionError(e.getMessage());
            return ERROR;
        } finally {
            tokenRequest.releaseConnection();
        }
    }

    public OauthContext getOauthContext() {
        return oauthContext;
    }

    private static Map<String, String> sanitizeParameterArrays(Map<String, Object> params) {
        Map<String, String> sanitizedParams = new HashMap<String, String>();
        for (Map.Entry<String, Object> p : params.entrySet()) {
            sanitizedParams.put(p.getKey(), ((String[]) p.getValue())[0]);
        }
        return sanitizedParams;
    }

}