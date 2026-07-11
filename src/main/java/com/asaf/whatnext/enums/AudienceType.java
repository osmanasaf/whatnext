package com.asaf.whatnext.enums;

/**
 * Bir etkinliğin hedef kitlesi. NULL/UNKNOWN olan eski kayıtlar sezgisel (heuristic)
 * ile fallback sınıflandırılır — bkz. {@link com.asaf.whatnext.utils.EventAudience}.
 */
public enum AudienceType {
    GENERAL,
    CHILDREN,
    UNKNOWN
}
