provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable pubsub API
resource "google_project_service" "project_pubsub_api" {
  project = var.project_id
  service = "pubsub.googleapis.com"
  timeouts {
    create = "30m"
    update = "40m"
  }
  disable_dependent_services = true
  disable_on_destroy         = true
}

# Enable container registry API
resource "google_project_service" "project_containerregistry_api" {
  project = var.project_id
  service = "containerregistry.googleapis.com"
  timeouts {
    create = "30m"
    update = "40m"
  }
  disable_dependent_services = true
  disable_on_destroy         = true
}

# Enable cloud build API
resource "google_project_service" "project_cloudbuild_api" {
  project = var.project_id
  service = "cloudbuild.googleapis.com"
  timeouts {
    create = "30m"
    update = "40m"
  }
  disable_dependent_services = true
  disable_on_destroy         = true
}

# Enable cloud run API
resource "google_project_service" "project_cloud_run_api" {
  project = var.project_id
  service = "run.googleapis.com"
  timeouts {
    create = "30m"
    update = "40m"
  }
  disable_dependent_services = true
  disable_on_destroy         = true
}

# Creating service account for Cloud Run
module "cloud_run_service_account" {
  source        = "terraform-google-modules/service-accounts/google"
  version       = "~> 3.0"
  project_id    = var.project_id
  prefix        = "cloud-run"
  names         = ["service-account"]
  project_roles = [
    "${var.project_id}=>roles/run.invoker",
  ]
}

# Topic and subscribers to emulate the publication binary data in a topic and
# subscription to process the data to transform in json format using dfdl
# definitions.
module "pubsub" {
  source       = "terraform-google-modules/pubsub/google"
  project_id   = var.project_id
  topic        = var.topic_message_controller
  topic_labels = var.topic_labels
  depends_on   = [google_project_service.project_pubsub_api]

  pull_subscriptions = [
    {
      name                 = "${var.topic_message_controller}-sub"
      ack_deadline_seconds = 10
    },
    {
      name                 = "${var.topic_message_controller}-sub2"
      ack_deadline_seconds = 10
    },
  ]
}

# Topic to publish binary data being processed and reformatted in json
resource "google_pubsub_topic" "topic_tpf_data_json" {
  name       = var.topic_tpf_data_json
  project    = var.project_id
  depends_on = [google_project_service.project_pubsub_api]
}

# Subscription to pull messages in json format from a client.
# This PoC does not has any client. Message can be pulled from the console.
resource "google_pubsub_subscription" "topic_tpf_data_sub" {
  name                 = "${var.topic_tpf_data_json}-sub"
  topic                = google_pubsub_topic.topic_tpf_data_json.name
  ack_deadline_seconds = 10
  depends_on           = [google_pubsub_topic.topic_tpf_data_json]
}

# Firestore
resource "google_app_engine_application" "firestore_app" {
  project       = var.project_id
  location_id   = var.location
  database_type = "CLOUD_FIRESTORE"
}

# Bigtable
resource "google_bigtable_instance" "dfdl-instance" {
  name                = "dfdl-instance"
  project             = var.project_id
  deletion_protection = false

  cluster {
    cluster_id   = "sabre-dfdl-c1"
    num_nodes    = 1
    storage_type = "SSD"
    zone         = "${var.region}-${var.zone}"
  }
}

# Fetches the project name, and provides the appropriate URLs to use for
# container registry for this project
data "google_container_registry_image" "open-cloud-dfdl" {
  name = "open-cloud-dfdl"
  tag  = "v1"
}

# Cloud Build
module "gcloud_build_image" {
  source                = "terraform-google-modules/gcloud/google"
  module_depends_on     = [google_project_service.project_cloudbuild_api]
  version               = "~> 2.0"
  create_cmd_body       = "builds submit ../../ --config=../../deployment/cloudbuild.yaml"
  create_cmd_entrypoint = "gcloud"
}

# Cloud Run Service
resource "google_cloud_run_service" "open-cloud-dfdl-service" {
  name                       = "open-cloud-dfdl-service"
  location                   = var.region
  project                    = var.project_id
  autogenerate_revision_name = true
  depends_on                 = [
    google_project_service.project_cloud_run_api, module.gcloud_build_image,
    module.cloud_run_service_account
  ]

  metadata {
    annotations = {
      "run.googleapis.com/launch-stage" = "BETA"
    }
    namespace = var.project_id
  }
  template {
    metadata {
      annotations = {
        "run.googleapis.com/execution-environment" = "gen2"
      }
    }
    spec {
      containers {
        # gcr image location
        image = data.google_container_registry_image.open-cloud-dfdl.image_url
        ports {
          name           = "http1"
          container_port = 8081
        }
      }
    }
  }
  traffic {
    percent         = 100
    latest_revision = true
  }
}

# Create restricted access for cloud run
data "google_iam_policy" "auth" {
  binding {
    role    = "roles/run.invoker"
    members = [
      "serviceAccount:${module.cloud_run_service_account.email}",
    ]
  }
}

# Cloud Run IAM policy
resource "google_cloud_run_service_iam_policy" "auth" {
  location    = google_cloud_run_service.open-cloud-dfdl-service.location
  project     = google_cloud_run_service.open-cloud-dfdl-service.project
  service     = google_cloud_run_service.open-cloud-dfdl-service.name
  policy_data = data.google_iam_policy.auth.policy_data
}

# Return Cloud Run service URL
output "url" {
  value = google_cloud_run_service.open-cloud-dfdl-service.status[0].url
}