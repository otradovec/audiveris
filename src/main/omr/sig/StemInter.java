//----------------------------------------------------------------------------//
//                                                                            //
//                               S t e m I n t e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.util.HorizontalSide;

/**
 * Class {@code StemInter} represents instances of Stem
 * interpretations.
 *
 * @author Hervé Bitteur
 */
public class StemInter
        extends BasicInter
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StemInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assignment quality
     */
    public StemInter (Glyph glyph,
                      double grade)
    {
        super(glyph, Shape.STEM, grade);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return BasicInter.getMinGrade();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // lookupHead //
    //------------//
    /**
     * Lookup a head connected to this stem, with proper head side and
     * pitch values.
     * Beware side is defined WRT head, not WRT stem.
     *
     * @param side  desired head side
     * @param pitch desired pitch position
     * @return the head instance if found, null otherwise
     */
    public Inter lookupHead (HorizontalSide side,
                             int pitch)
    {
        for (Relation rel : sig.edgesOf(this)) {
            if (rel instanceof HeadStemRelation) {
                Inter head = sig.getEdgeSource(rel);

                // Check side
                HorizontalSide headSide = ((HeadStemRelation) rel).getHeadSide();

                if (headSide != side) {
                    continue;
                }

                // Check pitch
                int headPitch = (head instanceof BlackHeadInter)
                        ? ((BlackHeadInter) head).getPitch()
                        : ((VoidHeadInter) head).getPitch();

                if (headPitch != pitch) {
                    continue;
                }

                // Got it!
                return head;
            }
        }
        
        return null;
    }    
}