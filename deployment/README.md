Copyright 2022 Google. This software is provided as-is, without warranty or
representation for any use or purpose. Your use of it is subject to your
agreement with Google.

# Deployment

## Activate Container Registry API

```
   $gcloud services enable containerregistry.googleapis.com
```

## Build and push image to container registry

```
    $gcloud auth application-default login
    $gcloud auth login
    $gcloud config set project <project_id>
    $gcloud auth configure-docker
    $gcloud builds submit --config=./deployment/cloudbuild.yaml
```

## Deploy to Cloud Run

```
   $gcloud run services replace ./deployment/service.yaml
```

## Access Control

```
    $gcloud run services add-iam-policy-binding dfdl-example-service \
        --member="serviceAccount:<project_id>@appspot.gserviceaccount.com" \
        --role="roles/run.invoker"

```

1) [project_id]: The project id used in the default service account

## Test

```
   curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" https://<cloud_run_service_url>/publish?message=<binary_example_payload>
   
   http://localhost:8081/publish?message=<binary_example_payload>
                          
```

1) [cloud_run_service_url]:  This will be returned on the console or the google
   cloud UI once the service is deployed. Alternatively use the following
   command get this service URL
   ```
   $gcloud run services describe <service> --format='value(status.url)'
   ```
   Replace [service] with the name of the service. More
   info [here](https://cloud.google.com/run/docs/managing/services#details)
2) [binary_example_payload]: binary message as shown in the
   example which can be found in
   [here](../examples/binary_example_payload.txt)

## References

[Authentication methods - gcloud credential helper](https://cloud.google.com/container-registry/docs/advanced-authentication#gcloud-helper)
[Managing Access Control](https://cloud.google.com/run/docs/securing/managing-access#gcloud)