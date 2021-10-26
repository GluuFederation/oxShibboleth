package org.gluu.idp.context;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.opensaml.messaging.context.BaseContext;

import net.shibboleth.idp.attribute.IdPAttribute;

/**
 * Scratch context to pass data between the servlet and Shibboleth IDP
 * Currently being used only by the post-processing script, but could 
 * be used in other places
 * @author Djeumen Rolain
 * @version 0.1, 07/05/2021
 */
public final class GluuScratchContext extends BaseContext{
    
    private List<IdPAttribute> idpAttributes;
    private JsonObjectBuilder  httpParamObjBuilder;

    public GluuScratchContext() {

        this.httpParamObjBuilder = Json.createObjectBuilder();
    }

    public List<IdPAttribute> getIdpAttributes() {

        return this.idpAttributes;
    }

    public void setIdpAttributes(List<IdPAttribute> idpAttributes) {

        this.idpAttributes = idpAttributes;
    }

    public void addExtraHttpParameter(String parameter, String value) {
        
        this.httpParamObjBuilder.add(parameter,value);
    }


    public String getExtraHttpParameters() {

        JsonObject obj = httpParamObjBuilder.build();
        if(obj.isEmpty()) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonWriter writer = Json.createWriter(stream);
        writer.writeObject(obj);
        writer.close();
        try {
            return stream.toString("UTF-8");
        }catch(UnsupportedEncodingException e) {
            return null;
        }
    }
}
