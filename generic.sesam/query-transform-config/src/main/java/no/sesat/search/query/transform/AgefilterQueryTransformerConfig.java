/* Copyright (2012) Schibsted ASA
 * This file is part of Possom.
 *
 *   Possom is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Possom is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with Possom.  If not, see <http://www.gnu.org/licenses/>.
 *
 * AgeFilterTransformer.java
 *
 */

package no.sesat.search.query.transform;

import org.w3c.dom.Element;

import no.sesat.search.query.transform.AbstractQueryTransformerConfig.Controller;

/**
 *
 * @version $Id$
 */
@Controller("AgefilterQueryTransformer")
public final class AgefilterQueryTransformerConfig extends AbstractQueryTransformerConfig {

    private String ageField; // In seconds
    private String ageSymbol;


    /**
     *
     * @param ageField
     */
    public void setAgeField(final String ageField) {
        this.ageField = ageField;
    }

    /**
     *
     * @return
     */
    public String getAgeField(){
        return ageField;
    }

    /**
     *
     * @param ageSymbol
     */
    public void setAgeSymbol(final String ageSymbol) {
        this.ageSymbol = ageSymbol;
    }

    /**
     *
     * @return
     */
    public String getAgeSymbol(){
        return ageSymbol;
    }

    @Override
    public AgefilterQueryTransformerConfig readQueryTransformer(final Element qt) {
        super.readQueryTransformer(qt);
        setAgeField(qt.getAttribute("age-field"));
        String optionalAttribute = qt.getAttribute("age-symbol");
        if (optionalAttribute != null && optionalAttribute.length() > 0) {
            setAgeSymbol(optionalAttribute);
        }
        return this;
    }
}
