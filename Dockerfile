# 1. Aşama: Maven ile jar üret
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Aşama: Runtime ortamı
FROM openjdk:21-jdk-slim

# Sistem bağımlılıklarını yükle
RUN apt-get update && apt-get install -y \
    wget curl gnupg2 unzip imagemagick fonts-liberation \
    libayatana-appindicator3-1 libasound2 libatk-bridge2.0-0 \
    libatk1.0-0 libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 \
    libnspr4 libnss3 libx11-xcb1 libxcomposite1 \
    libxdamage1 libxrandr2 xdg-utils ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Chrome ve ChromeDriver'ı Chrome-for-Testing ile kur (138 sürümü)
ENV CHROME_FOR_TESTING_VERSION=138.0.7204.49

# Chrome kurulumu
RUN wget -O /tmp/chrome.zip "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chrome-linux64.zip" && \
    unzip /tmp/chrome.zip -d /usr/local && \
    ln -s /usr/local/chrome-linux64/chrome /usr/bin/google-chrome && \
    rm /tmp/chrome.zip

# ChromeDriver kurulumu
RUN wget -O /tmp/chromedriver.zip "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chromedriver-linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin && \
    chmod +x /usr/local/bin/chromedriver && \
    rm /tmp/chromedriver.zip

# Jar dosyasını kopyala
COPY --from=build /app/target/*.jar /app/whatnext.jar

WORKDIR /app

EXPOSE 8080

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "whatnext.jar"]