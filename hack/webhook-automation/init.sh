#!/usr/bin/env bash

# Get folder path of this script
ROOT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

read -p "S3 Endpoint: " AWS_S3_ENDPOINT
echo

read -p "Terraform state key name: " AWS_S3_KEY
echo

read -s -p "S3 Access Key: " AWS_ACCESS_KEY_ID
echo

read -s -p "S3 Secret Key: " AWS_SECRET_ACCESS_KEY
echo

( 
  export AWS_ACCESS_KEY_ID 
  export AWS_SECRET_ACCESS_KEY 
  terraform init \
    -backend-config="endpoint=${AWS_S3_ENDPOINT}" \
    -backend-config="key=${AWS_S3_KEY}" 
)