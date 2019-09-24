terraform {
  backend "s3" {
    bucket = "terraform-states"
    region = "us-west-1"
    skip_credentials_validation = true
    force_path_style = true
  }
}


# Configure the GitLab Provider
provider "gitlab" {
    token = "${var.gitlab_token}"
    base_url = "${var.gitlab_base_url}"
}

data "gitlab_project" "data" {
    id = "${var.gitlab_project_id}"
}

output "project_path" {
  value = "${data.gitlab_project.data.path}"
}

resource "gitlab_project_hook" "webhook_mrVerify" {
  project               = "${var.gitlab_project_id}"
  url                   = "${var.jenkins_base_url}/project/${data.gitlab_project.data.path}/mrVerify"
  token                 = "${var.jenkins_webhook_token}"
  merge_requests_events = true
  note_events           = true
  push_events           = false
}

resource "gitlab_project_hook" "webhook_postMRReport" {
  project               = "${var.gitlab_project_id}"
  url                   = "${var.jenkins_base_url}/project/${data.gitlab_project.data.path}/postMRReport"
  token                 = "${var.jenkins_webhook_token}"
  merge_requests_events = true
  push_events           = false
}

resource "gitlab_project_hook" "webhook_devDeploy" {
  project               = "${var.gitlab_project_id}"
  url                   = "${var.jenkins_base_url}/project/${data.gitlab_project.data.path}/devDeploy"
  token                 = "${var.jenkins_webhook_token}"
  push_events           = true
}

resource "gitlab_project_hook" "webhook_qaDeploy" {
  project               = "${var.gitlab_project_id}"
  url                   = "${var.jenkins_base_url}/project/${data.gitlab_project.data.path}/qaDeploy"
  token                 = "${var.jenkins_webhook_token}"
  push_events           = false
  tag_push_events       = true
}

resource "gitlab_project_hook" "webhook_prodDeploy" {
  project               = "${var.gitlab_project_id}"
  url                   = "${var.jenkins_base_url}/project/${data.gitlab_project.data.path}/prodDeploy"
  token                 = "${var.jenkins_webhook_token}"
  merge_requests_events = true
  push_events           = false
}