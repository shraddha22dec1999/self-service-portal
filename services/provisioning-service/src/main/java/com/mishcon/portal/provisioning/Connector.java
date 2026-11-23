package com.mishcon.portal.provisioning;

/** One target system. Add a class that implements this to support a new system. */
public interface Connector {
    /** Short name stored on the job, for example "github". */
    String name();

    /** True when this connector handles the capability, for example "Grant repository access". */
    boolean supports(String capability);

    /** Gives the access. Throw an exception to mark the job as failed. */
    String grant(GrantRequest r);

    /** Takes the access away again. */
    String revoke(GrantRequest r);
}
