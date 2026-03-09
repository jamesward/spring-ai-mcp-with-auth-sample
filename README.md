# Spring AI MCP with Auth Sample

This sample has a Spring AI MCP server and a Spring Auth server providing auth to the MCP server.

It includes some production oriented aspects:
- Disable OAuth Consent (assuming you want that UX)
- Long-lived tokens so user doesn't have to re-auth often
- Prod stable JWT private key
- One-time-token auth that assumes users login with their email
- Dev mode for local testing which optionally disables prod-oriented aspects (auth: notifications and real users; mcp: auth)
- Custom UI with JTE & pre-compiled templates for login server
- Virtual threads
- User cache


## Auth Server

```
cd auth-server
```

Run the auth server in dev mode:
```
./gradlew bootTestRun
```

Test the login flow by opening `http://localhost:9000/`

In dev mode the test user's email is `test@example.com` and the OTP will be displayed in the STDOUT for the login server.

By default the server generates a random RSA key pair on each startup, which invalidates existing tokens on restart. For production, set `JWK_RSA_PRIVATE_KEY` to use a stable key.

Generate a key:
```
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -outform PEM -out jwk-private.pem
```

Set the env var (the value is the full PEM including headers):
```
export JWK_RSA_PRIVATE_KEY="$(cat jwk-private.pem)"
```

Then delete the file:
```
rm jwk-private.pem
```

Generate a prod container:
```
./gradlew bootBuildImage
```

## MCP Server

```
cd mcp-server
```

The MCP server can be started in dev mode to disable auth:
```
./gradlew bootTestRun
```

In dev mode the server can be used with the MCP Inspector.

The MCP URL is: `http://localhost:8090/`

To test with auth, make sure the auth server is running:
```
export ISSUER_URI=http://localhost:9000
./gradlew bootRun
```

You can now add the MCP server to a local AI agent to test the login flow.
Note: With auth enabled, the MCP Inspector is not able to auth / access the MCP server.

