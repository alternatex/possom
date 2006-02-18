/* Copyright (2005-2006) Schibsted Søk AS
 * AbstractLeafClause.java
 *
 * Created on 7 January 2006, 16:06
 */

package no.schibstedsok.front.searchportal.query.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import no.schibstedsok.front.searchportal.query.LeafClause;
import no.schibstedsok.front.searchportal.query.token.TokenEvaluatorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Basic implementation of the LeafClause interface.
 * Implements ontop of AbstractClause.
 * <b>Objects of this class are immutable</b>
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 * @version $Id$
 */
public abstract class AbstractLeafClause extends AbstractClause implements LeafClause {

    private static final Log LOG = LogFactory.getLog(AbstractLeafClause.class);

    /**
     * Works off the assumption that LeafClause constructor's have the exact parameter list:
     *       final String term,
     *       final String field,
     *       final Set&lt;Predicate&gt; knownPredicates,
     *       final Set&lt;Predicate&gt; possiblePredicates
     *
     * Where this is true subclasses are free to use this helper method.
     *
     * @param clauseClass the exact subclass of AbstracLeafClause that we are about to create (or find already in use).
     * @param term the term the clause we are about to create (or find) will have.
     * @param field the field the clause we are about to create (or find) will have.
     * @param predicate2evaluatorFactory the factory handing out evaluators against TokenPredicates.
     * Also holds state information about the current term/clause we are finding predicates against.
     * @param predicates2check the complete list of predicates that could apply to the current clause we are finding predicates for.
     * @param weakCache the map containing the key to WeakReference (of the Clause) mappings.
     * @return Either a clause already in use that matches this term and field, or a newly created cluase for this term and field.
     */
    public static AbstractLeafClause createClause(
            final Class/*<? extends LeafClause>*/ clauseClass,
            final String term,
            final String field,
            final TokenEvaluatorFactory predicate2evaluatorFactory,
            final Collection/*<Predicate>*/ predicates2check,
            final Map/*<Long,WeakReference<AbstractClause>>*/ weakCache) {


        final String key = field + ":" + term; // important that the key argument is unique to this object.

        // check weak reference cache of immutable wordClauses here.
        // no need to synchronise, no big lost if duplicate identical objects are created and added over each other
        //  into the cache, compared to the performance lost of trying to synchronise this.
        AbstractLeafClause clause = (AbstractLeafClause) findClauseInUse(key, weakCache);

        if (clause == null) {
            // create predicate sets
            predicate2evaluatorFactory.setClausesKnownPredicates(new HashSet/*<Predicate>*/());
            predicate2evaluatorFactory.setClausesPossiblePredicates(new HashSet/*<Predicate>*/());
            // find the applicale predicates now
            findPredicates(predicate2evaluatorFactory, predicates2check);
            try {
                // find the constructor...
                final Constructor constructor = clauseClass.getDeclaredConstructor(new Class[]{
                    String.class, String.class, Set.class, Set.class
                });
                // use the constructor...
                clause = (AbstractLeafClause) constructor.newInstance(new Object[]{
                    term, 
                    field, 
                    predicate2evaluatorFactory.getClausesKnownPredicates(), 
                    predicate2evaluatorFactory.getClausesPossiblePredicates()
                });

            } catch (SecurityException ex) {
                LOG.error(ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR + clauseClass.getName(), ex);
            } catch (NoSuchMethodException ex) {
                LOG.error(ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR + clauseClass.getName(), ex);
            } catch (IllegalArgumentException ex) {
                LOG.error(ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR + clauseClass.getName(), ex);
            } catch (InstantiationException ex) {
                LOG.error(ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR + clauseClass.getName(), ex);
            } catch (InvocationTargetException ex) {
                LOG.error(ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR + clauseClass.getName(), ex);
            } catch (IllegalAccessException ex) {
                LOG.error(ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR + clauseClass.getName(), ex);
            }

            addClauseInUse(key, clause, weakCache);
        }

        return clause;
    }

    /** You must use <CODE>AbstractLeafClause(String, Set&lt;Predicate&gt;, Set&lt;Predicate&gt;)</CODE> instead.
     * This constructor will throw an IllegalArgumentException.
     **/
    protected AbstractLeafClause() {
        throw new IllegalArgumentException(ERR_MUST_ALWAYS_USE_ARGED_CONSTRUCTOR);
    }

    /**
     * Create clause with the given term, known and possible predicates.
     * @param term the term (query string) for this clause.
     * @param knownPredicates the set of known predicates for this clause.
     * @param possiblePredicates the set of possible predicates for this clause.
     */
    protected AbstractLeafClause(
            final String term,
            final String field,
            final Set/*<Predicate>*/ knownPredicates,
            final Set/*<Predicate>*/ possiblePredicates) {

        super(term, knownPredicates, possiblePredicates);
        this.field = field;
    }


    protected final String field;


    /**
     * Get the field.
     *
     * @return the field.
     */
    public String getField() {
        return field;
    }
    
    /** {@inheritDoc}
     */
    public String toString() {
        //return getClass().getSimpleName() + "[" + getTerm() + "," + getField() + "]"; // JDK1.5
        return getClass().getName() + "[" + getTerm() + "," + getField() + "]";
     }
}
