# Prerequisite

If you want to build your own complete plugin like DevPilot, there are some required condition:
1. AI gateway: support multi LLM model and provide api for plugin [Gateway repo](https://github.com/openpilot-hub/devpilot-gateway)
2. Auth System: support authorization check for login user (You can close it by setting `DefaultConst.AUTH_ON` to false)
3. Telemetry System: upload user behavior data for analysis (You can close it by setting `DefaultConst.TELEMETRY_ON` to false)
4. Request Encoding: request will be encoded by base64 for security (You can close it by setting `DefaultConst.REQUEST_ENCODING_ON` to false))