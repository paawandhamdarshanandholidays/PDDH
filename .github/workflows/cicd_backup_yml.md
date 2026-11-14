name: CI Pipeline (Angular + Spring Microservices)

on:
  push:
    branches:
      - master
      - release
      - integration
      - develop
      - 'feature/*'

permissions:
  contents: read
  actions: write

jobs:
  build-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
          cache-dependency-path: webapp/package-lock.json
      - name: Install frontend dependencies
        working-directory: webapp
        run: npm ci --ignore-scripts
      - name: Build frontend
        working-directory: webapp
        run: npm run build -- --configuration production
      - name: Test frontend
        working-directory: webapp
        run: npm test -- --watch=false

  build-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
          cache-dependency-path: backend/pom.xml
      - name: Fix NewTopic partitions method in test
        run: |
          sed -i 's/\.partitions()/\.numPartitions()/g' backend/role-profile-common/src/test/java/com/pddh/roleprofile/RoleProfileConfigurationTest.java
      - name: Verify all backend services
        run: mvn -B -f backend/pom.xml clean verify
      - name: Analyze backend coverage in SonarCloud
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: >-
          mvn -B -f backend/pom.xml
          org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar
          -Dsonar.organization=${{ vars.SONAR_ORGANIZATION }}
          -Dsonar.projectKey=${{ vars.SONAR_PROJECT_KEY }}
          -Dsonar.qualitygate.wait=true
          -Dsonar.qualitygate.timeout=600
      - name: Delete old backend JAR artifacts
        if: github.ref == 'refs/heads/master'
        uses: actions/github-script@v7
        with:
          script: |
            const response = await github.rest.actions.listArtifactsForRepo({
              owner: context.repo.owner,
              repo: context.repo.repo,
            });
            for (const artifact of response.data.artifacts) {
              if (artifact.name === 'backend-jars') {
                console.log(`Deleting old artifact: ${artifact.id} (${artifact.name})`);
                await github.rest.actions.deleteArtifact({
                  owner: context.repo.owner,
                  repo: context.repo.repo,
                  artifact_id: artifact.id,
                });
              }
            }
      - name: Upload backend service JARs
        if: github.ref == 'refs/heads/master'
        uses: actions/upload-artifact@v4
        with:
          name: backend-jars
          path: backend/*/target/*.jar
          if-no-files-found: error
          retention-days: 1

  publish-docker-images:
    runs-on: ubuntu-latest
    needs:
      - build-frontend
      - build-backend
    if: github.event_name == 'push' && github.ref == 'refs/heads/master'
    strategy:
      fail-fast: false
      matrix:
        include:
          - image: frontend
            context: ./webapp
            dockerfile: ./webapp/Dockerfile
          - image: admin-service
            context: ./backend/admin-service
            dockerfile: ./backend/Dockerfile
          - image: api-gateway
            context: ./backend/api-gateway
            dockerfile: ./backend/Dockerfile
          - image: auth-service
            context: ./backend/auth-service
            dockerfile: ./backend/Dockerfile
          - image: booking-service
            context: ./backend/booking-service
            dockerfile: ./backend/Dockerfile
          - image: ceo-service
            context: ./backend/ceo-service
            dockerfile: ./backend/Dockerfile
          - image: contact-service
            context: ./backend/contact-service
            dockerfile: ./backend/Dockerfile
          - image: customer-service
            context: ./backend/customer-service
            dockerfile: ./backend/Dockerfile
          - image: events-service
            context: ./backend/events-service
            dockerfile: ./backend/Dockerfile
          - image: eureka-server
            context: ./backend/eureka-server
            dockerfile: ./backend/Dockerfile
          - image: gallery-service
            context: ./backend/gallery-service
            dockerfile: ./backend/Dockerfile
          - image: manager-service
            context: ./backend/manager-service
            dockerfile: ./backend/Dockerfile
          - image: offers-service
            context: ./backend/offers-service
            dockerfile: ./backend/Dockerfile
          - image: places-service
            context: ./backend/places-service
            dockerfile: ./backend/Dockerfile
          - image: staff-service
            context: ./backend/staff-service
            dockerfile: ./backend/Dockerfile
          - image: stays-service
            context: ./backend/stays-service
            dockerfile: ./backend/Dockerfile
          - image: temples-service
            context: ./backend/temples-service
            dockerfile: ./backend/Dockerfile
          - image: trips-service
            context: ./backend/trips-service
            dockerfile: ./backend/Dockerfile
    steps:
      - uses: actions/checkout@v4
      - name: Download backend service JARs
        if: ${{ matrix.image != 'frontend' }}
        uses: actions/download-artifact@v4
        with:
          name: backend-jars
          path: backend
      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      - name: Extract image tags
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ secrets.DOCKERHUB_USERNAME }}/pawandhamdarshanandholidays-${{ matrix.image }}
          tags: |
            type=raw,value=latest
            type=sha,format=long
      - name: Build and push image
        uses: docker/build-push-action@v6
        with:
          context: ${{ matrix.context }}
          file: ${{ matrix.dockerfile }}
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
