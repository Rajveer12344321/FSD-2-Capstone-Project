package com.bloodbridge.service;

import com.bloodbridge.domain.BloodRequest;
import com.bloodbridge.domain.DonationRecord;
import com.bloodbridge.domain.InvitationStatus;
import com.bloodbridge.repo.BloodRequestRepository;
import com.bloodbridge.repo.DonationRecordRepository;
import com.bloodbridge.repo.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CSV exports. Personal contact details are never included; donors appear with shortened names only. */
@Service
public class ReportService {
    private final BloodRequestRepository requests;
    private final InvitationRepository invitations;
    private final DonationRecordRepository donations;

    public ReportService(BloodRequestRepository requests, InvitationRepository invitations,
                         DonationRecordRepository donations) {
        this.requests = requests;
        this.invitations = invitations;
        this.donations = donations;
    }

    @Transactional(readOnly = true)
    public String requestsCsv() {
        StringBuilder sb = new StringBuilder(
                "id,created_at,blood_group,component,units,hospital,city,urgency,status,required_by,invited,accepted\n");
        for (BloodRequest r : requests.findAllByOrderByCreatedAtDesc()) {
            sb.append(r.getId()).append(',')
              .append(r.getCreatedAt()).append(',')
              .append(r.getBloodGroup().label()).append(',')
              .append(r.getComponent()).append(',')
              .append(r.getUnitsRequired()).append(',')
              .append(csv(r.getHospital())).append(',')
              .append(csv(r.getCity())).append(',')
              .append(r.getUrgency()).append(',')
              .append(r.getStatus()).append(',')
              .append(r.getRequiredBy()).append(',')
              .append(invitations.countByRequest(r)).append(',')
              .append(invitations.countByRequestAndStatus(r, InvitationStatus.ACCEPTED)).append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String donationsCsv() {
        StringBuilder sb = new StringBuilder("id,donated_on,donor,blood_group,units,request_id,recorded_by\n");
        for (DonationRecord d : donations.findTop100ByOrderByCreatedAtDesc()) {
            sb.append(d.getId()).append(',')
              .append(d.getDonatedOn()).append(',')
              .append(csv(RequestService.shortName(d.getDonor().getName()))).append(',')
              .append(d.getBloodGroup().label()).append(',')
              .append(d.getUnits()).append(',')
              .append(d.getRequest() == null ? "" : d.getRequest().getId()).append(',')
              .append(csv(d.getRecordedBy())).append('\n');
        }
        return sb.toString();
    }

    /** Quotes a field and neutralises spreadsheet formula injection. */
    private String csv(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
