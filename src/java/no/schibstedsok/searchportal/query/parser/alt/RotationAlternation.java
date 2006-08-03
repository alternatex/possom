/* Copyright (2005-2006) Schibsted Søk AS
 *
 * RotationAlternation.java
 *
 * Created on 4 March 2006, 11:51
 *
 */

package no.schibstedsok.searchportal.query.parser.alt;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import no.schibstedsok.common.ioc.BaseContext;
import no.schibstedsok.searchportal.query.AndClause;
import no.schibstedsok.searchportal.query.Clause;
import no.schibstedsok.searchportal.query.DefaultOperatorClause;
import no.schibstedsok.searchportal.query.LeafClause;
import no.schibstedsok.searchportal.query.OperationClause;
import no.schibstedsok.searchportal.query.OrClause;
import no.schibstedsok.searchportal.query.XorClause;
import no.schibstedsok.searchportal.query.parser.AbstractReflectionVisitor;
import no.schibstedsok.searchportal.query.parser.QueryParser;
import org.apache.log4j.Logger;

/**
 * @version $Id$
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 */
public final class RotationAlternation {

    public interface Context extends BaseContext, QueryParser.Context {  }

    // Constants -----------------------------------------------------
    private static final Logger LOG = Logger.getLogger(RotationAlternation.class);
    private static final String DEBUG_ROOT_NOT_OPERATION = "Root is not an OperationClause";
    private static final String DEBUG_FOUND_FORESTS = "Numer of forests found in query ";
    private static final String DEBUG_ORIGINAL_BRANCH_ADD = "Adding to original branch ";

    // Attributes ----------------------------------------------------

    private final Context context;
    private final ParentFinder parentFinder = new ParentFinder();

    /** mappings from the newly rotated clause to the same original clause **/
    private final Map<OperationClause,OperationClause> originalFromNew = new HashMap<OperationClause,OperationClause>();
    /** mappings from the original clause to the same newly rotated clause **/
    private final Map<OperationClause,OperationClause> newFromOriginal = new HashMap<OperationClause,OperationClause>();
    /** mappings from the newly rotated clause to the same unrotated clause **/
    private final Map<OperationClause,OperationClause> beforeRotationFromNew = new HashMap<OperationClause,OperationClause>();
    /** mappings from the original to the unrotated clause */
    private final Map<OperationClause,OperationClause> beforeRotationFromOriginal = new HashMap<OperationClause,OperationClause>();

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    /**
     * Creates a new instance of RotationAlternation
     */
    public RotationAlternation(final Context cxt) {
        context = cxt;
    }

    // Public --------------------------------------------------------

    public Clause createRotations(final Clause originalRoot) {
        // find forests (subtrees) of AndClauses and OrClauses.
        if(originalRoot instanceof OperationClause){
            OperationClause root = (OperationClause) originalRoot;
            final Set<OperationClause> forestRoots = new ForestFinder().findForestRoots(root);
            LOG.debug(DEBUG_FOUND_FORESTS + forestRoots.size());
            for(OperationClause clause : forestRoots){

                LinkedList<? extends OperationClause> rotations = null;
                if(clause instanceof AndClause){
                    rotations = createForestRotation(AndClause.class, (AndClause) clause);
                }else if(clause instanceof OrClause){
                    rotations = createForestRotation(OrClause.class, (OrClause) clause);
                }else if(clause instanceof DefaultOperatorClause){
                    rotations = createForestRotation(DefaultOperatorClause.class, (DefaultOperatorClause) clause);
                }
                
                final XorClause result = createXorClause(clause, rotations);
                root = replaceDescendant(clause.getClass(), root, result, clause);
            }
            return root;
        }else{
            LOG.debug(DEBUG_ROOT_NOT_OPERATION);
            return originalRoot;
        }

    }

    // Z implementation ----------------------------------------------

