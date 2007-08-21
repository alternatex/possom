/*
 * Copyright (2005-2007) Schibsted Søk AS
 * This file is part of SESAT.
 * You can use, redistribute, and/or modify it, under the terms of the SESAT License.
 * You should have received a copy of the SESAT License along with this program.  
 * If not, see https://dev.sesat.no/confluence/display/SESAT/SESAT+License
 */
package no.sesat.search.query.parser;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import no.sesat.search.query.AndClause;
import no.sesat.search.query.Clause;
import no.sesat.search.query.LeafClause;
import no.sesat.search.query.token.EvaluationState;
import no.sesat.search.query.token.TokenEvaluationEngine;
import no.sesat.search.query.token.TokenPredicate;
import no.sesat.search.site.Site;

/**
 * The AndClauseImpl represents a joining clause between two terms in the query.
 * For example: "term1 AND term2".
 * <b>Objects of this class are immutable</b>
 *
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 * @version $Id$
 */
public final class AndClauseImpl extends AbstractOperationClause implements AndClause {

    private static final int WEAK_CACHE_INITIAL_CAPACITY = 2000;
    private static final float WEAK_CACHE_LOAD_FACTOR = 0.5f;
    private static final int WEAK_CACHE_CONCURRENCY_LEVEL = 16;
    
    /** Values are WeakReference object to AbstractClause.
     * Unsynchronized are there are no 'changing values', just existance or not of the AbstractClause in the system.
     */
    private static final Map<Site,Map<String,WeakReference<AndClauseImpl>>> WEAK_CACHE
            = new ConcurrentHashMap<Site,Map<String,WeakReference<AndClauseImpl>>>();

    /* A WordClause specific collection of TokenPredicates that *could* apply to this Clause type. */
    private static final Collection<TokenPredicate> PREDICATES_APPLICABLE;

    static {
        final Collection<TokenPredicate> predicates = new ArrayList();
        predicates.add(TokenPredicate.ALWAYSTRUE);
        // Add all FastTokenPredicates
        predicates.addAll(TokenPredicate.getFastTokenPredicates());
        PREDICATES_APPLICABLE = Collections.unmodifiableCollection(predicates);
    }

    private final Clause secondClause;

    /**
     * Creator method for AndClauseImpl objects. By avoiding the constructors,
     * and assuming all AndClauseImpl objects are immutable, we can keep track
     * (via a weak reference map) of instances already in use in this JVM and reuse
     * them.
     * The methods also allow a chunk of creation logic for the AndClauseImpl to be moved
     * out of the QueryParserImpl.jj file to here.
     *
     * @param first the left child clause of the operation clause we are about to create (or find).
     * @param second the right child clause of the operation clause we are about to create (or find).
     * @param engine the factory handing out evaluators against TokenPredicates.
     * Also holds state information about the current term/clause we are finding predicates against.
     * @return returns a AndAndClauseImplstance matching the term, left and right child clauses.
     * May be either newly created or reused.
     */
    public static AndClauseImpl createAndClause(
        final Clause first,
        final Clause second,
        final TokenEvaluationEngine engine) {

        // construct the proper "schibstedsøk" formatted term for this operation.
        //  XXX eventually it would be nice not to have to expose the internal string representation of this object.
        final String term =
                (first instanceof LeafClause && ((LeafClause) first).getField() != null
                    ?  ((LeafClause) first).getField() + ":"
                    : "")
                + first.getTerm()
                + " AND "
                + (second instanceof LeafClause && ((LeafClause) second).getField() != null
                    ?  ((LeafClause) second).getField() + ":"
                    : "")
                + second.getTerm();

        try{
            // create predicate sets
            engine.setState(new EvaluationState(term, new HashSet<TokenPredicate>(), new HashSet<TokenPredicate>()));

            final String unique = '(' + term + ')';

            // the weakCache to use.
            Map<String,WeakReference<AndClauseImpl>> weakCache = WEAK_CACHE.get(engine.getSite());
            if(weakCache == null){
                
                weakCache = new ConcurrentHashMap<String,WeakReference<AndClauseImpl>>(
                        WEAK_CACHE_INITIAL_CAPACITY,
                        WEAK_CACHE_LOAD_FACTOR,
                        WEAK_CACHE_CONCURRENCY_LEVEL);
                
                WEAK_CACHE.put(engine.getSite(),weakCache);
            }

            // use helper method from AbstractLeafClause
            return createClause(
                    AndClauseImpl.class,
                    unique,
                    first,
                    second,
                    engine,
                    PREDICATES_APPLICABLE, weakCache);

        }finally{
            engine.setState(null);
        }
    }

    /**
     * Create clause with the given term, field, known and possible predicates.
     * @param term the term (query string) for this clause.
     * @param first the left child clause
     * @param second the right child clause
     * @param knownPredicates the set of known predicates for this clause.
     * @param possiblePredicates the set of possible predicates for this clause.
     */
    protected AndClauseImpl(
            final String term,
            final Clause first,
            final Clause second,
            final Set<TokenPredicate> knownPredicates,
            final Set<TokenPredicate> possiblePredicates) {

        super(term, first, knownPredicates, possiblePredicates);
        this.secondClause = second;
    }

    /**
     * Get the secondClause.
     *
     * @return the secondClause.
     */
    public Clause getSecondClause() {
        return secondClause;
    }

}