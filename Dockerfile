# 1. Aşama: Maven ile jar üret
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Aşama: Uygulama için gerekli ortamı kur
FROM openjdk:21-jdk-slim

# Chrome ve ImageMagick kurulumları
RUN apt-get update && \
    apt-get install -y wget gnupg2 curl imagemagick fonts-liberation libappindicator3-1 libasound2 libatk-bridge2.0-0 libatk1.0-0 libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 libx11-xcb1 libxcomposite1 libxdamage1 libxrandr2 xdg-utils && \
    wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list' && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# ChromeDriver'ı daha dayanıklı şekilde kur
RUN set -eux; \
    CHROME_VERSION=$(google-chrome --version | grep -oP '\\d+\\.\\d+\\.\\d+') || CHROME_VERSION=""; \
    if [ -n "$CHROME_VERSION" ]; then \
      DRIVER_VERSION=$(curl -sS "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION%.*}" || curl -sS https://chromedriver.storage.googleapis.com/LATEST_RELEASE); \
    else \
      DRIVER_VERSION=$(curl -sS https://chromedriver.storage.googleapis.com/LATEST_RELEASE); \
    fi; \
    wget -O /tmp/chromedriver.zip "https://chromedriver.storage.googleapis.com/${DRIVER_VERSION}/chromedriver_linux64.zip"; \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/; \
    chmod +x /usr/local/bin/chromedriver; \
    rm /tmp/chromedriver.zip

# Jar dosyasını build aşamasından kopyala
COPY --from=build /app/target/*.jar /app/whatnext.jar

WORKDIR /app

EXPOSE 8080

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "whatnext.jar"] 