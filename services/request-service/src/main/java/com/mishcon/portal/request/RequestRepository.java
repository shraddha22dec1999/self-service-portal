package com.mishcon.portal.request;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByRequesterOrderBySeqDesc(String requester);

    List<AccessRequest> findByStatusOrderBySeqDesc(AccessRequest.Status status);

    List<AccessRequest> findAllByOrderBySeqDesc();
}