    // Y overrides ---------------------------------------------------

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    private <T extends OperationClause> LinkedList<T> createForestRotation(final Class<T> rotateFor, final T oForestRoot) {

        LOG.debug("createForestRotation(" + rotateFor.getSimpleName() + ", " + oForestRoot + ")");

        // store this right-leaning branch for later comparason.
        final LinkedList<T> origBranch = new LinkedList<T>();
        for (T oC = oForestRoot; oC != null; oC = rightOpChild(rotateFor, oC)) {

            // add to branch
            LOG.debug(DEBUG_ORIGINAL_BRANCH_ADD + oC);
            origBranch.add(oC);
            // add to the state-memory maps (creating initial map state, simple self pointing mappings)
            originalFromNew.put(oC,oC);
            beforeRotationFromNew.put(oC,oC);
            beforeRotationFromOriginal.put(oC,oC);
        }

        // the size of the original right-leaning branch is also the number of alternations
        final LinkedList<T> alternations = new LinkedList<T>();
        // and the first nAlternation is the original branch
        alternations.addFirst(oForestRoot);

        // oIterate backwards from the right-most child (of type rotateFor) on the branch
        for (T oIterate = origBranch.removeLast(); origBranch.size() > 1; oIterate = origBranch.removeLast()) {

            // clear mappings
            beforeRotationFromOriginal.clear();
            for (Entry<OperationClause,OperationClause> entry : beforeRotationFromNew.entrySet()) {

                // reverse key to values in each entry
                beforeRotationFromOriginal.put(originalFromNew.get(entry.getValue()), entry.getKey());
            }
            originalFromNew.clear();
            newFromOriginal.clear();
            beforeRotationFromNew.clear();

            // find the right-most parent of iteration clause
            final T rLastestForestRoot = (T) beforeRotationFromOriginal.get(oForestRoot);
            T rBottom = (T) beforeRotationFromOriginal.get(oIterate);
            while(rBottom == leftChild(parent(rLastestForestRoot, rBottom))){
                rBottom = parent(rLastestForestRoot, rBottom);
            }

            // from 'rBottom' move upwards to the left,
            //  continue repeating if next parent does not have parent to the right
            T rTop = rBottom;
            while(parent(rLastestForestRoot, rTop) == leftChild(parent(rLastestForestRoot, parent(rLastestForestRoot, rTop)))){
                rTop = parent(rLastestForestRoot, rTop);
            }

            // we can rotate these now
            final T nAlternation = rotate(rotateFor, oForestRoot, oIterate, rTop, rBottom);
            alternations.addLast(nAlternation);
        }

        return alternations;
    }


    private <T extends OperationClause> T rotate(
            final Class<T> rotateFor,
            final T oForestRoot, // from original
            final T oIterate,  // from original
            final T rTop,  // from last rotation
            final T rBottom) { // from last rotation

        LOG.debug("rotate(" + oForestRoot + ", " + oIterate + ", " + rTop + ", " + rBottom + ")");

        // RIGHT-LEANING-LOWER BRANCH ROTATION
        LOG.debug("--- RIGHT-LEANING-LOWER BRANCH ROTATION ---");
        // re-construct the branch starting at the oIterate
        // the orpan must be from the newly rotated branch. (not the original or the last rotated branch).
        // the first nOrphan is the exception because it is always a leaf that's free to re-use.
        Clause nOrphan = leftChild(beforeRotationFromOriginal.get(oIterate));

        T oC = parent(oForestRoot, oIterate);
        do{
            oC = parent(oForestRoot, oC);
            LOG.debug(" orpan--> " + nOrphan);
            LOG.debug(" c--> " + oC);
            // oC is actually from the original branch.
            //  But it doesn't matter because the left child is a leaf that's free to re-use.
            nOrphan = createOperatorClause(rotateFor, leftChild(oC), nOrphan, oC);
            LOG.debug("  result--> " + nOrphan);

        }while(beforeRotationFromOriginal.get(oC) != rTop); // we have just rotated the rTop clause. getthefuckout.


        // LEFT-LEANING-UPPER-BRANCH ROTATION
        //  rotate what's now between the nOrphan and rBottom
        LOG.debug("--- LEFT-LEANING-UPPER-BRANCH ROTATION ---");
        oC = rightOpChild(rotateFor, (T)originalFromNew.get(nOrphan));
        // first find the first right child that's not yet been orphaned.
        while (newFromOriginal.get(oC) == null /*orphanage.contains( newFromOriginal.get(oC))*/) {
            oC = rightOpChild(rotateFor, oC);
        }

        // re-construct the left-leaning tail branch
        do{
            oC = rightOpChild(rotateFor, oC);
            LOG.debug(" orphan--> " + nOrphan);
            LOG.debug(" c--> " + oC);
            // oC is actually from the original branch.
            final T rC = (T) beforeRotationFromOriginal.get(oC);
            nOrphan = createOperatorClause(rotateFor, nOrphan, rightChild(rC), rC);
            LOG.debug("  result--> " + nOrphan);

        }while(beforeRotationFromOriginal.get(oC) != rBottom); // we have just rotated the rBottom. getthefuckout.


        // ORIGINAL TREE ROOT ROTATION
        LOG.debug("--- ORIGINAL TREE ROOT ROTATION ---");
        // keep rotating above the centre of rotation
            // loop rebuilding the tree, only replacing old instances with new instances.
        final T rForestRoot = (T) beforeRotationFromOriginal.get(oForestRoot);
        nOrphan = replaceDescendant(rotateFor, rForestRoot, (OperationClause) nOrphan, rTop);

        return (T) nOrphan;
    }

