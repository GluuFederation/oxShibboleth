package org.gluu.idp.model;

import java.io.Serializable;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

/**
 * 
 */
@DataEntry(sortBy = "oxAuthClaimName")
@ObjectClass(value="gluuAttribute")
public class GluuAttribute implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @DN
    private String dn;
    
    @AttributeName(ignoreDuringUpdate=true)
    private String inum;

    @AttributeName(name="gluuAttributeName")
    private String name;

    @AttributeName(name = "oxAuthClaimName")
    private String oxAuthClaimName;


    public String getDn() {

        return this.dn;
    }

    public void setDn(String dn) {

        this.dn = dn;
    }
    
    public String getInum() {

        return this.inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public String getName() {

        return this.name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getOxAuthClaimName() {

        return this.oxAuthClaimName;
    }

    public void setOxAuthClaimName(String oxAuthClaimName) {

        this.oxAuthClaimName = oxAuthClaimName;
    }
}
