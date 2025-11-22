# leonBlog-be

leonBlog 后端服务，基于 Spring Boot 提供个人资料与文章 API 接口。

## 技术栈

- Spring Boot 3.2.5
- Java 17
- Maven

## 开发环境

- JDK 17+
- Maven 3.6+

## 本地开发

### 运行服务

1. 配置数据目录路径（通过环境变量或配置文件）：
   ```bash
   export BLOG_DATA_PATH=/path/to/your/blog-data
   export BLOG_BASE_URL=http://localhost:8080
   ```

2. 启动服务：
   ```bash
   mvn spring-boot:run
   ```

### 构建

构建 JAR 文件：
```bash
mvn clean package
```

构建 Docker 镜像：
```bash
docker build -t leonblog/backend:latest .
```

## API 接口

- `GET /api/profile` - 获取个人资料
- `GET /api/articles` - 获取文章列表
- `GET /api/articles/{id}` - 获取文章详情

## 部署

部署说明请参考 [leonBlog-deploy](https://github.com/yourusername/leonBlog-deploy) 项目。
