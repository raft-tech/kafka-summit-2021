#-----------------------------------------------------------------------------
# High level policy for controlling access to Kafka.
#
# * Deny operations by default.
#
# The kafka-authorizer-opa plugin will query OPA for decisions at
# /kafka/authz/allow. If the policy decision is _true_ the request is allowed.
# If the policy decision is _false_ the request is denied.
#-----------------------------------------------------------------------------
package kafka.authz

# Deny operations by default if no explicit allow
default allow = false

# Users that have authorization to read from Kafka topics
users = {
  "bobjones": [
    {"operations": ["Read", "Write", "Describe", "Create"], "resource": "pii"}
  ],
  "alicesmith": [
    {"operations": ["Read", "Describe"], "resource": "pii"}
  ]
}

# Check if authenticated user has access to run the given operation on a resource
# In this case, the resource would be the topic name and the operation would be,
# for example, "read", write", "describe", "create", or "delete".
# It first looks for the user in "users". If found, it will loop through 
# the array and checks if the resource (topic) matches the requested resource. 
# If it does, check if the requested operation is defined in the list of allowed operations.
# If it is, then return "true", else return "false"
allow {
  some permission
  grant := users[principal.name][permission]
  grant.resource == input.resource.name
  grant.operations[_] == input.operation.name
}

allow {
  inter_broker_communication
}

allow {
  input.resource.resourceType.name == "Group"
}

# Used by the Confluent Control Center
allow {
  controlcenter_user
}

######## Helper Functions ########
inter_broker_communication {
  principal.name == "admin"
}

# Parse the Kafka SASL user from OPA's response
principal = {"name": name} {
  name := input.session.sanitizedUser
}

# Used by the Confluent Control Center to manage internal Kafka topics
controlcenter_user {
  principal.name == "ANONYMOUS"
}
