#!/usr/bin/env bash
set -euo pipefail

TEMPLATE="${1:-aws/cloudformation/ecs-fargate-safe.yml}"
STACK_NAME="${2:-chat-safe}"

echo "Template: ${TEMPLATE}"
echo "Stack:    ${STACK_NAME}"
echo
echo "SAFETY: This script refuses to deploy unless you export CONFIRM_DEPLOY=YES"
echo "Example:"
echo "  CONFIRM_DEPLOY=YES aws cloudformation deploy \\"
echo "    --stack-name ${STACK_NAME} \\"
echo "    --template-file ${TEMPLATE} \\"
echo "    --capabilities CAPABILITY_NAMED_IAM \\"
echo "    --parameter-overrides EnableDeploy=true BackendImage=... FrontendImage=... JwtSecret=..."
echo

if [[ "${CONFIRM_DEPLOY:-}" != "YES" ]]; then
  echo "Refusing to deploy (CONFIRM_DEPLOY is not YES)."
  exit 1
fi

echo "Deploying stack... (you explicitly confirmed)"
aws cloudformation deploy \
  --stack-name "${STACK_NAME}" \
  --template-file "${TEMPLATE}" \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides EnableDeploy=true
