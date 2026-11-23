package portal.approval

import data.portal.hierarchy
import rego.v1

# The portal calls:  POST /v1/data/portal/approval/decision
# Input examples are in the inputs/ folder.
#
# Common input fields:
#   action        : "request" | "approve" | "reject" | "delegate" | "view" | "add_access"
#   user          : who is acting
#   requester     : who raised the request (approve, reject, delegate)
#   step          : "L1" | "L2" (approve, reject, delegate)
#   backup_owner  : mandatory on "request"
#   delegate_to   : target of a delegation
#   delegates     : people a step was delegated to (set by the request service, approve and reject)
#   request       : {requester, backup_owner, approvers[]} (view)
#   catalog       : catalogue id (add_access)

default allow := false

# Any employee can browse and raise a request.
allow if {
	input.action == "request"
	input.user in hierarchy.employees
}

# Approvers of the current step can approve, reject or delegate.
# Nobody can act on their own request.
allow if {
	input.action in {"approve", "reject", "delegate"}
	input.user != input.requester
	input.user in hierarchy.step_approvers(input.requester, input.step)
}

# A person the step was delegated to can approve or reject that step.
# The request service fills input.delegates from the stored delegation.
allow if {
	input.action in {"approve", "reject"}
	input.user != input.requester
	input.user in input.delegates
}

# Global super admins can act on any request, except their own.
allow if {
	input.action in {"approve", "reject", "delegate"}
	input.user != input.requester
	input.user in data.platform.super_admins
}

# A request is visible to the requester, the backup owner, its approvers and super admins.
allow if {
	input.action == "view"
	can_view
}

can_view if input.user == input.request.requester

can_view if input.user == input.request.backup_owner

can_view if input.user in input.request.approvers

can_view if input.user in data.platform.super_admins

# Only the creator of a catalogue can add accesses to it. Super admins can too.
allow if {
	input.action == "add_access"
	input.user == data.catalogs[input.catalog].creator
}

allow if {
	input.action == "add_access"
	input.user in data.platform.super_admins
}

# A catalogue is visible to members of the teams listed in its visible_to array.
# The creator and super admins always see it. "All teams" in the array means everyone.
# To widen it, change visible_to in data.json. In the portal that change is an
# "OPA Configurations" request in the Product catalogue, approved by L1 and L2.
allow if {
	input.action == "view_catalog"
	some team in data.teams
	input.user in team.members
	team.name in data.catalogs[input.catalog].visible_to
}

allow if {
	input.action == "view_catalog"
	"All teams" in data.catalogs[input.catalog].visible_to
	input.user in hierarchy.employees
}

allow if {
	input.action == "view_catalog"
	input.user == data.catalogs[input.catalog].creator
}

allow if {
	input.action == "view_catalog"
	input.user in data.platform.super_admins
}

# ---- Rules that block an otherwise allowed action ----------------------------

deny contains "backup owner is mandatory" if {
	input.action == "request"
	object.get(input, "backup_owner", "") == ""
}

deny contains "backup owner must be someone other than the requester" if {
	input.action == "request"
	input.backup_owner == input.user
}

deny contains "backup owner must be an existing employee" if {
	input.action == "request"
	input.backup_owner != ""
	not input.backup_owner in hierarchy.employees
}

deny contains "delegate_to must be an existing employee" if {
	input.action == "delegate"
	not input.delegate_to in hierarchy.employees
}

deny contains "cannot delegate to the requester" if {
	input.action == "delegate"
	input.delegate_to == input.requester
}

# ---- Final answer ------------------------------------------------------------

default permitted := false

permitted if {
	allow
	count(deny) == 0
}

decision := {
	"allow": permitted,
	"reasons": sort([r | some r in deny]),
	"approvers": approvers_for_step,
}

# Who should be asked for the step, after escalation for inactive approvers.
approvers_for_step := hierarchy.step_approvers(input.requester, input.step) if {
	input.requester
	input.step
} else := []
