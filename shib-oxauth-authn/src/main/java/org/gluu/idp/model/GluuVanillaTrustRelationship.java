package org.gluu.idp.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.persist.model.base.InumEntry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Vanilla Trust relationship model 
 * Just enough data in it to extract some useful information 
 * (e.g. logout redirect uri)
 */

@DataEntry()
@ObjectClass(value="gluuSAMLconfig")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GluuVanillaTrustRelationship {

    private static final long serialVersionUID = -1L;

    @DN
    private String dn;

    @AttributeName(ignoreDuringUpdate=true)
    private String inum;

    @AttributeName(name = "oxAuthPostLogoutRedirectURI")
	private String spLogoutURL;

    @AttributeName(name="spLogoutRedirectUrl")
    private String spLogoutRedirectUrl;

    public String getDn() {

        return this.dn;
    }

    public void setDn(final String dn) {

        this.dn = dn;
    }

    public String getInum() {

        return this.inum;
    }

    public void setInum(final String inum) {

        this.inum = inum;
    }

    public String getSpLogoutURL() {

        return this.spLogoutURL;
    }

    public void setSpLogoutURL(final String spLogoutURL) {

        this.spLogoutURL = spLogoutURL;
    }

    public String getSpLogoutRedirectUrl() {

        return this.spLogoutRedirectUrl;
    }

    public void setSpLogoutRedirectUrl(final String spLogoutRedirectUrl) {

        this.spLogoutRedirectUrl = spLogoutRedirectUrl;
    }

}
