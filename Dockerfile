# 1. Aşama: Maven ile jar üret
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Aşama: Uygulama ortamı + Chrome + ChromeDriver + ImageMagick
FROM openjdk:21-jdk-slim

# Sistem bağımlılıklarını yükle
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
 && rm -rf /var/lib/apt/lists/*

# Google Chrome kur
RUN mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /etc/apt/keyrings/google-chrome.gpg && \
    echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# ChromeDriver (sabit, uyumlu sürüm)
ENV CHROME_DRIVER_VERSION=137.0.7012.75

RUN wget -O /tmp/chromedriver.zip "https://chromedriver.storage.googleapis.com/${CHROME_DRIVER_VERSION}/chromedriver_linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    chmod +x /usr/local/bin/chromedriver && \
    rm /tmp/chromedriver.zip

# Jar dosyasını kopyala
COPY --from=build /app/target/*.jar /app/whatnext.jar

WORKDIR /app

EXPOSE 8080

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "whatnext.jar"]
