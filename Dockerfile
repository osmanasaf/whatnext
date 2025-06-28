# 1. Aşama: Maven ile jar üret
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Aşama: Runtime ortamı + Chrome + ChromeDriver + ImageMagick
FROM openjdk:21-jdk-slim

# Gerekli sistem paketlerini yükle
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg2 \
    unzip \
    imagemagick \
    fonts-liberation \
    libayatana-appindicator3-1 \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libgdk-pixbuf2.0-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xdg-utils \
    ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Google Chrome 126 kur
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get update && \
    apt-get install -y ./google-chrome-stable_current_amd64.deb && \
    rm google-chrome-stable_current_amd64.deb && \
    rm -rf /var/lib/apt/lists/*

# ChromeDriver 126.0.6478.126 kur
ENV CHROME_DRIVER_VERSION=126.0.6478.126

RUN wget -O /tmp/chromedriver.zip "https://chromedriver.storage.googleapis.com/${CHROME_DRIVER_VERSION}/chromedriver_linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    chmod +x /usr/local/bin/chromedriver && \
    rm /tmp/chromedriver.zip

# Jar dosyasını kopyala
COPY --from=build /app/target/*.jar /app/whatnext.jar

WORKDIR /app

EXPOSE 8080

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "whatnext.jar"]
