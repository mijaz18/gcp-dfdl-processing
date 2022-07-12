variable "project_name" {
  type        = string
  description = "The project name"
}

variable "project_id" {
  type        = string
  description = "The project ID to manage the Pub/Sub resources"
}

variable "region" {
  type        = string
  description = "The project region"
}

variable "zone" {
  type        = string
  description = "The project zone"
}

variable "location" {
  type        = string
  description = "The firestore and app engine application location"
}

variable "topic_message_controller" {
  type        = string
  description = "The name for the Pub/Sub topic with data in binary"
}

variable "topic_tpf_data_json" {
  type        = string
  description = "The name for the Pub/Sub topic with data in json"
}

variable "topic_labels" {
  type        = map(string)
  description = "A map of labels to assign to the Pub/Sub topic"
  default     = {}
}