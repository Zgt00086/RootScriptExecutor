# Root Script Executor (Android 16 Compatible)

基于C/S架构和命名空间隔离的Root脚本执行器，专为Android 16+设计，具有高级隐蔽特性。

## 核心特性

### 1. C/S架构设计
- **无痕二进制释放**：完全依赖libsu的RootService功能，不在tmp目录释放固定二进制文件
- **动态IPC通道**：使用libsu在内存中构建进程间通信通道
- **随机化Socket名称**：防止安全软件的Socket扫描器遍历特征

### 2. Mount Namespace隔离
- **防御底层遍历**：在执行脚本前使用`nsenter -t 1 -m`脱离当前挂载空间
- **文件I/O隐蔽**：脚本执行产生的文件操作对全局命名空间不可见
- **临时挂载点隔离**：防止反作弊工具检测到异常挂载

### 3. 进程伪装技术
- **进程名称伪装**：启动后立即修改`/proc/self/comm`为系统服务名
- **进程树优化**：使用`exec`替换Shell进程，减少进程层级
- **PPID溯源防护**：缩短进程链条，防止通过父子关系溯源

### 4. 执行流优化
- **环境变量设置**：自动添加当前目录到PATH
- **脚本权限管理**：自动设置可执行权限
- **错误处理**：完善的异常捕获和日志记录

## 架构说明

### 客户端 (Client)
- **MainActivity**：用户界面，提供脚本上传、执行、列表查看功能
- **libsu集成**：通过RootService绑定与服务器通信

### 服务器端 (Server)
- **RootService**：运行在root进程中的服务
- **IRootService接口**：定义客户端与服务器之间的通信协议
- **命名空间隔离**：在独立mount namespace中执行脚本

## 技术实现

### 随机化IPC通道
```kotlin
// 生成随机Socket名称
val randomSocketName = "root_service_${UUID.randomUUID().toString().replace("-", "")}"
```

### Mount Namespace隔离
```bash
nsenter -t 1 -m sh -c "
    cd /data/adb/5/
    export PATH=\$PATH:.
    exec ./your_script.sh
"
```

### 进程伪装
```kotlin
// 修改进程名
val commFile = File("/proc/self/comm")
val fakeName = "system_server_watchdog"
FileOutputStream(commFile).use { fos ->
    fos.write(fakeName.toByteArray())
}
```

## 使用方法

### 1. 构建应用
```bash
cd /root/RootScriptExecutor
./gradlew assembleDebug
```

### 2. 安装应用
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 使用流程
1. 启动应用，自动连接RootService
2. 输入脚本名称和内容
3. 点击"Upload"上传脚本到`/data/adb/5/`
4. 点击"Execute"在隔离命名空间中执行
5. 点击"List Scripts"查看可用脚本

### 4. 示例脚本
项目根目录包含`example_script.sh`，展示：
- 系统信息获取
- 命名空间验证
- 进程树检查
- 文件系统操作
- 网络测试

## 安全特性

### 反检测机制
1. **无文件释放**：不在磁盘留下二进制痕迹
2. **随机Socket**：每次启动使用不同的IPC通道
3. **进程伪装**：伪装成系统核心服务
4. **命名空间隔离**：文件操作对全局不可见
5. **进程树优化**：减少可溯源的进程层级

### 兼容性
- Android 16+ (API 34+)
- 支持ARM/x86架构
- 需要Magisk或SuperSU等root方案
- 兼容libsu 5.2.1+

## 目录结构
```
RootScriptExecutor/
├── app/
│   ├── src/main/java/com/example/rootscriptexecutor/
│   │   ├── MainActivity.kt          # 客户端界面
│   │   ├── RootService.kt           # 服务器端服务
│   │   └── IRootService.kt          # IPC接口定义
│   ├── src/main/res/                # 资源文件
│   └── build.gradle                 # 模块配置
├── build.gradle                     # 项目配置
├── settings.gradle                  # 项目设置
├── gradle.properties                # Gradle属性
├── example_script.sh                # 测试脚本
└── README.md                        # 本文档
```

## 依赖库
- libsu-core: 5.2.1 (root访问)
- libsu-service: 5.2.1 (RootService)
- libsu-nio: 5.2.1 (文件操作)
- AndroidX组件

## 注意事项

1. **权限要求**：需要完整的root权限
2. **Android版本**：最低API 24，针对API 34+优化
3. **安全警告**：仅用于合法用途
4. **测试环境**：建议在测试设备上验证

## 故障排除

### 常见问题
1. **RootService连接失败**：检查设备root状态和libsu版本
2. **脚本执行失败**：验证脚本权限和语法
3. **命名空间隔离无效**：检查内核是否支持mount namespace

### 日志查看
```bash
adb logcat -s "RootService|MainActivity"
```

## 许可证
本项目仅供学习和研究使用，请遵守相关法律法规。