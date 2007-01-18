/* Copyright (2005-2006) Schibsted Søk AS
 *
 * Scorer.java
 *
 * Created on 13 January 2006, 09:58
 *
 */

package no.schibstedsok.searchportal.query.analyser;

import java.io.IOException;
import no.schibstedsok.searchportal.query.parser.AbstractReflectionVisitor;
import no.schibstedsok.searchportal.query.token.TokenPredicate;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;

/** Responsible for Visiting the Query and scoring a total according
 *   to the rule's predicateScores listed in the context.
 * This class is not thread-safe.
 *
 * @version $Id$
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 */
public final class Scorer extends AbstractReflectionVisitor {

    /** The contextual dependencies the Scorer requires to calculate a total score for this Query it will visit. */
    public interface Context extends AnalysisRule.Context {

        /** TODO comment me. **/
        String getNameForAnonymousPredicate(Predicate predicate);
    }

    private static final Logger LOG = Logger.getLogger(Scorer.class);

    private int score = 0;
    private final Context context;

    private static final String DEBUG_UPDATE_SCORE = "Updating Score...";
    private static final String INFO_STALE_SCORE = "Scoring failed...";

    /** Create the Scorer with the required context.
     **/
    public Scorer(final Context cxt) {
        context = cxt;
    }

    /** the scoring result. should not be called before visiting is over.
     **/
    public int getScore() {
        return score;
    }

    /** TODO comment me. **/
    public void addScore(final PredicateScore predicateScore) {

        final Predicate predicate = predicateScore.getPredicate();

        LOG.debug(DEBUG_UPDATE_SCORE + toString(predicate) + " adds " + predicateScore.getScore());

        report("   <predicate-add name=\"" + toString(predicate) + "\">"
                + predicateScore.getScore()
                + "</predicate-add>\n");
        
        score += predicateScore.getScore();
    }

    /** TODO comment me. **/
    public void minusScore(final PredicateScore predicateScore) {

        final Predicate predicate = predicateScore.getPredicate();

        LOG.debug(DEBUG_UPDATE_SCORE + toString(predicate) + " subtracts " + predicateScore.getScore());

        report("   <predicate-minus name=\"" + toString(predicate) + "\">"
                + predicateScore.getScore()
                + "</predicate-minus>\n");

        score -= predicateScore.getScore();
    }

    /** TODO comment me. **/
    public void error(final PredicateScore predicateScore) {

        final Predicate predicate = predicateScore.getPredicate();

        LOG.info(INFO_STALE_SCORE + toString(predicate));

        report("   <predicate-error name=\"" + toString(predicate) + "\">±"
                + predicateScore.getScore()
                + "</predicate-error>\n");
    }
    
    private void report(final String msg){
        
        try{
            context.getReportBuffer().append(msg);
        
        }catch(IOException ioe){
            LOG.warn(ioe.getMessage());
        }
    }

    private String toString(final Predicate predicate){

        return predicate instanceof TokenPredicate
                    ? predicate.toString()
                    : context.getNameForAnonymousPredicate(predicate);
    }
}
