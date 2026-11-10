package com.bloodbridge.domain;

/** CLOSED means the request ended (cancelled, fulfilled or expired) before the donor answered. */
public enum InvitationStatus { PENDING, ACCEPTED, DECLINED, CLOSED }
