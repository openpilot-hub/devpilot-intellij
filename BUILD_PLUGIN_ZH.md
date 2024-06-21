# 前提条件

如果想构建一个完整的属于你自己的DevPilot应用，需要有如下几个条件：
1. AI网关：用于兼容不同的LLM模型，并提供API给插件使用 [网关仓库](https://github.com/openpilot-hub/devpilot-gateway)
2. 权限系统：用于校验插件用户的登录和使用权限（可以通过设置`DefaultConst.AUTH_ON`为false来关闭）
3. 指标系统：用于处理用户上报的使用数据用于分析（可以通过设置`DefaultConst.TELEMETRY_ON`为false来关闭）