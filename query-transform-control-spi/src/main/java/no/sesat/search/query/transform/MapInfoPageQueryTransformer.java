/* Copyright (2006-2007) Schibsted Søk AS
 * This file is part of SESAT.
 * You can use, redistribute, and/or modify it, under the terms of the SESAT License.
 * You should have received a copy of the SESAT License along with this program.  
 * If not, see https://dev.sesat.no/confluence/display/SESAT/SESAT+License

 */
package no.sesat.search.query.transform;

import java.util.Map;

import no.sesat.search.datamodel.generic.StringDataObject;

/**
 * Transformes the query if the requestparameters contains a contentId.
 * 
 * @author Stian Hegglund
 * @version $Revision:$
 */
public class MapInfoPageQueryTransformer extends AbstractQueryTransformer {
     
    private final MapInfoPageQueryTransformerConfig config;
    
    /** Required constructor. 
     * @param config Query transformer config
     */
    public MapInfoPageQueryTransformer(final QueryTransformerConfig config){
        this.config = (MapInfoPageQueryTransformerConfig) config;
    }
    
    /**
     * If the request parameteters contains the contentid parameter, append recordid to the query.
     * 
     * @see no.sesat.search.query.transform.QueryTransformer
     */
    public String getTransformedQuery() {
        final String originalQuery = getContext().getTransformedQuery();
        Map<String,StringDataObject> requestParameters = getContext().getDataModel().getParameters().getValues();
       
        if(requestParameters != null && requestParameters.containsKey(config.getParameterName())){
            return config.getPrefix() + ":" + requestParameters.get(config.getParameterName()).getString();
        }
        
        return originalQuery; 
    }
    
    public String getFilter() {
        Map<String,StringDataObject> requestParameters = getContext().getDataModel().getParameters().getValues();
       
        if(requestParameters != null && requestParameters.containsKey(config.getParameterName()) &&
                requestParameters.containsKey(config.getFilterParameterName())){
            return "+" + config.getFilterPrefix() + ":'" + requestParameters.get(config.getFilterParameterName()).getString() + "'";
        }
        
        return "";
    }
}