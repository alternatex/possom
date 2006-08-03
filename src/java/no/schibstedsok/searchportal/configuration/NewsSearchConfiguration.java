// Copyright (2006) Schibsted Søk AS
/*
 * NewsSearchConfiguration.java
 *
 * Created on March 7, 2006, 5:42 PM
 *
 */

package no.schibstedsok.searchportal.configuration;

/**
 *
 * @author magnuse
 */
public class NewsSearchConfiguration extends FastSearchConfiguration {

    public NewsSearchConfiguration(){
        super(null);
    }

    public NewsSearchConfiguration(final SearchConfiguration asc){
        super(asc);
    }
}