    /** will return null instead of a leafClause **/
    private <T extends OperationClause> T leftOpChild(final Class<T> opCls, final T clause){
        final Clause c = leftChild(clause);
        return opCls.isAssignableFrom(c.getClass()) ? (T) c : null;
    }

    /** return the left child, left or operation. **/
    private Clause leftChild(final OperationClause clause) {
        final Clause c = clause.getFirstClause();
        return c;
    }

    /** will return null instead of a leafClause **/
    private <T extends OperationClause> T rightOpChild(final Class<T> opCls, final T clause){
        final Clause c = rightChild(clause);
        return opCls.isAssignableFrom(c.getClass()) ? (T) c : null;
    }

    /** will return right child, left or operation. **/
    private Clause rightChild(final OperationClause clause) {
        Clause c = null;
        if (clause instanceof AndClause) {
            c = ((AndClause) clause).getSecondClause();
        }  else if (clause instanceof OrClause) {
            c = ((OrClause) clause).getSecondClause();
        }  else if (clause instanceof DefaultOperatorClause) {
            c = ((DefaultOperatorClause) clause).getSecondClause();
        }
        LOG.debug("rightChild -->" + c);
        return c;
    }

    /** return the parent operation clause of the given child.
     * And the child must be a descendant of the root. **/
    private <T extends OperationClause> T parent(final T root, final Clause child) {
        return parentFinder.getParent(root, child);
    }

    private OperationClause replaceDescendant(
            final Class<?extends OperationClause> opCls,
            final OperationClause root,
            final OperationClause newChild,
            final OperationClause replacementFor){

        OperationClause nC = newChild;
        OperationClause rR = replacementFor;
        while(root != rR){
            rR = parent(root, rR);
            nC = replaceOperatorClause((Class<OperationClause>)opCls, nC, rR);
        }
        return nC;
    }

    private <T extends OperationClause> T replaceOperatorClause(
            final Class<T> opCls,
            final Clause newChild,
            final T replacementFor) {

        return createOperatorClause(opCls,
                    leftChild(replacementFor) instanceof LeafClause ? leftChild(replacementFor) : newChild,
                    rightChild(replacementFor) instanceof LeafClause ? rightChild(replacementFor) : newChild,
                    replacementFor);
    }

    /** Create a new operator clause, of type opCls, with the left and right children.
     * We must also specify for whom it is to be a replacement for.
     * The replacementFor must be from the original branch.
     **/
    private <T extends OperationClause> T createOperatorClause(
            final Class<T> opCls,
            final Clause left,
            final Clause right,
            final T replacementFor) {

        LOG.debug("createOperatorClause(" + ", " + left + ", " + right + ", " + replacementFor + ")");
        T clause = null;
        if (clause instanceof AndClause) {
            clause = (T) context.createAndClause(left, right);
        }  else if (clause instanceof OrClause) {
            clause = (T) context.createOrClause(left, right);
        }  else if (clause instanceof DefaultOperatorClause) {
            clause = (T) context.createDefaultOperatorClause(left, right);
        }
        // update our mappings between rotations
        originalFromNew.put(clause, replacementFor);
        newFromOriginal.put(replacementFor, clause);
        beforeRotationFromNew.put(clause, beforeRotationFromOriginal.get(replacementFor));

        return clause;
    }

    private XorClause createXorClause(final OperationClause oRoot, final LinkedList<? extends OperationClause> rotations){


        return context.createXorClause(
                rotations.removeFirst(),
                rotations.isEmpty() ? oRoot : createXorClause(oRoot, rotations),
                XorClause.ROTATION_ALTERNATION);
    }

    // Inner classes -------------------------------------------------

    private static final class ParentFinder extends AbstractReflectionVisitor {
        private boolean searching = false;
        private OperationClause parent;
        private Clause child;

