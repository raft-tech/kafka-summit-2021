#-----------------------------------------------------------------------------
# High level policy for controlling access to Kafka.
#
# * Deny operations by default.
# * Allow operations if no explicit denial.
#
# The kafka-authorizer-opa plugin will query OPA for decisions at
# /kafka/authz/allow. If the policy decision is _true_ the request is allowed.
# If the policy decision is _false_ the request is denied.
#-----------------------------------------------------------------------------
package kafka.authz

default allow = false

# List of users that have authorization to read from Kafka topics
clients = {
  "bobjones": [
    {"operations": ["Read", "Write", "Describe", "Create"], "resource": "pii"}
  ],
  "alicesmith": [
    {"operations": ["Read", "Describe"], "resource": "pii"}
  ]
}

allow {
  inter_broker_communication
}

# Used by the Confluent Control Center
allow {
  controlcenter_user
}

allow {
  some permission
  grant := clients[principal.name][permission]
  grant.resource == input.resource.name
  grant.operations[_] == input.operation.name
}

allow {
  input.resource.resourceType.name == "Group"
}

######## Helper Functions ########
inter_broker_communication {
  principal.name == "admin"
}

# Used by the Confluent Control Center to manage internal Kafka topics
controlcenter_user {
  principal.name == "ANONYMOUS"
}

# Parse the Kafka SASL user from OPA's response
principal = {"name": name} {
  name := input.session.sanitizedUser
}