#!/usr/bin/env bash
set -euo pipefail
mkdir -p infra/ecs/out
for TPL in infra/ecs/*.tpl.json; do
  OUT="infra/ecs/out/$(basename "${TPL%.tpl.json}.json")"
  sed -e "s|\${LAB_ROLE_ARN}|${LAB_ROLE_ARN}|g" \
      -e "s|\${AWS_REGION}|${AWS_REGION}|g" \
      -e "s|\${IMAGE_URI}|${IMAGE_URI}|g" \
      -e "s|\${CLUSTER}|${CLUSTER}|g" \
      -e "s|\${NAMESPACE}|${NAMESPACE}|g" \
      -e "s|\${SUBNET1}|${SUBNET1}|g" \
      -e "s|\${SUBNET2}|${SUBNET2}|g" \
      -e "s|\${SG_ID}|${SG_ID}|g" \
      "$TPL" > "$OUT"
done
echo "Plantillas renderizadas en infra/ecs/out/"