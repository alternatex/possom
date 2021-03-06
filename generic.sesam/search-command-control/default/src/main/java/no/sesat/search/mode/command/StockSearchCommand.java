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
 */
/*
 * StockSearchCommand.java
 *
 */

package no.sesat.search.mode.command;

import no.sesat.search.query.transform.SynonymQueryTransformer;
import no.sesat.search.result.BasicResultList;
import no.sesat.search.result.BasicResultItem;
import no.sesat.search.result.ResultItem;
import no.sesat.search.result.ResultList;
import org.apache.log4j.Logger;

/**
 *
 *
 * @version $Id$
 */
public final class StockSearchCommand extends AbstractSearchCommand {

    private static final Logger LOG = Logger.getLogger(StockSearchCommand.class);

    /**
     *
     * @param cxt
     */
    public StockSearchCommand(final Context cxt) {

        super(cxt);
    }

    public ResultList<ResultItem> execute() {

        final ResultList<ResultItem> result = new BasicResultList<ResultItem>();
        final String q = getTransformedQuery();
        LOG.info("transformed query is " + q);

        // TODO: Remove this dependency on the query transformer. Prevents the query transformer from being moved into
        // the skin.
        // for now we are only interested in complete matches. and the SynonymQT only deals with stock-tickers.
        if( SynonymQueryTransformer.isSynonym( q )){

            ResultItem item = new BasicResultItem();


            final String tickerCode = SynonymQueryTransformer.isTicker(q)
                    ? q
                    : SynonymQueryTransformer.getSynonym(q);
            final String tickerName = SynonymQueryTransformer.isTickersFullname(q)
                    ? q
                    : SynonymQueryTransformer.getSynonym(q);


            item = item.addField("tickerCode", tickerCode).addField("tickerName", tickerName);

            result.addResult(item);
            result.setHitCount(1);
        }
        return result;
    }

}
