#!/bin/bash
set -x

#ansible-playbook -e "user=${PROD_USER}" -u "${PROD_USER}" -i "${PROD_HOST}," --private-key="${SSH_KEY}" deploy-prod.yml

sftp -i "${SSH_KEY}" -v "${USER}@${HOST}" <<EOF
cd /opt/autocoin/binance-bot
put ../target/autocoin-binance-bot-*.jar
put start.sh
put stop.sh
put restart.sh
quit
EOF

ssh -i "${SSH_KEY}" "${USER}@${HOST}" <<EOF
  cd /opt/autocoin/binance-bot
  chmod u+x *.sh
  chmod o-rwx *.sh
  ./restart.sh
EOF
