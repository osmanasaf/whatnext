FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -q

FROM openjdk:21-jdk-slim
RUN apt-get update -qq && apt-get install -qq -y --no-install-recommends \
    wget curl unzip imagemagick ca-certificates fonts-liberation \
    libayatana-appindicator3-1 libasound2 libatk-bridge2.0-0 libatk1.0-0 \
    libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 libx11-xcb1 \
    libxcomposite1 libxdamage1 libxrandr2 xdg-utils \
    gnupg2 software-properties-common >/dev/null && \
    rm -rf /var/lib/apt/lists/*

# Chrome ve ChromeDriver'ı yükle
ENV CHROME_FOR_TESTING_VERSION=138.0.7204.49
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver

# Chrome'u yükle
RUN wget -q -O /tmp/chrome.zip \
    "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chrome-linux64.zip" && \
    unzip -qq /tmp/chrome.zip -d /usr/local && \
    ln -s /usr/local/chrome-linux64/chrome /usr/bin/google-chrome && \
    chmod +x /usr/bin/google-chrome && \
    rm /tmp/chrome.zip

# ChromeDriver'ı yükle
RUN wget -q -O /tmp/chromedriver.zip \
    "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chromedriver-linux64.zip" && \
    unzip -qq /tmp/chromedriver.zip -d /tmp && \
    mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/ && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver*

# Chrome ve ChromeDriver'ın doğru yüklendiğini kontrol et
RUN google-chrome --version && chromedriver --version

# Environment variables for Chrome
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver
ENV WEBDRIVER_CHROME_DRIVER=/usr/local/bin/chromedriver

COPY --from=build /app/target/*.jar /app/whatnext.jar

WORKDIR /app
EXPOSE 8080

CMD ["java", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/dev/stdout", "-jar", "whatnext.jar"]
