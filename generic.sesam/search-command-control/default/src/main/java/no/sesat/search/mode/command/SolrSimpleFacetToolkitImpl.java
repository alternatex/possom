/*
 * Copyright (2012) Schibsted ASA
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
 */
package no.sesat.search.mode.command;

import java.util.Map;
import no.sesat.search.datamodel.generic.StringDataObject;
import no.sesat.search.mode.config.FacetedCommandConfig;
import no.sesat.search.result.FacetedSearchResult;
import no.sesat.search.result.Modifier;
import no.sesat.search.result.Navigator;
import no.sesat.search.result.ResultItem;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * Solr's Simple Faceting toolkit.
 *
 * {@link http://wiki.apache.org/solr/SolrFacetingOverview}
 * {@link http://wiki.apache.org/solr/SimpleFacetParameters}
 */
public class SolrSimpleFacetToolkitImpl implements SolrSearchCommand.FacetToolkit {

    // Constructors --------------------------------------------------

    // Public --------------------------------------------------------

    @Override
    public void createFacets(final SearchCommand.Context context, final SolrQuery query) {

        final Map<String, Navigator> facets = getSearchConfiguration(context).getFacets();
        query.setFacet(0 < facets.size());

        // facet counters || selection
        for (final Navigator facet : facets.values()) {
            createFacet(context, facet, query);
        }
    }

    @Override
    public void collectFacets(
            final SearchCommand.Context context,
            final QueryResponse response,
            final FacetedSearchResult<? extends ResultItem> searchResult) {

        final Map<String, Navigator> facets = getSearchConfiguration(context).getFacets();
        for (final Navigator facet : facets.values()) {
            collectFacet(context, response, searchResult, facet);
        }
    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    private void createFacet(final SearchCommand.Context context, final Navigator facet, final SolrQuery query){

        // we want the facet count
        query.addFacetField(facet.getField());

        final StringDataObject facetValue = context.getDataModel().getParameters().getValue(facet.getId());

        if (null != facetValue) {

            // splitting here allows for multiple navigation selections within the one navigation level.
            final String[] arr = getSearchConfiguration(context).getFacetSeparator().isEmpty()
                    ? new String[]{facetValue.getString()}
                    : facetValue.getString().split(getSearchConfiguration(context).getFacetSeparator());

            for (String navSingleValue : arr) {

                final String value = facet.isBoundaryMatch()
                        ? "^\"" + navSingleValue + "\"$"
                        : "\"" + navSingleValue + "\"";

                query.addFilterQuery(facet.getField() + ':' + value);
            }
            // request children facets of any selected facet
            if(null != facet.getChildNavigator()){
                createFacet(context, facet.getChildNavigator(), query);
            }
        }
    }

    private void collectFacet(
            final SearchCommand.Context context,
            final QueryResponse response,
            final FacetedSearchResult<? extends ResultItem> searchResult,
            final Navigator facet){

        final FacetField field = response.getFacetField(facet.getField());
        // facet counters
        if(null != field && null != field.getValues()){
            for (FacetField.Count c : field.getValues()){
                final Modifier mod = new Modifier(c.getName(), (int)c.getCount(), facet);
                searchResult.addModifier(facet.getId(), mod);
            }
            // collect children facets
            if(null != facet.getChildNavigator()){
               collectFacet(context, response, searchResult, facet.getChildNavigator());
            }
        }
    }

    private FacetedCommandConfig getSearchConfiguration(final SearchCommand.Context context) {
        return (FacetedCommandConfig) context.getSearchConfiguration();
    }

    // Inner classes -------------------------------------------------

}
