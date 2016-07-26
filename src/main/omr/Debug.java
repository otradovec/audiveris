//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         D e b u g                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.classifier.NeuralClassifier;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.ShapeDescription;

import omr.image.TemplateFactory;

import omr.sheet.Picture;
import omr.sheet.SheetStub;
import omr.sheet.ui.StubDependent;
import omr.sheet.ui.StubsController;

import org.jdesktop.application.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Convenient class meant to temporarily inject some debugging.
 * To be used in sync with file user-actions.xml in config folder
 *
 * @author Hervé Bitteur
 */
public class Debug
        extends StubDependent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Debug.class);

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // checkSources //
    //--------------//
    /**
     * Check which sources are still in Picture cache.
     *
     * @param e unused
     */
    @Action
    public void checkSources (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if ((stub != null) && stub.hasSheet()) {
            Picture picture = stub.getSheet().getPicture();

            if (picture != null) {
                picture.checkSources();
            }
        }
    }

    //    //------------------//
    //    // injectChordNames //
    //    //------------------//
    //    @Action(enabledProperty = SHEET_AVAILABLE)
    //    public void injectChordNames (ActionEvent e)
    //    {
    //        Score score = ScoreController.getCurrentScore();
    //
    //        if (score == null) {
    //            return;
    //        }
    //
    //        ScoreSystem system = score.getFirstPage()
    //                                  .getFirstSystem();
    //        system.acceptChildren(new ChordInjector());
    //    }
    //    //---------------//
    //    // ChordInjector //
    //    //---------------//
    //    private static class ChordInjector
    //        extends AbstractScoreVisitor
    //    {
    //        //~ Static fields/initializers -----------------------------------------
    //
    //        /** List of symbols to inject. */
    //        private static final String[] shelf = new String[] {
    //                                                  "BMaj7/D#", "BMaj7", "G#m9",
    //                                                  "F#", "C#7sus4", "F#"
    //                                              };
    //
    //        //~ Instance fields ----------------------------------------------------
    //
    //        /** Current index to symbol to inject. */
    //        private int symbolCount = 0;
    //
    //        //~ Methods ------------------------------------------------------------
    //
    //        @Override
    //        public boolean visit (ChordSymbol symbol)
    //        {
    //            // Replace chord info by one taken from the shelf
    //            if (symbolCount < shelf.length) {
    //                symbol.info = ChordInfo.create(shelf[symbolCount++]);
    //            }
    //
    //            return false;
    //        }
    //    }
    //----------------//
    // checkTemplates //
    //----------------//
    /**
     * Generate the templates for all relevant shapes for a range of interline values.
     *
     * @param e unused
     */
    @Action
    public void checkTemplates (ActionEvent e)
    {
        TemplateFactory factory = TemplateFactory.getInstance();

        for (int i = 10; i < 40; i++) {
            logger.info("Catalog for interline {}", i);
            factory.getCatalog(i);
        }

        logger.info("Done.");
    }

    //------------------//
    // saveTrainingData //
    //------------------//
    /**
     * Generate a file (format csv) to be used by deep learning software,
     * with the training data.
     *
     * @param e unused
     */
    @Action
    public void saveTrainingData (ActionEvent e)
            throws FileNotFoundException
    {
        Path path = WellKnowns.EVAL_FOLDER.resolve(
                "samples-" + ShapeDescription.getName() + ".csv");
        OutputStream os = new FileOutputStream(path.toFile());
        final PrintWriter out = getPrintWriter(os);

        SampleRepository repository = SampleRepository.getInstance();

        if (!repository.isLoaded()) {
            repository.loadRepository(true);
        }

        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        for (Sample sample : samples) {
            double[] ins = ShapeDescription.features(sample, sample.getInterline());

            for (double in : ins) {
                out.print((float) in);
                out.print(",");
            }

            ///out.println(sample.getShape().getPhysicalShape());
            out.println(sample.getShape().getPhysicalShape().ordinal());
        }

        out.flush();
        out.close();
        logger.info("Classifier data saved in " + path.toAbsolutePath());
    }

    //--------------//
    // trainAndSave //
    //--------------//
    /**
     *
     *
     * @param e unused
     */
    @Action
    public void trainAndSave (ActionEvent e)
            throws FileNotFoundException, IOException
    {
        Path modelPath = WellKnowns.EVAL_FOLDER.resolve(NeuralClassifier.MODEL_FILE_NAME);
        Files.deleteIfExists(modelPath);
        Path normsPath = WellKnowns.EVAL_FOLDER.resolve(NeuralClassifier.NORMS_FILE_NAME);
        Files.deleteIfExists(normsPath);

        SampleRepository repository = SampleRepository.getInstance();

        if (!repository.isLoaded()) {
            repository.loadRepository(true);
        }

        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        NeuralClassifier classifier = NeuralClassifier.getInstance();
        classifier.train(samples, null);
        classifier.store();
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (OutputStream os)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(os, WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            logger.warn("Error creating PrintWriter " + ex, ex);

            return null;
        }
    }
}
