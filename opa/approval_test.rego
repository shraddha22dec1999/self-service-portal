package portal.approval_test

import data.portal.approval
import data.portal.hierarchy
import rego.v1

# ---- L1 and L2 come from the registered team --------------------------------

test_l1_l2_for_app_engg_member if {
	hierarchy.l1("app_engg_member_1") == ["sol_engg_lead"]
	hierarchy.l2("app_engg_member_1") == ["manpreet"]
}

test_l1_l2_for_sol_engg_lead if {
	hierarchy.l1("sol_engg_lead") == ["manpreet"]
	hierarchy.l2("sol_engg_lead") == ["amit"]
}

# ---- Inactive approvers go one level up, then to the global super admins ----

test_inactive_l1_goes_to_l2 if {
	hierarchy.step_approvers("app_engg_member_1", "L1") == ["manpreet"] with data.platform.inactive as ["sol_engg_lead"]
}

test_inactive_l1_and_l2_go_to_super_admins if {
	hierarchy.step_approvers("app_engg_member_1", "L1") == ["super_admin_1", "super_admin_2"] with data.platform.inactive as ["sol_engg_lead", "manpreet"]
}

test_inactive_l2_goes_to_super_admins if {
	hierarchy.step_approvers("app_engg_member_1", "L2") == ["super_admin_1", "super_admin_2"] with data.platform.inactive as ["manpreet"]
}

# ---- A manager joins: data change only, no policy change --------------------

test_new_manager_joins_app_engg if {
	hierarchy.l1("app_engg_member_1") == ["new_mgr", "sol_engg_lead"] with data.teams.app_engg.l1 as ["sol_engg_lead", "new_mgr"]
}

# ---- A new team registers ----------------------------------------------------

test_new_team_registration if {
	new_team := {"name": "Data Eng", "l1": ["lead_a"], "l2": ["manpreet"], "members": ["dev_x"]}
	hierarchy.l1("dev_x") == ["lead_a"] with data.teams.data_eng as new_team
	hierarchy.l2("dev_x") == ["manpreet"] with data.teams.data_eng as new_team
}

test_registration_check_flags_missing_l1 if {
	bad_team := {"name": "Broken", "l1": [], "l2": ["manpreet"], "members": ["dev_y"]}
	"team broken needs at least one L1 approver" in hierarchy.violations with data.teams.broken as bad_team
}

test_registration_is_clean_for_current_data if {
	count(hierarchy.violations) == 0
}

# ---- Approvals ---------------------------------------------------------------

test_l1_can_approve if {
	approval.permitted with input as {"action": "approve", "user": "sol_engg_lead", "requester": "app_engg_member_1", "step": "L1"}
}

test_l2_cannot_approve_l1_step if {
	not approval.permitted with input as {"action": "approve", "user": "manpreet", "requester": "app_engg_member_1", "step": "L1"}
}

test_l2_can_approve_l2_step if {
	approval.permitted with input as {"action": "approve", "user": "manpreet", "requester": "app_engg_member_1", "step": "L2"}
}

test_no_self_approval if {
	not approval.permitted with input as {"action": "approve", "user": "app_engg_member_1", "requester": "app_engg_member_1", "step": "L1"}
}

test_global_super_admin_can_approve if {
	approval.permitted with input as {"action": "approve", "user": "super_admin_1", "requester": "app_engg_member_1", "step": "L1"}
}

# ---- Backup owner ------------------------------------------------------------

test_request_needs_backup_owner if {
	not approval.permitted with input as {"action": "request", "user": "app_engg_member_1", "backup_owner": ""}
}

test_backup_owner_cannot_be_requester if {
	not approval.permitted with input as {"action": "request", "user": "app_engg_member_1", "backup_owner": "app_engg_member_1"}
}

test_request_with_valid_backup_owner if {
	approval.permitted with input as {"action": "request", "user": "app_engg_member_1", "backup_owner": "app_engg_member_2"}
}

# ---- Delegation --------------------------------------------------------------

test_approver_can_delegate_to_any_employee if {
	approval.permitted with input as {"action": "delegate", "user": "sol_engg_lead", "requester": "app_engg_member_1", "step": "L1", "delegate_to": "app_engg_member_2"}
}

test_non_approver_cannot_delegate if {
	not approval.permitted with input as {"action": "delegate", "user": "app_engg_member_2", "requester": "app_engg_member_1", "step": "L1", "delegate_to": "manpreet"}
}

test_cannot_delegate_to_unknown_person if {
	not approval.permitted with input as {"action": "delegate", "user": "sol_engg_lead", "requester": "app_engg_member_1", "step": "L1", "delegate_to": "stranger"}
}

# ---- Visibility and catalogue rules -----------------------------------------

test_backup_owner_can_view if {
	approval.permitted with input as {"action": "view", "user": "app_engg_member_2", "request": {"requester": "app_engg_member_1", "backup_owner": "app_engg_member_2", "approvers": ["sol_engg_lead", "manpreet"]}}
}

test_unrelated_person_cannot_view if {
	not approval.permitted with input as {"action": "view", "user": "amit", "request": {"requester": "app_engg_member_1", "backup_owner": "app_engg_member_2", "approvers": ["sol_engg_lead", "manpreet"]}}
}

test_only_creator_adds_access if {
	approval.permitted with input as {"action": "add_access", "user": "sol_engg_lead", "catalog": "app_engg_catalogue"}
	not approval.permitted with input as {"action": "add_access", "user": "manpreet", "catalog": "app_engg_catalogue"}
}

# ---- Catalogue visibility per team ------------------------------------------

test_team_member_sees_own_catalogue if {
	approval.permitted with input as {"action": "view_catalog", "user": "app_engg_member_1", "catalog": "app_engg_catalogue"}
}

test_other_team_cannot_see_catalogue if {
	not approval.permitted with input as {"action": "view_catalog", "user": "manpreet", "catalog": "app_engg_catalogue"}
}

# Changing OPA data is what makes a catalogue visible to another team.
test_widening_visible_to_shows_catalogue if {
	approval.permitted with input as {"action": "view_catalog", "user": "manpreet", "catalog": "app_engg_catalogue"} with data.catalogs.app_engg_catalogue.visible_to as ["App Engg", "Manpreet"]
}

test_super_admin_sees_every_catalogue if {
	approval.permitted with input as {"action": "view_catalog", "user": "super_admin_1", "catalog": "app_engg_catalogue"}
}

# ---- Delegation: the delegate can decide, a stranger cannot -------------------

test_delegate_can_approve if {
	approval.permitted with input as {"action": "approve", "user": "someone_else", "requester": "app_engg_member_1", "step": "L1", "delegates": ["someone_else"]}
}

test_stranger_cannot_approve if {
	not approval.permitted with input as {"action": "approve", "user": "someone_else", "requester": "app_engg_member_1", "step": "L1", "delegates": []}
}

test_delegate_cannot_approve_own_request if {
	not approval.permitted with input as {"action": "approve", "user": "app_engg_member_1", "requester": "app_engg_member_1", "step": "L1", "delegates": ["app_engg_member_1"]}
}
