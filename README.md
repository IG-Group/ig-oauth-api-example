## IG OAuth API Sample Application

### Overview
The sample backend application to show how to establish an OAuth 2.0 flow with the IG authorization server in order to use
it with frontend application run on the same host.

### Getting started
Configure the application:

    application.properties

You will need to set your OAuth client id, OAuth client secret, OAuth scope, your server's hostname and port if required.

Use maven 3 or later to build the project:

    mvn clean install
    
This will create a Spring Boot executable jar file. To start the web application:

    java -jar target/ig-oauth-server-example-0.0.1-SNAPSHOT.jar
    
### Frontend example
You can visit an example application that uses this service: [frontend](https://github.com/IG-Group/fix-ws-client-example)

### Endpoints

The application exposes 3 endpoints:

    GET /oauth2/authorize?redirect_uri=<URI>
The frontend application should be redirected to this endpoint to get access_token, refresh_token and client_id from OAuth2 provider.
The requested query parameter is the redirection URL to which the server will send tokens and client ID.

    POST /oauth2/token { code: <generated code from successful redirect> }
 
After successfully redirecting, in order to finally receive an access token, request with code in body of request.
The response is as follows:

    { access_token: string, expires_in: string }
    Set-Cookie: refreshToken=string
     
Notice the refreshToken as a cookie header in response. this is a **private** cookie that the browser must attach to refresh tokens.

    GET /oauth2/refresh

The endpoint for generating new tokens based on refresh token. The refresh token is served once again in a private cookie.
    
### Communication with OAuth provider

    GET /oauth2/authorize           (response_type = code)
to the IG authorization server which will prompt the user for their IG credentials and ask them to consent IG to provide
the vendor with an authorization code. The IG authorization server will then return an authorization code on a 403 redirect 
response (specified by the vendor on the "redirect_url" query parameter). The redirect url needs to match the url configured
on the authorization server for security reasons which in this sample is configured for 

    /api-vendor-sample/authorization-handler
    
The authorization handler will retrieve the authorization code from the redirect request's query parameters and then

1) Request access and refresh tokens    

    POST /oauth2/access_token        (grant_type = authorization_code)    
2) Obtain the user's client id         

    GET  /oauth2/userinfo    
3) Redirect the frontend app to provided page with the access token, refresh token and client ID