        private static final String ERR_CANNOT_CALL_VISIT_DIRECTLY
                = "visit(object) can't be called directly on this visitor!";
        private static final String ERR_CHILD_NOT_IN_HEIRARCHY
                = "The child is not part of this clause family!";

        public synchronized <T extends OperationClause> T getParent(final T root, final Clause child) {
            this.child = child;
            if (searching || child == null) {
                throw new IllegalStateException(ERR_CANNOT_CALL_VISIT_DIRECTLY);
            }
            searching = true;
            visit(root);
            searching = false;
            this.child = null;
            if (parent == null) {
                throw new IllegalArgumentException(ERR_CHILD_NOT_IN_HEIRARCHY);
            }
            return (T)parent;
        }


        protected void visitImpl(final OperationClause clause) {
            if (parent == null) {
                if (clause.getFirstClause() == child) {
                    parent = clause;
                }  else  {
                    clause.getFirstClause().accept(this);
                }
            }
        }

        protected void visitImpl(final OrClause clause) {
            if (parent == null) {
                if (clause.getFirstClause() == child || clause.getSecondClause() == child) {
                    parent = clause;
                }  else  {
                    clause.getFirstClause().accept(this);
                    clause.getSecondClause().accept(this);
                }
            }
        }

        protected void visitImpl(final AndClause clause) {
            if (parent == null) {
                if (clause.getFirstClause() == child || clause.getSecondClause() == child) {
                    parent = clause;
                }  else  {
                    clause.getFirstClause().accept(this);
                    clause.getSecondClause().accept(this);
                }
            }
        }

        protected void visitImpl(final DefaultOperatorClause clause) {
            if (parent == null) {
                if (clause.getFirstClause() == child || clause.getSecondClause() == child) {
                    parent = clause;
                }  else  {
                    clause.getFirstClause().accept(this);
                    clause.getSecondClause().accept(this);
                }
            }
        }

        protected void visitImpl(final LeafClause clause) {
            // leaves can't be parents :-)
        }

    }

    private static final class ForestFinder extends AbstractReflectionVisitor {
        
        
        private static final Logger LOG = Logger.getLogger(ForestFinder.class);
        private static final String DEBUG_COUNT_TO = "Trees in forest ";
        private boolean searching = false;
        private final Set<OperationClause> roots = new HashSet<OperationClause>();

        private static final String ERR_CANNOT_CALL_VISIT_DIRECTLY
                = "visit(object) can't be called directly on this visitor!";

        public synchronized Set<OperationClause> findForestRoots(final OperationClause root) {

            if (searching) {
                throw new IllegalStateException(ERR_CANNOT_CALL_VISIT_DIRECTLY);
            }
            searching = true;
            roots.clear();
            visit(root);
            searching = false;
            return Collections.unmodifiableSet(roots);
        }


        protected void visitImpl(final OperationClause clause) {

            clause.getFirstClause().accept(this);
        }

        protected void visitImpl(final XorClause clause) {

            clause.getFirstClause().accept(this);
            clause.getSecondClause().accept(this);
        }

        protected void visitImpl(final OrClause clause) {

            int count = 0;
            for( OrClause or = clause;
                    or.getSecondClause() instanceof OrClause;
                    or = (OrClause) or.getSecondClause()){

                ++count;
                LOG.debug(DEBUG_COUNT_TO + count);
            }
            if(count >=3){
                roots.add(clause);
            }
            clause.getFirstClause().accept(this);
            clause.getSecondClause().accept(this);
        }

        protected void visitImpl(final AndClause clause) {
            int count = 0;
            for( AndClause or = clause;
                    or.getSecondClause() instanceof AndClause;
                    or = (AndClause) or.getSecondClause()){

                ++count;
                LOG.debug(DEBUG_COUNT_TO + count);
            }
            if(count >=3){
                roots.add(clause);
            }
            clause.getFirstClause().accept(this);
            clause.getSecondClause().accept(this);
        }

        protected void visitImpl(final DefaultOperatorClause clause) {
            int count = 0;
            for( DefaultOperatorClause or = clause;
                    or.getSecondClause() instanceof DefaultOperatorClause;
                    or = (DefaultOperatorClause) or.getSecondClause()){

                ++count;
                LOG.debug(DEBUG_COUNT_TO + count);
            }
            if(count >=3){
                roots.add(clause);
            }
            clause.getFirstClause().accept(this);
            clause.getSecondClause().accept(this);
        }

        protected void visitImpl(final LeafClause clause) {
            // leaves can't be forest roots :-)
        }

    }

}