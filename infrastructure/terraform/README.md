Copyright 2022 Google. This software is provided as-is, without warranty or
representation for any use or purpose. Your use of it is subject to your
agreement with Google.

## Enable API

API Being Enable:

* pubsub
* container registry
* cloud build
* cloud run

## Firestore Provision

Reference: [Create a database with Terraform](https://firebase.google.com/docs/firestore/solutions/automate-database-create#create_a_database_with_terraform)

### Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| project\_id | The project ID to manage the google_app_engine_application resource | `string` | n/a | yes |
| location\_id |  The location of both your App Engine application and your Cloud Firestore database | `string` | n/a | yes |
| database\_type | The database_type can be CLOUD_FIRESTORE or CLOUD_DATASTORE_COMPATIBILITY | `string` | n/a | yes |

## Bigtable Provision

Reference: [google_bigtable_instance](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/bigtable_instance)

### Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| name | The name (also called Instance Id in the Cloud Console) of the Cloud Bigtable instance | `string` | n/a | yes |
| project | The ID of the project in which the resource belongs. If it is not provided, the provider project is used | `string` | n/a | no |
| cluster\_id | The ID of the Cloud Bigtable cluster | `string` | n/a | yes |
| num\_nodes | The number of nodes in your Cloud Bigtable cluster | `string` | n/a | no |
| storage\_type | The storage type to use. One of "SSD" or "HDD" | `string` | SSD | no |
| zone | The zone to create the Cloud Bigtable cluster in. If it not specified, the provider zone is used.| `string` | n/a | no |

## Pub/Sub Topic and Subscription Provision

Provision of pub/sub topics and subscriptions to run the poc.

### Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| project\_id | The project ID to manage the Pub/Sub resources | `string` | n/a | yes |
| topic\_labels | A map of labels to assign to the Pub/Sub topic | `map(string)` | `{}` | no |
| topic | The name for the Pub/Sub topic | `string` | n/a | yes |

## Cloud Run Service Account Provision

Provision of service account to be used to invoke cloud run service
Reference: [service-accounts](https://registry.terraform.io/modules/terraform-google-modules/service-accounts/google/latest)

### Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| name | The name (also called Service name in the Cloud Console) of the Cloud Run Service | `string` | n/a | yes |
| project\_id | The project ID where service account will be created | `string` | n/a | yes |
| prefix |  Prefix applied to service account names | `string` | n/a | yes |
| names | Names of the service accounts to create | `list` | n/a | yes |
| project\_roles | Common roles to apply to all service accounts, project=>role as element | `list` | n/a | yes |

## Cloud Run Provision

Provision Cloud Run Service to deploy poc on GKE
Reference: [google_cloud_run_service](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/cloud_run_service)

### Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| name | The name (also called Service name in the Cloud Console) of the Cloud Run Service | `string` | n/a | yes |
| project\_id | The project ID to manage the Cloud Run resources | `string` | n/a | yes |
| location |  The location - specifically region of Cloud Run instances | `string` | n/a | yes |
| image | Container image to be deployed on Cloud Run | `string` | n/a | yes |
| (port) name | Port type for ingress requests | `string` | n/a | yes |
| container_port | Port number for ingress requests | `string` | n/a | yes |

## Cloud Build Trigger Provision

Provision Cloud Build Trigger to build and push image to Container Registery

### Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| create_cmd_entrypoint | On create, the command entrypoint to use - in this case 'gcloud' | `string` | n/a | yes |
| create_cmd_body | On create, the command body to use - in this case build submit command | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| project\_id | The project ID |
| topic\_tpf_data_json | The name of the Pub/Sub topic created to publish json data|
| topic\_name\_message_controller | The name of the Pub/Sub topic created to publish binary data |
| bigtable\_instance | The instance name |
| firestore\_app | The name of google app engine app created for firestore |
| service\_url | The url used to access the application deployed on cloud run |
| service\_account\_email | Service Account email |

## Usage

1. Before you start run the following commands to set the credentials

```    
    $ gcloud auth login
    $ gcloud config set project <project_id>
    $ gcloud auth configure-docker
```

2. Populate `terraform.tfvars` with the [required variables](#inputs).
3. Run the following commands within this directory

```
    $ terraform init     # to get the plugins
    $ terraform plan     # to see the infrastructure plan
    $ terraform apply    # to apply the infrastructure build
    $ terraform destroy  # to destroy the built infrastructure
```

4. Run the FirestoreInitializer or BigtableInitializer

```
.
└── mfol
    └── src
        └── main
            └── java
                └── com.example.mfol
                    └── util
                        ├── BigtableInitializer # Populates firestore database with example DFDL
                        └── FirestoreInitializer # Populates bigtable database with example DFDL
```

5. Test the application

```
   curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" https://<cloud_run_service_url>/publish?message=<complete_event_message_stream>
   
   http://localhost:8081/publish?message=<complete_event_message_stream>
                          
```

The <complete_event_message_stream> can be found under examples folder

```
.
└── mfol
       └── examples
        ├── CompleteEventMessageStream_RB40GD.txt # Example event message stream sent by Sabre
        ├── RB40GD.tpfdf.dfdl.xsd # Example DFDL sent by Sabre
        └── tpfbase.lib.dfdl.xsd # Example tpfbase used by all DFDLs sent by Sabre
```