package com.geosun.rmpd.domain.model;

import com.geosun.rmpd.domain.enums.DeclarationStatus;
import com.geosun.rmpd.domain.enums.TransportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "declaration")
public class Declaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeclarationStatus status = DeclarationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", length = 30)
    private TransportType transportType;

    @Column(name = "cmr_number", length = 50)
    private String cmrNumber;

    @Column(name = "route_start_date")
    private LocalDate routeStartDate;

    @Column(name = "route_end_date")
    private LocalDate routeEndDate;

    @Column(name = "loading_country", length = 2)
    private String loadingCountry;

    @Column(name = "unloading_country", length = 2)
    private String unloadingCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permit_id")
    private Permit permit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_party_id")
    private Party senderParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_party_id")
    private Party receiverParty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "route_points_json", columnDefinition = "JSON")
    private String routePointsJson;

    @Column(name = "puesc_sys_ref", length = 100)
    private String puescSysRef;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;

    @Column(name = "xsd_version", nullable = false, length = 30)
    private String xsdVersion = "RMPD_v20.11.2024";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public DeclarationStatus getStatus() {
        return status;
    }

    public void setStatus(DeclarationStatus status) {
        this.status = status;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    public String getCmrNumber() {
        return cmrNumber;
    }

    public void setCmrNumber(String cmrNumber) {
        this.cmrNumber = cmrNumber;
    }

    public LocalDate getRouteStartDate() {
        return routeStartDate;
    }

    public void setRouteStartDate(LocalDate routeStartDate) {
        this.routeStartDate = routeStartDate;
    }

    public LocalDate getRouteEndDate() {
        return routeEndDate;
    }

    public void setRouteEndDate(LocalDate routeEndDate) {
        this.routeEndDate = routeEndDate;
    }

    public String getLoadingCountry() {
        return loadingCountry;
    }

    public void setLoadingCountry(String loadingCountry) {
        this.loadingCountry = loadingCountry;
    }

    public String getUnloadingCountry() {
        return unloadingCountry;
    }

    public void setUnloadingCountry(String unloadingCountry) {
        this.unloadingCountry = unloadingCountry;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Permit getPermit() {
        return permit;
    }

    public void setPermit(Permit permit) {
        this.permit = permit;
    }

    public Party getSenderParty() {
        return senderParty;
    }

    public void setSenderParty(Party senderParty) {
        this.senderParty = senderParty;
    }

    public Party getReceiverParty() {
        return receiverParty;
    }

    public void setReceiverParty(Party receiverParty) {
        this.receiverParty = receiverParty;
    }

    public String getRoutePointsJson() {
        return routePointsJson;
    }

    public void setRoutePointsJson(String routePointsJson) {
        this.routePointsJson = routePointsJson;
    }

    public String getPuescSysRef() {
        return puescSysRef;
    }

    public void setPuescSysRef(String puescSysRef) {
        this.puescSysRef = puescSysRef;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public String getXsdVersion() {
        return xsdVersion;
    }

    public void setXsdVersion(String xsdVersion) {
        this.xsdVersion = xsdVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
