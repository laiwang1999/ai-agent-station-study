# 基础镜像
FROM openjdk:17-jdk-alpine

# 作者
MAINTAINER yang125root

# 配置
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD target/ai-agent-station-study-app.jar /ai-agent-station-study-app.jar

ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /ai-agent-station-study-app.jar $PARAMS"]