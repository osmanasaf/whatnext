FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update -qq && apt-get install -qq -y --no-install-recommends \
    wget curl unzip imagemagick ca-certificates fonts-liberation \
    libayatana-appindicator3-1 libasound2 libatk-bridge2.0-0 libatk1.0-0 \
    libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 libx11-xcb1 \
    libxcomposite1 libxdamage1 libxrandr2 xdg-utils \
    gnupg2 software-properties-common libgbm1 libxshmfence1 >/dev/null && \
    rm -rf /var/lib/apt/lists/*

RUN which magick || which convert || ls -l /usr/bin/ | grep -i magick
RUN convert -version || echo 'convert yok'

ENV CHROME_FOR_TESTING_VERSION=138.0.7204.49
RUN wget -q -O /tmp/chrome.zip \
    "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chrome-linux64.zip" && \
    unzip -qq /tmp/chrome.zip -d /usr/local && \
    chmod +x /usr/local/chrome-linux64/chrome && \
    rm /tmp/chrome.zip

RUN wget -q -O /tmp/chromedriver.zip \
    "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chromedriver-linux64.zip" && \
    unzip -qq /tmp/chromedriver.zip -d /tmp && \
    mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/ && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver*

RUN wget -q -O /tmp/headless-shell.zip \
    "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FOR_TESTING_VERSION}/linux64/chrome-headless-shell-linux64.zip" && \
    unzip -qq /tmp/headless-shell.zip -d /usr/local && \
    chmod +x /usr/local/chrome-headless-shell-linux64/chrome-headless-shell && \
    rm /tmp/headless-shell.zip

RUN ls -la /usr/local/chrome-linux64/ && ls -la /usr/local/bin/chromedriver
ENV CHROME_BIN=/usr/local/chrome-linux64/chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver
ENV WEBDRIVER_CHROME_DRIVER=/usr/local/bin/chromedriver
ENV CHROME_HEADLESS_SHELL=/usr/local/chrome-headless-shell-linux64/chrome-headless-shell

RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/whatnext.jar
COPY --from=build /app/video /app/video

WORKDIR /app/video
RUN npm ci --omit=dev

WORKDIR /app
EXPOSE 8080

ENV WHATNEXT_VIDEO_PROJECT_PATH=/app/video
CMD ["java", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/dev/stdout", "-jar", "whatnext.jar"]
