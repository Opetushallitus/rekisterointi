#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail

function main {
  select_java_version "21"

  VALTUUDET_API_KEY=$(aws ssm get-parameter --profile oph-dev --output text --with-decryption --query Parameter.Value --name /untuva/services/varda-rekisterointi/valtuudet-api-key)
  VALTUUDET_CLIENT_ID=$(aws ssm get-parameter --profile oph-dev --output text --with-decryption --query Parameter.Value --name /untuva/services/varda-rekisterointi/valtuudet-client-id)
  VALTUUDET_OAUTH_PASSWORD=$(aws ssm get-parameter --profile oph-dev --output text --with-decryption --query Parameter.Value --name /untuva/services/varda-rekisterointi/valtuudet-oauth-password)
  SERVICE_USERNAME=$(aws ssm get-parameter --profile oph-dev --output text --with-decryption --query Parameter.Value --name /untuva/services/varda-rekisterointi/service-username)
  SERVICE_PASSWORD=$(aws ssm get-parameter --profile oph-dev --output text --with-decryption --query Parameter.Value --name /untuva/services/varda-rekisterointi/service-password)

  mvn spring-boot:run \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="
      -Drekisterointi.service.username=$SERVICE_USERNAME
      -Drekisterointi.service.password=$SERVICE_PASSWORD
      -Drekisterointi.valtuudet.api-key=$VALTUUDET_API_KEY
      -Drekisterointi.valtuudet.client-id=$VALTUUDET_CLIENT_ID
      -Drekisterointi.valtuudet.oauth-password=$VALTUUDET_OAUTH_PASSWORD
      -DbaseUrl=https://virkailija.untuvaopintopolku.fi
      -Drekisterointi.baseUrl=http://localhost:3000
      -Dcas-oppija.baseUrl=https://untuvaopintopolku.fi"
}

function select_java_version {
  java_version="$1"
  JAVA_HOME="$(/usr/libexec/java_home --failfast --version "${java_version}" > /dev/null 2>&1 || fatal "JDK version ${java_version} required but not installed")"
  export JAVA_HOME
}

function fatal {
  log "ERROR" "$1"
  exit 1
}

function log {
  local -r level="$1"
  local -r message="$2"
  local -r timestamp=$(date +"%Y-%m-%d %H:%M:%S")

  echo >&2 -e "${timestamp} ${level} ${message}"
}

main
