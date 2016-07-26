//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           V o i c e s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.score.LogicalPart;
import omr.score.Page;
import omr.score.Score;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SlurInter;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Class {@code Voices} connects voices and harmonizes their IDs (and thus colors)
 * within a stack, a system, a page or a score.
 *
 * @author Hervé Bitteur
 */
public abstract class Voices
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Voices.class);

    /** To sort voices by their ID. */
    public static final Comparator<Voice> byId = new Comparator<Voice>()
    {
        @Override
        public int compare (Voice v1,
                            Voice v2)
        {
            return Integer.compare(v1.getId(), v2.getId());
        }
    };

    /** To sort voices by vertical position within their containing measure or stack. */
    public static final Comparator<Voice> byOrdinate = new Comparator<Voice>()
    {
        @Override
        public int compare (Voice v1,
                            Voice v2)
        {
            if (v1.getMeasure().getStack() != v2.getMeasure().getStack()) {
                throw new IllegalArgumentException("Comparing voices in different stacks");
            }

            // Check if they are located in different parts
            Part p1 = v1.getMeasure().getPart();
            Part p2 = v2.getMeasure().getPart();

            if (p1 != p2) {
                return Part.byId.compare(p1, p2);
            }

            // Look for the first time slot with incoming chords for both voices.
            // If such slot exists, compare the two chords ordinates in that slot.
            Slot firstSlot1 = null;
            Slot firstSlot2 = null;

            for (Slot slot : v1.getMeasure().getStack().getSlots()) {
                Voice.SlotVoice vc1 = v1.getSlotInfo(slot);

                if ((vc1 == null) || (vc1.status != Voice.Status.BEGIN)) {
                    continue;
                }

                if (firstSlot1 == null) {
                    firstSlot1 = slot;
                }

                AbstractChordInter c1 = vc1.chord;

                Voice.SlotVoice vc2 = v2.getSlotInfo(slot);

                if ((vc2 == null) || (vc2.status != Voice.Status.BEGIN)) {
                    continue;
                }

                if (firstSlot2 == null) {
                    firstSlot2 = slot;
                }

                AbstractChordInter c2 = vc2.chord;

                return Inter.byOrdinate.compare(c1, c2);
            }

            // No common slot found, use index of first slot for each voice
            if ((firstSlot1 != null) && (firstSlot2 != null)) {
                return Integer.compare(firstSlot1.getId(), firstSlot2.getId());
            }

            // Use ordinate (there is a whole rest)
            AbstractChordInter c1 = v1.getFirstChord();
            AbstractChordInter c2 = v2.getFirstChord();

            return Inter.byOrdinate.compare(c1, c2);
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // refinePage //
    //------------//
    /**
     * Connect voices within the same logical part across all systems of a page.
     *
     * @param page the page to process
     */
    public static void refinePage (Page page)
    {
        final SystemInfo firstSystem = page.getFirstSystem();
        final SlurAdapter systemSlurAdapter = new SlurAdapter()
        {
            @Override
            public SlurInter getInitialSlur (SlurInter slur)
            {
                return slur.getExtension(LEFT);
            }
        };

        for (LogicalPart logicalPart : page.getLogicalParts()) {
            for (SystemInfo system : page.getSystems()) {
                Part part = system.getPartById(logicalPart.getId());

                if (part != null) {
                    if (system != firstSystem) {
                        // Check tied voices from previous system
                        for (Voice voice : part.getFirstMeasure().getVoices()) {
                            Integer tiedId = getTiedId(voice, systemSlurAdapter);

                            if ((tiedId != null) && (voice.getId() != tiedId)) {
                                part.swapVoiceId(voice.getId(), tiedId);
                            }
                        }
                    }
                }
            }
        }
    }

    //-------------//
    // refineScore //
    //-------------//
    /**
     * Connect voices within the same logical part across all pages of a score.
     * <p>
     * Ties across sheets cannot easily be persisted, so we detect and use them on the fly.
     *
     * @param score the score to process
     */
    public static void refineScore (Score score)
    {
        SystemInfo prevSystem = null; // Last system of preceding page, if any

        for (int pageNumber = 1; pageNumber <= score.getPageCount(); pageNumber++) {
            Page page = score.getPage(pageNumber);

            if (prevSystem != null) {
                for (LogicalPart scorePart : score.getLogicalParts()) {
                    // Check tied voices from same logicalPart in previous page
                    final LogicalPart logicalPart = page.getLogicalPartById(scorePart.getId());

                    if (logicalPart == null) {
                        continue; // scorePart not found in this page
                    }

                    final Part part = page.getFirstSystem().getPartById(logicalPart.getId());

                    if (part == null) {
                        continue; // logicalPart not found in the first system
                    }

                    final Map<SlurInter, SlurInter> links = getLinks(logicalPart, page, prevSystem);
                    final SlurAdapter pageSlurAdapter = new SlurAdapter()
                    {
                        @Override
                        public SlurInter getInitialSlur (SlurInter slur)
                        {
                            return links.get(slur);
                        }
                    };

                    for (Voice voice : part.getFirstMeasure().getVoices()) {
                        Integer tiedId = getTiedId(voice, pageSlurAdapter);

                        if ((tiedId != null) && (voice.getId() != tiedId)) {
                            logicalPart.swapVoiceId(page, voice.getId(), tiedId);
                        }
                    }
                }
            }

            prevSystem = page.getLastSystem();

            // TODO: Here we could dispose of sheet/page...
            /// score.disposePage(page);
        }
    }

    //-------------//
    // refineStack //
    //-------------//
    /**
     * Refine voice IDs within a stack.
     * <p>
     * When this method is called, initial IDs have been assigned according to voice creation
     * (whole voices first, then slot voices, with each voice remaining in its part).
     * See {@link Slot#buildVoices(java.util.List)} and {@link Slot#assignVoices()} methods.
     * <p>
     * Here we simply rename the IDs from top to bottom (roughly), within each part.
     *
     * @param stack the stack to process
     */
    public static void refineStack (MeasureStack stack)
    {
        // Within each measure, sort voices vertically and rename them accordingly.
        for (Measure measure : stack.getMeasures()) {
            measure.sortVoices();
            measure.renameVoices();
        }
    }

    //--------------//
    // refineSystem //
    //--------------//
    /**
     * Connect voices within the same part across all measures of a system.
     * <p>
     * When this method is called, each stack has a sequence of voices, the goal is now to
     * connect them from one stack to the other.
     *
     * @param system the system to process
     */
    public static void refineSystem (SystemInfo system)
    {
        final MeasureStack firstStack = system.getFirstMeasureStack();
        final SlurAdapter measureSlurAdapter = new SlurAdapter()
        {
            @Override
            public SlurInter getInitialSlur (SlurInter slur)
            {
                return slur;
            }
        };

        for (Part part : system.getParts()) {
            for (MeasureStack stack : system.getMeasureStacks()) {
                if (stack != firstStack) {
                    // Check tied voices from same part in previous measure
                    final Measure measure = stack.getMeasureAt(part);
                    final List<Voice> measureVoices = measure.getVoices(); // Sorted vertically

                    for (Voice voice : measureVoices) {
                        Integer tiedId = getTiedId(voice, measureSlurAdapter);

                        if ((tiedId != null) && (voice.getId() != tiedId)) {
                            measure.swapVoiceId(voice, tiedId);
                        }
                    }
                }
            }
        }
    }

    //----------//
    // getLinks //
    //----------//
    /**
     * Within the same logical part, retrieve the connections between the (orphan) slurs
     * at beginning of this page and the (orphan) slurs at end of the previous page.
     *
     * @param logicalPart     the logicalPart to connect
     * @param page            the containing page
     * @param precedingSystem the last system of previous page, if any
     * @return the links (slur-> prevSlur), perhaps empty but not null
     */
    private static Map<SlurInter, SlurInter> getLinks (LogicalPart logicalPart,
                                                       Page page,
                                                       SystemInfo precedingSystem)
    {
        if (precedingSystem != null) {
            final SystemInfo firstSystem = page.getFirstSystem();
            final Part part = firstSystem.getPartById(logicalPart.getId());
            final Part precedingPart = precedingSystem.getPartById(logicalPart.getId());

            if ((part != null) && (precedingPart != null)) {
                return part.connectSlursWith(precedingPart); // Links: Slur -> prevSlur
            }
        }

        return Collections.emptyMap();
    }

    //-----------//
    // getTiedId //
    //-----------//
    /**
     * Check whether the provided voice is tied (via a tie slur) to a previous voice
     * and thus must use the same ID.
     *
     * @param voice       the voice to check
     * @param slurAdapter to provide the linked slur at previous location
     * @return the imposed ID if any, null otherwise
     */
    private static Integer getTiedId (Voice voice,
                                      SlurAdapter slurAdapter)
    {
        final AbstractChordInter firstChord = voice.getFirstChord();
        final SIGraph sig = firstChord.getSig();

        // Is there an incoming tie on a head of this chord?
        for (Inter note : firstChord.getNotes()) {
            if (note instanceof AbstractHeadInter) {
                for (Relation r : sig.getRelations(note, SlurHeadRelation.class)) {
                    SlurHeadRelation shRel = (SlurHeadRelation) r;

                    if (shRel.getSide() == RIGHT) {
                        SlurInter slur = (SlurInter) sig.getOppositeInter(note, r);

                        if (slur.isTie()) {
                            SlurInter prevSlur = slurAdapter.getInitialSlur(slur);

                            if (prevSlur != null) {
                                AbstractHeadInter left = prevSlur.getHead(LEFT);

                                if (left != null) {
                                    final Voice leftVoice = left.getVoice();
                                    logger.debug("{} ties {} over to {}", slur, voice, leftVoice);

                                    return leftVoice.getId();
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //-------------//
    // SlurAdapter //
    //-------------//
    private static interface SlurAdapter
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Report the slur connected to the left of the provided one.
         * This can be the extending slur when looking in previous system, or the slur itself when
         * looking in previous measure within the same system.
         *
         * @param slur the slur to follow
         * @return the extending slur (or the slur itself)
         */
        SlurInter getInitialSlur (SlurInter slur);
    }
}
