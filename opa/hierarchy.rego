package portal.hierarchy

import rego.v1

# Team registration
#
# Every team registers itself in data.teams.<team_id> with these fields:
#   name    : display name
#   l1      : array of L1 approvers (the members' manager or managers)
#   l2      : array of L2 approvers (the L1's manager or managers)
#   members : array of people who raise requests under this team
#
# Super admins are global. They live in data.platform.super_admins and belong to
# no team. They are the last level of escalation for every team.

# ---- L1 and L2 for a person --------------------------------------------------

l1_set(user) := {a |
	some team in data.teams
	user in team.members
	some a in team.l1
	a != user
}

l2_set(user) := {a |
	some team in data.teams
	user in team.members
	some a in team.l2
	a != user
}

l1(user) := sort(l1_set(user))

l2(user) := sort(l2_set(user))

# ---- Inactive approvers and escalation ---------------------------------------

active(list) := [x |
	some x in list
	not x in data.platform.inactive
]

# Who may act on a step for this requester.
# L1 step: active L1s, else active L2s (one level up), else the global super admins.
# L2 step: active L2s, else the global super admins.
step_approvers(requester, "L1") := a if {
	a := active(l1(requester))
	count(a) > 0
} else := b if {
	b := active(l2(requester))
	count(b) > 0
} else := data.platform.super_admins

step_approvers(requester, "L2") := a if {
	a := active(l2(requester))
	count(a) > 0
} else := data.platform.super_admins

# ---- Everyone the portal knows about -----------------------------------------

members_all := {e |
	some team in data.teams
	some e in team.members
}

approvers_all := {e |
	some team in data.teams
	some e in array.concat(team.l1, team.l2)
}

employees := (members_all | approvers_all) | {a | some a in data.platform.super_admins}

# Declared arrays per person, for the portal UI and audits.
# Query: data.portal.hierarchy.chains
chains[user] := {"l1": l1(user), "l2": l2(user), "team": teams_of(user)} if {
	some user in members_all
}

teams_of(user) := sort([name |
	some _, team in data.teams
	user in team.members
	name := team.name
])

# ---- Registration checks (run in CI before a team change is published) ------

required_fields := ["name", "l1", "l2", "members"]

violations contains msg if {
	some id, team in data.teams
	some f in required_fields
	not team[f]
	msg := sprintf("team %s is missing field %s", [id, f])
}

violations contains msg if {
	some id, team in data.teams
	count(team.l1) == 0
	msg := sprintf("team %s needs at least one L1 approver", [id])
}

violations contains msg if {
	some id, team in data.teams
	count(team.members) == 0
	msg := sprintf("team %s needs at least one member", [id])
}

violations contains msg if {
	some id, team in data.teams
	some a in team.l1
	a in team.l2
	msg := sprintf("team %s: %s is in both L1 and L2", [id, a])
}

violations contains msg if {
	some id, team in data.teams
	some m in team.members
	m in team.l1
	msg := sprintf("team %s: %s cannot be their own L1", [id, m])
}

violations contains msg if {
	count(data.platform.super_admins) == 0
	msg := "no global super admin is defined"
}
