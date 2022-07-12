output "project_id" {
  value       = var.project_id
  description = "The project ID"
}

output "topic_name_message_controller" {
  value       = module.pubsub.topic
  description = "The name of the Pub/Sub topic created to be used by MessageController Service"
}

output "topic_tpf_data_json" {
  value       = google_pubsub_topic.topic_tpf_data_json.name
  description = "The name of the Pub/Sub topic created to publish the binary data being processed and reformatted in json"
}

output "bigtable_instance" {
  value       = google_bigtable_instance.dfdl-instance.name
  description = "The name of the bigtable instance"
}

output "firestore_app" {
value       = google_app_engine_application.firestore_app.name
description = "The name of the firestore app in app engine"
}

output "gcr_location" {
value = data.google_container_registry_image.open-cloud-dfdl.image_url
description = "The URL at which the image can be accessed"
}

output "service_url" {
value = google_cloud_run_service.open-cloud-dfdl-service.status[0].url
description = "The URL at which the deployed application can be accessed"
}

output "cloud_run_service_account_email" {
value = module.cloud_run_service_account.email
description = "The service account email used to invoke cloud run service"
}