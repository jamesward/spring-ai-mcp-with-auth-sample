# Spring AI MCP with Auth Sample

This sample has a Spring AI MCP server that requires authentication.

The MCP server can be started in dev mode to disable auth:
```
./gradlew bootTestRun
```

In dev mode the server can be used with the MCP Inspector.

The MCP URL is: `http://localhost:8090/mcp`

To test with auth:
```
./gradlew bootRun
```

You can now add the MCP server to a local AI agent to test the login flow.
Note: With auth enabled, the MCP Inspector is not able to auth / access the MCP server.

