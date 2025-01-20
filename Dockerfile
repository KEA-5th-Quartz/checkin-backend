# 1. Java 이미지를 베이스로 사용
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 애플리케이션 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 4. JAR 파일 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]